package io.everon

import groovy.json.JsonSlurper

import java.nio.charset.StandardCharsets

class ApigeeHttpClient {

    private static final String LOGIN_URL = "https://login.apigee.com/oauth/token";
    private static final String BASE_ORGANIZATIONS_URL = "https://apigee.com/dapi/api/organizations";
    private static final String BASE_PORTALS_URL = "https://apigee.com/portals/api/sites";

    String loginUrl
    String specFolderUrl
    String specContentUrlTemplate
    String specDocUrl
    String apiDocsUrl
    String apiDocSnapshotUrlTemplate

    ApigeeHttpClient(String organizationName, String portalName) {

        loginUrl = LOGIN_URL
        specFolderUrl = BASE_ORGANIZATIONS_URL + "/${organizationName}/specs/folder/home"
        specContentUrlTemplate = BASE_ORGANIZATIONS_URL + "/${organizationName}/specs/doc/<id>/content"
        specDocUrl = BASE_ORGANIZATIONS_URL + "/${organizationName}/specs/doc"
        apiDocsUrl = BASE_PORTALS_URL + "/${portalName}/apidocs"
        apiDocSnapshotUrlTemplate = BASE_PORTALS_URL + "/${portalName}/apidocs/<api_doc_id>/snapshot"

    }

    String obtainApigeeAccessToken(String username, String password, String basicAuthorizationToken) {

        String urlParameters = "username=${username}&password=${password}&grant_type=password"
        byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8)
        HttpURLConnection connection = buildHttpConnection(
                loginUrl,
                urlParameters,
                "Basic " + basicAuthorizationToken,
                "application/x-www-form-urlencoded",
                "POST")

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
            throw new RuntimeException("Failed to authenticate with Apigee. Response code ${responseCode} received." +
                    "${connection.getErrorStream().getText()}")
        }

    }

    Map<String, Object> getExistingSpecsFolder(String accessToken) {

        def connection = new URL(specFolderUrl).openConnection()
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
            throw new RuntimeException("Failed to retrieve existing Open API specs from Apigee." +
                    "Response code ${responseCode} received. ${connection.getErrorStream().getText()}")
        }

    }

    String publishNewApiSpecDoc(String specName, String folderId, String apigeeAccessToken) {

        def body = "{ \"folder\": \"${folderId}\", \"kind\": \"Doc\", \"name\": \"${specName}\"}"
        byte[] postData = body.getBytes(StandardCharsets.UTF_8)
        HttpURLConnection connection = buildHttpConnection(
                specDocUrl,
                body,
                "Bearer ${apigeeAccessToken}",
                "application/json",
                "POST")

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
            throw new RuntimeException("Failed to authenticate with Apigee. Response code ${responseCode} received." +
                    "${connection.getErrorStream().getText()}")
        }

    }

    boolean publishApiSpecContent(String specName, String specContent, String docId, String apigeeAccessToken) {

        def specContentUrl = specContentUrlTemplate.replace("<id>", docId)
        def existingSpecContent = getExistingSpecContent(specContentUrl, apigeeAccessToken)
        if (existingSpecContent == specContent) {
            println "Spec content unchanged for ${specName}, publishing will not be attempted."
            return false
        }

        byte[] postData = specContent.getBytes(StandardCharsets.UTF_8)
        HttpURLConnection connection = buildHttpConnection(
                specContentUrl,
                specContent,
                "Bearer ${apigeeAccessToken}",
                "text/plain",
                "PUT")

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
            throw new RuntimeException("Failed to upload spec content with Apigee." +
                    "Response code ${responseCode} received. ${connection.getErrorStream().getText()}")
        }

    }

    static String getExistingSpecContent(String specContentUrl, String accessToken) {

        def connection = new URL(specContentUrl).openConnection()
        connection.setRequestProperty("Authorization", "Bearer ${accessToken}")
        def responseCode

        try {
            responseCode = connection.getResponseCode()
        } catch (Exception ex) {
            throw new RuntimeException("Failed when getting content of existing Open API specs to Apigee. " + ex)
        }

        if (responseCode == 200) {
            return connection.getInputStream().getText()
        } else {
            throw new RuntimeException("Failed to retrieve contents of existing Open API specs from Apigee " +
                    "at ${specContentUrl}. Response code ${responseCode} received." +
                    "${connection.getErrorStream().getText()}")
        }

    }

    List<Map<String, Object>> getExistingApiDocs(String apigeeAccessToken) {

        if (!apiDocsUrl?.trim()) {
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
            throw new RuntimeException("Failed to retrieve existing API docs from Apigee." +
                    "Response code ${responseCode} received. ${connection.getErrorStream().getText()}")
        }

    }

    def publishApiDocSnapshot(String apiDocId, String apigeeAccessToken) {

        String apiDocSnapshotUrl = apiDocSnapshotUrlTemplate.replace("<api_doc_id>", apiDocId)
        byte[] postData = "".getBytes(StandardCharsets.UTF_8)
        HttpURLConnection connection = buildHttpConnection(
                apiDocSnapshotUrl,
                "",
                "Bearer ${apigeeAccessToken}",
                "application/json",
                "PUT")

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
            throw new RuntimeException("Failed to update API doc snapshot with Apigee." +
                    "Response code ${responseCode} received.${connection.getErrorStream().getText()}")
        }

    }

    static HttpURLConnection buildHttpConnection(
            String urlPath, String body, String authorizationHeader, String contentType, String method) {

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
