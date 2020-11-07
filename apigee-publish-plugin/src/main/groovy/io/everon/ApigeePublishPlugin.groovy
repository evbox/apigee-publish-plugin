package io.everon

import groovy.json.JsonSlurper
import org.gradle.api.Plugin
import org.gradle.api.Project

import java.nio.charset.StandardCharsets
import java.util.regex.Matcher
import java.util.regex.Pattern

class ApigeePublishPlugin implements Plugin<Project> {

    public static final String TASK_NAME = 'apigeePublish'

    void apply(Project project) {

        def extension = project.extensions.create('apigee', ApigeePublishExtension)

        project.tasks.register(TASK_NAME) {

            doLast {

                def specFileContents = readFileContents(extension.localSpecFilePaths)
                def apigeeAccessToken = obtainApigeeAccessToken(extension.loginUrl, extension.username, extension.password)
                def specsFolder = getExistingSpecsFolder(extension.specFolderUrl, apigeeAccessToken)
                def folderId = specsFolder.get("id")
                def existingOpenApiSpecs = specsFolder.get("contents")
                def existingApiDocs = getExistingApiDocs(extension.apiDocsUrl, apigeeAccessToken)

                for (String specName : specFileContents.keySet()) {

                    def specPublished

                    if (existingOpenApiSpecs.stream().noneMatch { existingOpenApiSpec -> existingOpenApiSpec.get("name") == specName }) {

                        def docId = publishNewApiSpecDoc(specName, folderId as String, extension.specDocUrl, apigeeAccessToken)
                        specPublished = publishApiSpecContent(specName, specFileContents.get(specName) as String, extension.specContentUrl.replace("<id>", docId), apigeeAccessToken)

                    } else {

                        def existingOpenApiSpec = existingOpenApiSpecs.stream()
                                .filter { existingOpenApiSpec -> existingOpenApiSpec.get("name") == specName }
                                .findFirst()
                                .orElseThrow()
                        specPublished = publishApiSpecContent(specName, specFileContents.get(specName) as String, extension.specContentUrl.replace("<id>", existingOpenApiSpec.get("id")),
                                apigeeAccessToken)

                    }

                    if (!specPublished || extension.apiDocsUrl?.trim()) {
                        continue
                    }

                    if (existingApiDocs.stream().noneMatch { existingApiDoc -> existingApiDoc.get("title") == specName }) {
                        println "No api docs found for ${specName}, API doc snapshot republishing will not be attempted."
                        println "    - To have your spec published automatically create API product for ${specName}, and publish it in API catalog of Everon API documentation portal" +
                                " (as described here https://docs.io.everon.dev/#/architecture/docs/apigee/apigee_developer_portal)."
                    } else {

                        def existingApiDoc = existingApiDocs.stream()
                                .filter { existingApiDoc -> existingApiDoc.get("title") == specName }
                                .findFirst()
                                .orElseThrow()
                        publishApiDocSnapshot(extension.apiDocSnapshotUrl.replace("<api_doc_id>", Integer.toString(existingApiDoc.get("id") as int)), apigeeAccessToken)

                    }


                }

            }

        }

    }

    static def readFileContents(String[] localFilePaths) {

        def fileContents = [:]
        for (String localFilePath : localFilePaths) {
            File file = new File(localFilePath)
            String fileContent = file.text
            fileContents[findSpecTitle(fileContent)] = fileContent
        }
        println "Successfully loaded API definitions ${fileContents.keySet()}."
        return fileContents

    }

    static String findSpecTitle(String openApiYaml) {

        Pattern pattern = Pattern.compile("title:(.*?)\n", Pattern.DOTALL)
        Matcher matcher = pattern.matcher(openApiYaml)
        while (matcher.find()) {
            return matcher.group(1).replace("\"", "").trim()
        }
        throw new RuntimeException("Could not find title field in spec content: ${openApiYaml}")
    }

    static String obtainApigeeAccessToken(String loginUrl, String username, String password) {

        String urlParameters = "username=${username}&password=${password}&grant_type=password"
        byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8)
        HttpURLConnection connection = buildHttpConnection(loginUrl, urlParameters, "Basic ZWRnZWNsaTplZGdlY2xpc2VjcmV0", "application/x-www-form-urlencoded", "POST")

        try {
            DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream())
            outputStream.write(postData)
        } catch (Exception ex) {
            throw new RuntimeException("Failed to make an authentication call to Apigee. " + ex)
        }

        def responseCode = connection.getResponseCode()
        if (responseCode == 200) {
            def responseBody = connection.getInputStream().getText()
            def accessToken = new JsonSlurper().parseText(responseBody).get("access_token")
            println "Authenticated at Apigee successfully. "
            return accessToken
        } else {
            throw new RuntimeException("Failed to authenticate with Apigee. Response code ${responseCode} received. ${connection.getErrorStream().getText()}")
        }

    }

    static Map<String, Object> getExistingSpecsFolder(String specsUrl, String accessToken) {

        def connection = new URL(specsUrl).openConnection()
        connection.setRequestProperty("Authorization", "Bearer ${accessToken}")
        def responseCode

        try {
            responseCode = connection.getResponseCode()
        } catch (Exception ex) {
            throw new RuntimeException("Failed to make a call to retrieve existing Open API specs to Apigee. " + ex)
        }

        if (responseCode == 200) {
            def responseBody = connection.getInputStream().getText()
            def specsFolder = new JsonSlurper().parseText(responseBody)
            println "Found following API specifications:"
            for (def existingOpenApiSpec : specsFolder.get("contents")) {
                println "    - ${existingOpenApiSpec.get("name")}"
            }
            return specsFolder as Map<String, Object>
        } else {
            throw new RuntimeException("Failed to retrieve existing Open API specs from Apigee. Response code ${responseCode} received. ${connection.getErrorStream().getText()}")
        }

    }

    static String publishNewApiSpecDoc(String specName, String folderId, String apigeeSpecDocUrl, String apigeeAccessToken) {

        def body = "{ \"folder\": \"${folderId}\", \"kind\": \"Doc\", \"name\": \"${specName}\"}"
        byte[] postData = body.getBytes(StandardCharsets.UTF_8)
        HttpURLConnection connection = buildHttpConnection(apigeeSpecDocUrl, body, "Bearer ${apigeeAccessToken}", "application/json", "POST")

        try {
            DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream())
            outputStream.write(postData)
        } catch (Exception ex) {
            throw new RuntimeException("Failed to make a call to create a new doc to Apigee. " + ex)
        }

        def responseCode = connection.getResponseCode()
        if (responseCode == 200) {
            def responseBody = connection.getInputStream().getText()
            def docId = new JsonSlurper().parseText(responseBody).get("id")
            println "New doc at Apigee created successfully for ${specName}. "
            return docId
        } else {
            throw new RuntimeException("Failed to authenticate with Apigee. Response code ${responseCode} received. ${connection.getErrorStream().getText()}")
        }

    }

    static boolean publishApiSpecContent(String specName, String specContent, String apigeeSpecUrl, String apigeeAccessToken) {

        def existingSpecContent = getExistingSpecContent(apigeeSpecUrl, apigeeAccessToken)
        if (existingSpecContent == specContent) {
            println "Spec content unchanged for ${specName}, publishing will not be attempted."
            return false
        }

        byte[] postData = specContent.getBytes(StandardCharsets.UTF_8)
        HttpURLConnection connection = buildHttpConnection(apigeeSpecUrl, specContent, "Bearer ${apigeeAccessToken}", "text/plain", "PUT")

        try {
            DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream())
            outputStream.write(postData)
        } catch (Exception ex) {
            throw new RuntimeException("Failed to make a call to upload spec content to Apigee. " + ex)
        }

        def responseCode = connection.getResponseCode()
        if (responseCode == 200) {
            println "Spec content uploaded successfully to Apigee for ${specName}. "
            return true
        } else {
            throw new RuntimeException("Failed to upload spec content with Apigee. Response code ${responseCode} received. ${connection.getErrorStream().getText()}")
        }

    }

    static String getExistingSpecContent(String specContentUrl, String accessToken) {

        def connection = new URL(specContentUrl).openConnection()
        connection.setRequestProperty("Authorization", "Bearer ${accessToken}")
        def responseCode

        try {
            responseCode = connection.getResponseCode()
        } catch (Exception ex) {
            throw new RuntimeException("Failed to make a call to fetch content existing Open API specs to Apigee. " + ex)
        }

        if (responseCode == 200) {
            return connection.getInputStream().getText()
        } else {
            throw new RuntimeException("Failed to retrieve contents of existing Open API specs from Apigee " +
                    "at ${specContentUrl}. Response code ${responseCode} received. ${connection.getErrorStream().getText()}")
        }

    }

    static List<Map<String, Object>> getExistingApiDocs(String apiDocsUrl, String apigeeAccessToken) {

        if (apiDocsUrl?.trim()) {
            return Collections.emptyList();
        }

        def connection = new URL(apiDocsUrl).openConnection()
        connection.setRequestProperty("Authorization", "Bearer ${apigeeAccessToken}")
        def responseCode

        try {
            responseCode = connection.getResponseCode()
        } catch (Exception ex) {
            throw new RuntimeException("Failed to make a call to retrieve existing API docs to Apigee. " + ex)
        }

        if (responseCode == 200) {
            def responseBody = connection.getInputStream().getText()
            def apiDocsResponse = new JsonSlurper().parseText(responseBody)
            println "Found following API docs:"
            for (def existingOpenApiSpec : apiDocsResponse.get("data")) {
                println "    - ${existingOpenApiSpec.get("title")}"
            }
            return apiDocsResponse.get("data") as List<Map<String, Object>>
        } else {
            throw new RuntimeException("Failed to retrieve existing API docs from Apigee. Response code ${responseCode} received. ${connection.getErrorStream().getText()}")
        }

    }

    static def publishApiDocSnapshot(String apiDocSnapshotUrl, String apigeeAccessToken) {

        byte[] postData = "".getBytes(StandardCharsets.UTF_8)
        HttpURLConnection connection = buildHttpConnection(apiDocSnapshotUrl, "", "Bearer ${apigeeAccessToken}", "application/json", "PUT")

        try {
            DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream())
            outputStream.write(postData)
        } catch (Exception ex) {
            throw new RuntimeException("Failed to make a call to update API doc snapshot to Apigee. " + ex)
        }

        def responseCode = connection.getResponseCode()
        if (responseCode == 200) {
            println "API doc snapshot updated successfully at Apigee on ${apiDocSnapshotUrl}. "
        } else {
            throw new RuntimeException("Failed to update API doc snapshot with Apigee. Response code ${responseCode} received. ${connection.getErrorStream().getText()}")
        }

    }

    static def buildHttpConnection(String urlPath, String body, String authorizationHeader, String contentType, String method) {

        int postDataLength = body.getBytes(StandardCharsets.UTF_8).length
        URL url = new URL(urlPath)
        HttpURLConnection connection = (HttpURLConnection) url.openConnection()
        connection.setDoOutput(true)
        connection.setInstanceFollowRedirects(false)
        connection.setRequestMethod(method)
        connection.setRequestProperty("Content-Type", contentType)
        connection.setRequestProperty("charset", "utf-8")
        connection.setRequestProperty("Content-Length", Integer.toString(postDataLength))
        connection.setRequestProperty("Authorization", authorizationHeader)
        connection.setUseCaches(false)
        return connection

    }

}