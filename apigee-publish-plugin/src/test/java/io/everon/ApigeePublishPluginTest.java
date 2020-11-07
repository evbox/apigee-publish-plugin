package io.everon;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class ApigeePublishPluginTest {

    @Test
    void apigeePublishPluginAddsApigeePublishTaskToProject() {

        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply("io.everon.apigee-publish");

        final Task apigeePublishTask = project.getTasks().getByName(ApigeePublishPlugin.TASK_NAME);

        assertThat(apigeePublishTask).isNotNull();

    }

}
