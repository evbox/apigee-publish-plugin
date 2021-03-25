package io.everon;

import org.apache.commons.io.IOUtils;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ApigeePublishPluginTest {

    private static final String SPEC_NAME = "spec";
    private static final String SPEC_CONTENT = "openapi: 3.0.2\ninfo:\n  title: Test API V1\n";
    private static final String APIGEE_TOKEN = "token";

    @Test
    @DisplayName("Plugin is applied successfully and contains an 'apigeePublish' task.")
    void apigeePublishPluginAddsApigeePublishTaskToProject() {

        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply("io.everon.apigee-publish");

        final Task apigeePublishTask = project.getTasks().getByName(ApigeePublishPlugin.TASK_NAME);

        assertThat(apigeePublishTask).isNotNull();

    }

    @Test
    @DisplayName("Read file contents")
    void testReadFile() {
        String[] filePaths = new String[]{getClass().getResource("test.yaml").getPath()};
        LinkedHashMap<String, String> contents = (LinkedHashMap<String, String>) ApigeePublishPlugin.readFileContents(filePaths);
        assertThat(contents.get("Test API V1")).isEqualTo(SPEC_CONTENT);
    }

    @Test
    @DisplayName("Find spec title")
    void testFindSpecTitle() throws IOException {
        String yaml = IOUtils.toString(getClass().getResourceAsStream("test.yaml"), StandardCharsets.UTF_8);

        String title = ApigeePublishPlugin.findSpecTitle(yaml);
        assertThat(title).isEqualTo("Test API V1");
    }

    @Test
    @DisplayName("Find spec title not found")
    void testFindSpecTitleNotFound() {
        assertThrows(RuntimeException.class, () -> ApigeePublishPlugin.findSpecTitle("test"));
    }

    @Test
    @DisplayName("Overwrite system properties")
    void testOverwriteSystemProperties() {
        ApigeePublishExtension extension = new ApigeePublishExtension();
        extension.setUsername("username");
        extension.setPassword("password");
        System.setProperty("APIGEE_USERNAME", "newUsername");
        System.setProperty("APIGEE_PASSWORD", "newPassword");
        System.setProperty("APIGEE_UNKNOWN", "unknown");
        System.setProperty("DUMMY", "dummyValue");

        ApigeePublishPlugin.overwriteExtensionWithSystemProperties(extension);

        assertThat(extension.getUsername()).isEqualTo("newUsername");
        assertThat(extension.getPassword()).isEqualTo("newPassword");
    }

    @Test
    @DisplayName("Publish docs test no docs")
    void testPublishDocsNoDocs() {
        List<Map<String, Object>> existingApiDocs = List.of(Map.of("title", "test"));
        ApigeeHttpClient client = Mockito.mock(ApigeeHttpClient.class);

        ApigeePublishPlugin.publishDoc(client, existingApiDocs, APIGEE_TOKEN, "test2");

        verifyZeroInteractions(client);
    }

    @Test
    @DisplayName("Publish docs test")
    void testPublishDocsNo() {
        List<Map<String, Object>> existingApiDocs = List.of(Map.of("title", "test", "id", "123"));
        ApigeeHttpClient client = Mockito.mock(ApigeeHttpClient.class);

        ApigeePublishPlugin.publishDoc(client, existingApiDocs, APIGEE_TOKEN, "test");

        verify(client).publishApiDocSnapshot("123", APIGEE_TOKEN);
    }

    @Test
    @DisplayName("Publish new specs test")
    void testNewPublishSpecs() {
        List<Map<String, Object>> existingApiSpec = List.of(Map.of("title", "test", "id", "123"));
        Map<String, Object> newApiSpec = Map.of(SPEC_NAME, SPEC_CONTENT);
        String docId = "321";
        ApigeeHttpClient client = Mockito.mock(ApigeeHttpClient.class);
        when(client.publishNewApiSpecDoc(SPEC_NAME, "folderId", APIGEE_TOKEN)).thenReturn(docId);

        ApigeePublishPlugin.publishSpec(client, existingApiSpec, "spec", APIGEE_TOKEN, newApiSpec, "folderId");

        verify(client).publishApiSpecContent(SPEC_NAME, SPEC_CONTENT, docId, APIGEE_TOKEN);
    }

    @Test
    @DisplayName("Publish existing specs test")
    void testExistingPublishSpecs() {
        List<Map<String, Object>> existingApiSpec = List.of(Map.of("title", "test", "id", "123", "name", "spec"));
        Map<String, Object> newApiSpec = Map.of(SPEC_NAME, SPEC_CONTENT);
        ApigeeHttpClient client = Mockito.mock(ApigeeHttpClient.class);

        ApigeePublishPlugin.publishSpec(client, existingApiSpec, "spec", APIGEE_TOKEN, newApiSpec, "folderId");

        verify(client).publishApiSpecContent(SPEC_NAME, SPEC_CONTENT, "123", APIGEE_TOKEN);
    }

}
