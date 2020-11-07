package io.everon;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class ApigeePublishPluginTest {

    @Test
    public void apigeePublishPluginAddsApigeePublishTaskToProject() {

        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply("io.everon.apigee-publish");

        final Task apigeePublishTask = project.getTasks().getByName(ApigeePublishPlugin.TASK_NAME);

        assertNotNull(apigeePublishTask);

    }

}
