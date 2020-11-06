# Apigee Publish Plugin
### Plugin Setup

Add following to your `build.gradle` file.
```
pluginManager.apply ApigeePublishPlugin
apigee {
    localSpecFilePaths = [
            "<name_of_your_api_spec_file>.yaml",
            "<name_of_your_api_spec_file>.yaml"
    ]
    username = "<apigee_username"
    password = "<apigee_password"
    portalName = "<apigee_portal_name>"
    organizationName = "<apigee_organization_name>"
}
```
### FAQ
TBD

## Caveats

Plugin is using API that Apigee UI is using.