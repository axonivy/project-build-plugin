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

  private ValidateProjectMojo mojo;

  @BeforeEach
  @InjectMojo(goal = ValidateProjectMojo.GOAL)
  void setUp(ValidateProjectMojo validate) {
    this.mojo = validate;
  }

  @Test
  void invalideUser() throws IOException {
    String yaml = """
      # yaml-language-server: $schema=https://json-schema.axonivy.com/14.0-dev/config/users.json
      Users:
        - Name: Alex
          Roles:
            - Gangster
      """;
    writeYaml(mojo.project.getBasedir().toPath(), "users", yaml);
    LogCollector log = new LogCollector();
    mojo.setLog(log);
    mojo.execute();
    assertThat(log.getWarnings()).isNotEmpty();
    assertThat(log.getWarnings().toString()).contains("config/users.yaml: User 'Alex' is configured to have role 'Gangster' which is not defined.");
  }

  @Test
  void invalideRole() throws IOException {
    String yaml = """
      # yaml-language-server: $schema=https://json-schema.axonivy.com/14.0-dev/config/roles.json
      Roles:
        - Id: HR Manager
          Parent: Manager
      """;
    writeYaml(mojo.project.getBasedir().toPath(), "roles", yaml);
    LogCollector log = new LogCollector();
    mojo.setLog(log);
    mojo.execute();
    assertThat(log.getErrors()).isNotEmpty();
    assertThat(log.getErrors().toString()).contains("config/roles.yaml: Role 'HR Manager' has an unknown parent 'Manager'.");
  }

  @Test
  void invalideWebService() throws IOException {
    var projectDir = mojo.project.getBasedir().toPath();
    var configDir = projectDir.resolve("config");
    var rolesFile = configDir.resolve("webservice-clients.yaml");

    Files.createDirectories(configDir);
    String yaml = """
      WebServiceClients:
        test name:
          Name: Test
        test.name:
          Name: Another
      """;
    LogCollector log = new LogCollector();
    Files.writeString(rolesFile, yaml);
    mojo.setLog(log);
    mojo.execute();
    assertThat(log.getWarnings()).isNotEmpty();
    assertThat(log.getWarnings().toString()).contains("config/webservice-clients.yaml: The web service client key 'test.name' should be sanitized to 'testname' to avoid potential issues. Use the name for a better readability.");
    assertThat(log.getWarnings().toString()).contains("config/webservice-clients.yaml: The web service client key 'test name' should be sanitized to 'test-name' to avoid potential issues. Use the name for a better readability.");
  }

  @Test
  void duplicateUserInOtherProject(@TempDir Path requiredProjectDir) throws IOException {
    var content = """
      # yaml-language-server: $schema=https://json-schema.axonivy.com/14.0-dev/config/users.json
      Users:
        - Name: Alex
          """;
    writeYaml(mojo.project.getBasedir().toPath(), "users", content);
    writeYaml(requiredProjectDir, "users", content);

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

  @Test
  void duplicateRoleInOtherProject(@TempDir Path requiredProjectDir) throws IOException {
    var content1 = """
      Roles:
        - Id: Manager
        - Id: Employee
          Parent: Manager
      """;
    writeYaml(mojo.project.getBasedir().toPath(), "roles", content1);
    var content2 = """
      Roles:
        - Id: Admin
        - Id: Employee
          Parent: Admin
      """;
    writeYaml(requiredProjectDir, "roles", content2);

    var requiredArtifact = mockArtifact("ch.ivyteam.project.test", "required-project", requiredProjectDir);
    Mockito.when(mojo.project.getArtifacts()).thenReturn(Set.of(requiredArtifact));

    var requiredMvnProject = mockMavenProject("required-project", requiredProjectDir);
    mockSession(requiredMvnProject);

    var log = new LogCollector();
    mojo.setLog(log);
    mojo.execute();
    assertThat(log.getLogs().toString()).contains("config/roles.yaml: Role 'Employee' exists in multiple projects with a different parent role. Maybe parent role should be 'Admin' instead of 'Manager'.");
  }

  private void writeYaml(Path projectDir, String name, String content) throws IOException {
    var configDir = projectDir.resolve("config");
    Files.createDirectories(configDir);
    Files.writeString(configDir.resolve(name + ".yaml"), content);
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
