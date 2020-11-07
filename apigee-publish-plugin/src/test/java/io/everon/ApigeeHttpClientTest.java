package io.everon;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.junit.jupiter.MockServerSettings;
import org.mockserver.model.HttpRequest;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockserver.matchers.Times.exactly;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@ExtendWith(MockServerExtension.class)
@MockServerSettings(ports = {8787})
class ApigeeHttpClientTest {

    private static final String ORGANIZATION_NAME = "organization";
    private static final String PORTAL_NAME = "portal";

    private MockServerClient mockServerClient;
    private final ApigeeHttpClient client = new ApigeeHttpClient(ORGANIZATION_NAME, PORTAL_NAME);

    @BeforeEach
    public void setup(MockServerClient mockServerClient) {
        this.mockServerClient = mockServerClient;
        mockServerClient.reset();
    }

    @Test
    void obtainApigeeAccessTokenSuccessfully() {

        String accessToken = "You shall pass!";
        String basicAuthToken = UUID.randomUUID().toString();
        client.setLoginUrl("http://127.0.0.1:8787/oauth/token");
        HttpRequest requestDefinition = request()
                .withMethod("POST")
                .withPath("/oauth/token")
                .withHeader("Authorization", "Basic " + basicAuthToken);
        mockServerClient
                .when(requestDefinition, exactly(1))
                .respond(response()
                        .withStatusCode(HttpStatus.SC_OK)
                        .withBody("{ \"access_token\": \"" + accessToken + "\" }"));

        String fetchedAcessToken =
                client.obtainApigeeAccessToken("chucknorris", "chucknorris", basicAuthToken);

        assertThat(fetchedAcessToken).isEqualTo(accessToken);
        mockServerClient.verify(requestDefinition);

    }

    @Test
    void obtainApigeeAccessTokenNonSuccessStatusReturnedExceptionThrown() {

        String basicAuthToken = UUID.randomUUID().toString();
        client.setLoginUrl("http://127.0.0.1:8787/oauth/token");
        HttpRequest requestDefinition = request()
                .withMethod("POST")
                .withPath("/oauth/token")
                .withHeader("Authorization", "Basic " + basicAuthToken);
        mockServerClient
                .when(requestDefinition, exactly(1))
                .respond(response()
                        .withStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR));

        assertThrows(RuntimeException.class,
                () -> client.obtainApigeeAccessToken("chucknorris", "chucknorris", basicAuthToken));
        mockServerClient.verify(requestDefinition);

    }

    @Test
    void getExistingSpecsFolderSuccessfully() {

        String accessToken = "You shall pass!";
        client.setSpecFolderUrl("http://127.0.0.1:8787/dapi/api/organizations/organization/specs/folder/home");
        HttpRequest requestDefinition = request()
                .withMethod("GET")
                .withPath("/dapi/api/organizations/organization/specs/folder/home")
                .withHeader("Authorization", "Bearer " + accessToken);
        mockServerClient
                .when(requestDefinition, exactly(1))
                .respond(response()
                        .withStatusCode(HttpStatus.SC_OK)
                        .withBody("{ \"contents\": [ { \"name\": \"spec1\" }, { \"name\": \"spec2\" } ] }"));

        Map<String, Object> existingSpecsFolder = client.getExistingSpecsFolder(accessToken);

        @SuppressWarnings("unchecked")
        List<Map<String, String>> contents = (List<Map<String, String>>) existingSpecsFolder.get("contents");
        assertThat(contents).hasSize(2);
        assertThat(contents.stream().map(spec -> spec.get("name")).collect(Collectors.toSet()))
                .contains("spec1", "spec2");
        mockServerClient.verify(requestDefinition);

    }

    @Test
    void getExistingSpecsFolderNonSuccessStatusReturnedExceptionThrown() {

        String accessToken = "You shall pass!";
        client.setSpecFolderUrl("http://127.0.0.1:8787/dapi/api/organizations/organization/specs/folder/home");
        HttpRequest requestDefinition = request()
                .withMethod("GET")
                .withPath("/dapi/api/organizations/organization/specs/folder/home")
                .withHeader("Authorization", "Bearer " + accessToken);
        mockServerClient
                .when(requestDefinition, exactly(1))
                .respond(response()
                        .withStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR));

        assertThrows(RuntimeException.class, () -> client.getExistingSpecsFolder(accessToken));

        mockServerClient.verify(requestDefinition);

    }

    @Test
    void publishNewApiSpecDocSuccessfully() {

        String specName = "spec1";
        String folderId = "folder1";
        String accessToken = "You shall pass!";
        String body = "{ \"folder\": \"" + folderId + "\", \"kind\": \"Doc\", \"name\": \"" + specName + "\"}";
        String docId = "docId";
        client.setSpecDocUrl("http://127.0.0.1:8787/dapi/api/organizations/organization/specs/doc");
        HttpRequest requestDefinition = request()
                .withMethod("POST")
                .withPath("/dapi/api/organizations/organization/specs/doc")
                .withBody(body)
                .withHeader("Authorization", "Bearer " + accessToken);
        mockServerClient
                .when(requestDefinition, exactly(1))
                .respond(response()
                        .withStatusCode(HttpStatus.SC_OK)
                        .withBody("{ \"id\": \"" + docId + "\" }"));

        String fetchedDocId = client.publishNewApiSpecDoc(specName, folderId, accessToken);

        assertThat(fetchedDocId).isEqualTo(docId);
        mockServerClient.verify(requestDefinition);

    }

    @Test
    void publishNewApiSpecDocNonSuccessStatusReturnedExceptionThrown() {

        String specName = "spec1";
        String folderId = "folder1";
        String accessToken = "You shall pass!";
        String body = "{ \"folder\": \"" + folderId + "\", \"kind\": \"Doc\", \"name\": \"" + specName + "\"}";
        client.setSpecDocUrl("http://127.0.0.1:8787/dapi/api/organizations/organization/specs/doc");
        HttpRequest requestDefinition = request()
                .withMethod("POST")
                .withPath("/dapi/api/organizations/organization/specs/doc")
                .withBody(body)
                .withHeader("Authorization", "Bearer " + accessToken);
        mockServerClient
                .when(requestDefinition, exactly(1))
                .respond(response()
                        .withStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR));

        assertThrows(RuntimeException.class, () -> client.publishNewApiSpecDoc(specName, folderId, accessToken));

        mockServerClient.verify(requestDefinition);

    }

    @Test
    void publishApiSpecContentSpecUnchangedPublishNotAttempted() {

        String specName = "spec1";
        String specContent = "Some OpenAPI content here...";
        String accessToken = "You shall pass!";
        String docId = "docId";
        client.setSpecContentUrlTemplate(
                "http://127.0.0.1:8787/dapi/api/organizations/organization/specs/doc/<id>/content");
        HttpRequest getRequestDefinition = request()
                .withMethod("GET")
                .withPath("/dapi/api/organizations/organization/specs/doc/" + docId + "/content")
                .withHeader("Authorization", "Bearer " + accessToken);
        mockServerClient
                .when(getRequestDefinition, exactly(1))
                .respond(response()
                        .withStatusCode(HttpStatus.SC_OK)
                        .withBody(specContent));

        boolean published = client.publishApiSpecContent(specName, specContent, docId, accessToken);

        assertThat(published).isFalse();
        mockServerClient.verify(getRequestDefinition);

    }

    @Test
    void publishApiSpecContentFetchingSpecContentFailedExceptionThrown() {

        String specName = "spec1";
        String specContent = "Some OpenAPI content here...";
        String accessToken = "You shall pass!";
        String docId = "docId";
        client.setSpecContentUrlTemplate(
                "http://127.0.0.1:8787/dapi/api/organizations/organization/specs/doc/<id>/content");
        HttpRequest getRequestDefinition = request()
                .withMethod("GET")
                .withPath("/dapi/api/organizations/organization/specs/doc/" + docId + "/content")
                .withHeader("Authorization", "Bearer " + accessToken);
        mockServerClient
                .when(getRequestDefinition, exactly(1))
                .respond(response()
                        .withStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR));

        assertThrows(RuntimeException.class,
                () -> client.publishApiSpecContent(specName, specContent, docId, accessToken));

        mockServerClient.verify(getRequestDefinition);

    }

    @Test
    void publishApiSpecContentSpecModifiedPublishSuccessful() {

        String specName = "spec1";
        String specContent = "Some OpenAPI content here...";
        String accessToken = "You shall pass!";
        String docId = "docId";
        client.setSpecContentUrlTemplate(
                "http://127.0.0.1:8787/dapi/api/organizations/organization/specs/doc/<id>/content");
        HttpRequest putRequestDefinition = request()
                .withMethod("PUT")
                .withPath("/dapi/api/organizations/organization/specs/doc/" + docId + "/content")
                .withBody(specContent)
                .withHeader("Authorization", "Bearer " + accessToken);
        mockServerClient
                .when(putRequestDefinition, exactly(1))
                .respond(response()
                        .withStatusCode(HttpStatus.SC_OK));
        HttpRequest getRequestDefinition = request()
                .withMethod("GET")
                .withPath("/dapi/api/organizations/organization/specs/doc/" + docId + "/content")
                .withHeader("Authorization", "Bearer " + accessToken);
        mockServerClient
                .when(getRequestDefinition, exactly(1))
                .respond(response()
                        .withStatusCode(HttpStatus.SC_OK)
                        .withBody(specContent + "change"));

        boolean published = client.publishApiSpecContent(specName, specContent, docId, accessToken);

        assertThat(published).isTrue();
        mockServerClient.verify(putRequestDefinition);
        mockServerClient.verify(getRequestDefinition);

    }

    @Test
    void publishApiSpecContentSpecModifiedNonSuccessStatusReturnedExceptionThrown() {

        String specName = "spec1";
        String specContent = "Some OpenAPI content here...";
        String accessToken = "You shall pass!";
        String docId = "docId";
        client.setSpecContentUrlTemplate(
                "http://127.0.0.1:8787/dapi/api/organizations/organization/specs/doc/<id>/content");
        HttpRequest putRequestDefinition = request()
                .withMethod("PUT")
                .withPath("/dapi/api/organizations/organization/specs/doc/" + docId + "/content")
                .withBody(specContent)
                .withHeader("Authorization", "Bearer " + accessToken);
        mockServerClient
                .when(putRequestDefinition, exactly(1))
                .respond(response()
                        .withStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR));
        HttpRequest getRequestDefinition = request()
                .withMethod("GET")
                .withPath("/dapi/api/organizations/organization/specs/doc/" + docId + "/content")
                .withHeader("Authorization", "Bearer " + accessToken);
        mockServerClient
                .when(getRequestDefinition, exactly(1))
                .respond(response()
                        .withStatusCode(HttpStatus.SC_OK)
                        .withBody(specContent + "change"));

        assertThrows(RuntimeException.class,
                () -> client.publishApiSpecContent(specName, specContent, docId, accessToken));

        mockServerClient.verify(putRequestDefinition);
        mockServerClient.verify(getRequestDefinition);

    }

    @Test
    void getExistingApiDocsSuccessfully() {

        String accessToken = "You shall pass!";
        client.setApiDocsUrl("http://localhost:8787/portals/api/sites/portal/apidocs");
        HttpRequest requestDefinition = request()
                .withMethod("GET")
                .withPath("/portals/api/sites/portal/apidocs")
                .withHeader("Authorization", "Bearer " + accessToken);
        mockServerClient
                .when(requestDefinition, exactly(1))
                .respond(response()
                        .withStatusCode(HttpStatus.SC_OK)
                        .withBody("{ \"data\": [ { \"title\": \"spec1\" }, { \"title\": \"spec2\" } ] }"));

        List<Map<String, Object>> apiDocs = client.getExistingApiDocs(accessToken);

        assertThat(apiDocs).hasSize(2);
        assertThat(apiDocs.stream().map(spec -> spec.get("title")).collect(Collectors.toSet()))
                .contains("spec1", "spec2");
        mockServerClient.verify(requestDefinition);

    }

    @Test
    void getExistingApiDocsUrlMissingEmptyListReturned() {

        client.setApiDocsUrl("");

        List<Map<String, Object>> apiDocs = client.getExistingApiDocs("");

        assertThat(apiDocs).isEmpty();
        mockServerClient.verifyZeroInteractions();

    }

    @Test
    void getExistingApiDocsNonSuccessStatusReturnedExceptionThrown() {

        String accessToken = "You shall pass!";
        client.setApiDocsUrl("http://localhost:8787/portals/api/sites/portal/apidocs");
        HttpRequest requestDefinition = request()
                .withMethod("GET")
                .withPath("/portals/api/sites/portal/apidocs")
                .withHeader("Authorization", "Bearer " + accessToken);
        mockServerClient
                .when(requestDefinition, exactly(1))
                .respond(response()
                        .withStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR));

        assertThrows(RuntimeException.class, () -> client.getExistingApiDocs(accessToken));

        mockServerClient.verify(requestDefinition);

    }

    @Test
    void publishApiDocSnapshotSuccessfully() {

        String accessToken = "You shall pass!";
        String apiDocId = "apiDocId";
        client.setApiDocSnapshotUrlTemplate(
                "http://127.0.0.1:8787/portals/api/sites/portal/apidocs/<api_doc_id>/snapshot");
        HttpRequest requestDefinition = request()
                .withMethod("PUT")
                .withPath("/portals/api/sites/portal/apidocs/" + apiDocId + "/snapshot")
                .withBody("")
                .withHeader("Authorization", "Bearer " + accessToken);
        mockServerClient
                .when(requestDefinition, exactly(1))
                .respond(response()
                        .withStatusCode(HttpStatus.SC_OK));

        client.publishApiDocSnapshot(apiDocId, accessToken);

        mockServerClient.verify(requestDefinition);

    }

    @Test
    void publishApiDocSnapshotNonSuccessStatusReturnedExceptionThrown() {

        String accessToken = "You shall pass!";
        String apiDocId = "apiDocId";
        client.setApiDocSnapshotUrlTemplate(
                "http://127.0.0.1:8787/portals/api/sites/portal/apidocs/<api_doc_id>/snapshot");
        HttpRequest requestDefinition = request()
                .withMethod("PUT")
                .withPath("/portals/api/sites/portal/apidocs/" + apiDocId + "/snapshot")
                .withBody("")
                .withHeader("Authorization", "Bearer " + accessToken);
        mockServerClient
                .when(requestDefinition, exactly(1))
                .respond(response()
                        .withStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR));

        assertThrows(RuntimeException.class, () -> client.publishApiDocSnapshot(apiDocId, accessToken));

        mockServerClient.verify(requestDefinition);

    }

}
