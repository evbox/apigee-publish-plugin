package io.everon

import org.gradle.api.Plugin
import org.gradle.api.Project

import java.util.regex.Matcher
import java.util.regex.Pattern

class ApigeePublishPlugin implements Plugin<Project> {

    public static final String TASK_NAME = 'apigeePublish'

    void apply(Project project) {

        def extension = project.extensions.create('apigee', ApigeePublishExtension)

        project.tasks.register(TASK_NAME) {

            ApigeeHttpClient client = new ApigeeHttpClient(extension.organizationName, extension.portalName)

            doLast {
                overwriteExtensionWithSystemProperties(extension)
                def specFileContents = readFileContents(extension.localSpecFilePaths)
                def apigeeAccessToken = client.obtainApigeeAccessToken(
                        extension.username, extension.password)
                def specsFolder = client.getExistingSpecsFolder(apigeeAccessToken)
                def folderId = specsFolder.get("id")
                def existingOpenApiSpecs = specsFolder.get("contents")
                def existingApiDocs = client.getExistingApiDocs(apigeeAccessToken)

                for (String specName : specFileContents.keySet()) {

                    def specPublished

                    if (existingOpenApiSpecs.stream()
                            .noneMatch { existingOpenApiSpec -> existingOpenApiSpec.get("name") == specName }) {

                        def docId = client.publishNewApiSpecDoc(specName, folderId as String, apigeeAccessToken)
                        specPublished = client.publishApiSpecContent(
                                specName, specFileContents.get(specName) as String, docId, apigeeAccessToken)

                    } else {

                        def existingOpenApiSpec = existingOpenApiSpecs.stream()
                                .filter { existingOpenApiSpec -> existingOpenApiSpec.get("name") == specName }
                                .findFirst()
                                .orElseThrow()
                        specPublished = client.publishApiSpecContent(
                                specName,
                                specFileContents.get(specName) as String,
                                existingOpenApiSpec.get("id") as String,
                                apigeeAccessToken)

                    }

                    if (!specPublished || !extension.portalName?.trim()) {
                        continue
                    }

                    if (existingApiDocs.stream()
                            .noneMatch { existingApiDoc -> existingApiDoc.get("title") == specName }) {
                        println "No api docs found for ${specName}," +
                                "API doc snapshot republishing will not be attempted."
                        println "    - To have your spec published automatically create API product for ${specName}," +
                                "and publish it in API catalog of Everon API documentation portal (as described here " +
                                "https://docs.everon.dev/#/architecture/docs/apigee/apigee_developer_portal)."
                    } else {

                        def existingApiDoc = existingApiDocs.stream()
                                .filter { existingApiDoc -> existingApiDoc.get("title") == specName }
                                .findFirst()
                                .orElseThrow()
                        client.publishApiDocSnapshot(Integer.toString(existingApiDoc.get("id") as int), apigeeAccessToken)

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

    static void overwriteExtensionWithSystemProperties(ApigeePublishExtension extension) {
        for (Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
            String key = entry.getKey().toString()
            if (key.contains("APIGEE_")) {
                switch (key) {
                    case "APIGEE_USERNAME":
                        extension.username = entry.getValue().toString()
                        break
                    case "APIGEE_PASSWORD":
                        extension.password = entry.getValue().toString()
                        break
                    default:
                        break
                }
            }
        }
    }

}
