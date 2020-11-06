package io.everon;

class ApigeePublishExtension {

    String loginUrl = "https://login.apigee.com/oauth/token"
    String username
    String password

    String portalName
    String organizationName
    String specFolderUrl = "https://apigee.com/dapi/api/organizations/${organizationName}/specs/folder/home"
    String specContentUrl = "https://apigee.com/dapi/api/organizations/${organizationName}/specs/doc/<id>/content"
    String specDocUrl = "https://apigee.com/dapi/api/organizations/${organizationName}/specs/doc"
    String apiDocsUrl = "https://apigee.com/portals/api/sites/${portalName}/apidocs"
    String apiDocSnapshotUrl = "https://apigee.com/portals/api/sites/${portalName}/apidocs/<api_doc_id>/snapshot"

    String[] localSpecFilePaths

    ApigeePublishExtension() {}

}
