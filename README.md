# Apigee Publish Plugin
### Plugin Setup

Add following to your `build.gradle` file.
```
plugins {
    id 'io.everon.apigee-publish' version '0.1.3'
}

apigee {
    localSpecFilePaths = [
            "<name_of_your_api_spec_file>.yaml",
            "<name_of_your_api_spec_file>.yaml"
    ]
    username = "<apigee_username>"
    password = "<apigee_password>
    organizationName = "<apigee_organization_name>"
    portalName = "<apigee_portal_name>"
}
```
Note that the portal name is optional. If not provided APIs will not be published to any portal.

### FAQ
TBD

## Caveats

Plugin is using API that Apigee UI is using.