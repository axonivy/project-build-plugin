package ch.ivyteam.ivy.maven.compile;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;

import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import ch.ivyteam.ivy.maven.extension.ProjectExtension;
import ch.ivyteam.ivy.maven.log.LogCollector;

@MojoTest
class TestValidateProjectMojo {

  @RegisterExtension
  static ProjectExtension projectExtension = new ProjectExtension("src/test/resources/validation/");

  private ValidateProjectMojo mojo;

  @BeforeEach
  @InjectMojo(goal = ValidateProjectMojo.GOAL)
  void beforeEach(ValidateProjectMojo validate) {
    this.mojo = validate;
  }

  @Test
  void validate() {
    var log = new LogCollector();
    mojo.setLog(log);
    mojo.execute();
    assertThat(log.getWarnings().toString())
        .contains("config/users.yaml: User 'Alex' is configured to have role 'Gangster' which is not defined.")
        .contains("config/webservice-clients.yaml: The web service client key 'test.name' should be sanitized to 'testname' to avoid potential issues. Use the name for a better readability.")
        .contains("config/webservice-clients.yaml: The web service client key 'test name' should be sanitized to 'test-name' to avoid potential issues. Use the name for a better readability.")
        .contains("config/rest-clients.yaml: The rest client key 'test.name' should be sanitized to 'testname' to avoid potential issues. Use the name for a better readability.")
        .contains("config/rest-clients.yaml: The rest client key 'test name' should be sanitized to 'test-name' to avoid potential issues. Use the name for a better readability.");
    assertThat(log.getErrors().toString())
        .contains("config/roles.yaml: Role 'HR Manager' has an unknown parent 'Manager'.")
        .contains("config/variables.yaml: Variable 'Test' is defined multiple times in variables.yaml.")
        .contains("dataclasses/validation/BusinessProcessData.d.json: The namespace 'invalid' does not match the directory of the Data Class.");
  }

  @Test
  void validate_outdated() throws IOException {
    var dir = mojo.project.getBasedir().toPath();
    Files.createDirectories(dir);
    var file = dir.resolve(".ivyproject");
    var content = """
      name=validation
      version=140013
      """;

    Files.writeString(file, content);
    var log = new LogCollector();
    mojo.setLog(log);
    mojo.execute();
    assertThat(log.getWarnings().toString())
        .contains("Project is outdated (version: 140013). Convert the project to the latest version.");
  }
}
