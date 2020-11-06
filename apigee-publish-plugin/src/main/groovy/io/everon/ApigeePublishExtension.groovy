package io.everon;

class ApigeePublishExtension {

    String loginUrl = "https://login.apigee.com/oauth/token"
    String username = (String) System.env['APIGEE_USER']
    String password = (String) System.env['APIGEE_PASSWORD']

    String portalName = (String) System.env['PORTAL_NAME']
    String organization = (String) System.env['ORGANIZATION_NAME']
    String specFolderUrl = "https://apigee.com/dapi/api/organizations/${organization}/specs/folder/home"
    String specContentUrl = "https://apigee.com/dapi/api/organizations/${organization}/specs/doc/<id>/content"
    String specDocUrl = "https://apigee.com/dapi/api/organizations/${organization}/specs/doc"
    String apiDocsUrl = "https://apigee.com/portals/api/sites/${portalName}/apidocs"
    String apiDocSnapshotUrl = "https://apigee.com/portals/api/sites/${portalName}/apidocs/<api_doc_id>/snapshot"

    String[] localSpecFilePaths

    ApigeePublishExtension() {}

}
