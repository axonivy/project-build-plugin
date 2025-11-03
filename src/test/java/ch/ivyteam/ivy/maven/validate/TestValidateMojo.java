package ch.ivyteam.ivy.maven.validate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.util.List;

import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.ivyteam.ivy.maven.log.LogCollector;

@MojoTest
class TestValidateMojo {

  private ValidateMojo mojo;

  @BeforeEach
  @InjectMojo(goal = ValidateMojo.GOAL)
  void setUp(ValidateMojo validate) {
    this.mojo = validate;
  }

  @Test
  void samePluginVersions() throws Exception {
    var log = new LogCollector();
    mojo.setLog(log);
    var p1 = createMavenProject("project1", "13.1.0");
    var p2 = createMavenProject("project2", "13.1.0");
    mojo.validateConsistentPluginVersion(List.of(p1, p2));
    assertThat(log.getDebug()).hasSize(2);
    assertThat(log.getDebug().get(0).toString())
        .contains("com.axonivy.ivy.ci:project-build-plugin:13.1.0 configured in MavenProject: group:project1:13.1.0-SNAPSHOT");
    assertThat(log.getDebug().get(1).toString())
        .contains("com.axonivy.ivy.ci:project-build-plugin:13.1.0 configured in MavenProject: group:project2:13.1.0-SNAPSHOT");
    assertThat(log.getErrors()).isEmpty();
  }

  @Test
  void differentPluginVersions() throws Exception {
    var log = new LogCollector();
    mojo.setLog(log);
    var p1 = createMavenProject("project1", "13.1.0");
    var p2 = createMavenProject("project2", "13.1.1");
    assertThatThrownBy(() -> mojo.validateConsistentPluginVersion(List.of(p1, p2)))
        .isInstanceOf(MojoExecutionException.class);
    assertThat(log.getErrors()).hasSize(1);
    assertThat(log.getErrors().get(0).toString())
        .isEqualTo("""
          Several versions of project-build-plugins are configured [13.1.0, 13.1.1]:
          13.1.0 -> [project1]
          13.1.1 -> [project2]""");
  }

  private MavenProject createMavenProject(String projectId, String version) {
    var project = new MavenProject();
    project.setGroupId("group");
    project.setArtifactId(projectId);
    project.setVersion("13.1.0-SNAPSHOT");
    project.setFile(new File("src/test/resources/" + projectId));
    var plugin = new Plugin();
    plugin.setGroupId(ValidateMojo.PLUGIN_GROUPID);
    plugin.setArtifactId(ValidateMojo.PLUGIN_ARTIFACTID);
    plugin.setVersion(version);
    project.getBuild().getPlugins().add(plugin);
    return project;
  }
}
