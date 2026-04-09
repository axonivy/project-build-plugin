package ch.ivyteam.ivy.maven.compile;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import ch.ivyteam.ivy.maven.extension.ProjectExtension;
import ch.ivyteam.ivy.maven.log.LogCollector;

@MojoTest
@ExtendWith(ProjectExtension.class)
class TestValidateProjectMojo {

  private static final String USERS_YAML = """
    # yaml-language-server: $schema=https://json-schema.axonivy.com/14.0-dev/config/users.json
    Users:
      - Name: Alex
        Password: 123
        FullName: Alexander
        Mail: alex@alex.ch
    """;

  private ValidateProjectMojo mojo;

  @BeforeEach
  @InjectMojo(goal = ValidateProjectMojo.GOAL)
  void setUp(ValidateProjectMojo validate) {
    this.mojo = validate;
  }

  @Test
  void invalideUser() throws IOException {
    var projectDir = mojo.project.getBasedir().toPath();
    var configDir = projectDir.resolve("config");
    var usersFile = configDir.resolve("users.yaml");

    Files.createDirectories(configDir);
    String yaml = """
      # yaml-language-server: $schema=https://json-schema.axonivy.com/14.0-dev/config/users.json
      Users:
        - Name: Alex
          Password: 123
          FullName: Alexander
          Mail: alex@alex.ch
          Roles:
            - Gangster
      """;
    LogCollector log = new LogCollector();
    Files.writeString(usersFile, yaml);
    mojo.setLog(log);
    mojo.execute();
    assertThat(log.getWarnings()).isNotEmpty();
    assertThat(log.getWarnings().toString()).contains("config/users.yaml: User 'Alex' is configured to have role 'Gangster' which is not defined.");
  }

  @Test
  void duplicateUserInRequiredProject(@TempDir Path requiredProjectDir) throws IOException {
    writeUsersYaml(mojo.project.getBasedir().toPath());
    writeUsersYaml(requiredProjectDir);

    var requiredArtifact = mockArtifact("ch.ivyteam.project.test", "required-project", requiredProjectDir);
    Mockito.when(mojo.project.getArtifacts()).thenReturn(Set.of(requiredArtifact));

    var requiredMvnProject = mockMavenProject("required-project", requiredProjectDir);
    mockSession(requiredMvnProject);

    var log = new LogCollector();
    mojo.setLog(log);
    mojo.execute();
    System.out.println(log.getLogs().toString());
    assertThat(log.getLogs().toString()).contains("config/users.yaml: User 'Alex' is defined more than once. This can lead to misbehaviour because only one user is available during the simulation.");
  }

  private void writeUsersYaml(Path projectDir) throws IOException {
    var configDir = projectDir.resolve("config");
    Files.createDirectories(configDir);
    Files.writeString(configDir.resolve("users.yaml"), USERS_YAML);
  }

  private void mockSession(MavenProject... projects) {
    var session = Mockito.mock(MavenSession.class);
    Mockito.when(session.getAllProjects()).thenReturn(List.of(projects));
    mojo.session = session;
  }

  private static MavenProject mockMavenProject(String artifactId, Path basedir) {
    var project = Mockito.mock(MavenProject.class);
    Mockito.when(project.getGroupId()).thenReturn("ch.ivyteam.project.test");
    Mockito.when(project.getArtifactId()).thenReturn(artifactId);
    Mockito.when(project.getId()).thenReturn(artifactId);
    Mockito.when(project.getName()).thenReturn(artifactId);
    Mockito.when(project.getBasedir()).thenReturn(basedir.toFile());
    Mockito.when(project.getArtifacts()).thenReturn(Set.of());
    var selfArtifact = mockArtifact("ch.ivyteam.project.test", artifactId, basedir);
    Mockito.when(project.getArtifact()).thenReturn(selfArtifact);
    return project;
  }

  private static Artifact mockArtifact(String groupId, String artifactId, Path filePath) {
    var artifact = Mockito.mock(Artifact.class);
    Mockito.when(artifact.getGroupId()).thenReturn(groupId);
    Mockito.when(artifact.getArtifactId()).thenReturn(artifactId);
    Mockito.when(artifact.getFile()).thenReturn(filePath.toFile());
    return artifact;
  }
}
