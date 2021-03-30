# Apigee Publish Plugin
### Plugin Setup

Add following to your `build.gradle` file.
```
plugins {
    id 'io.everon.apigee-publish' version '0.2.1'
}

apigee {
    localSpecFilePaths = [
            "<name_of_your_api_spec_file>.yaml",
            "<name_of_your_api_spec_file>.yaml"
    ]
    username = "<apigee_username>"
    password = "<apigee_password>"
    organizationName = "<apigee_organization_name>"
    portalName = "<apigee_portal_name>"
}
```

 * **localSpecFilePaths** - List of file paths to your OpenAPI spec yaml files.
 * **username** - Username to your Apigee account.
 * **password** - Your Apigee password.
 * **organizationName** - Name of the organization where you want to upload specs.
 * **portalName** - Name of the portal where you want to publish specs. Note that the portal name is optional.
 If not provided APIs will not be published (only specs will be uploaded).
 
It is also possible to send username/password as dynamic properties in which case they will override the gradle plugin settings.
The properties should be named APIGEE_USERNAME and APIGEE_PASSWORD. So for instance:

```
./gradlew apigeePublish -DAPIGEE_USERNAME=myusername -DAPIGEE_PASSWORD=mypassword
```
 
Currently for the plugin to work you need to have specification, product and catalog setup and connected the first time.

### FAQ
TBD

## Caveats

Plugin is relying on API that Apigee UI is consuming.
