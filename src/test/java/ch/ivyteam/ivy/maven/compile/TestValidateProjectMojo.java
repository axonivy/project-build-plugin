package ch.ivyteam.ivy.maven.compile;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;

import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import ch.ivyteam.ivy.maven.extension.ProjectExtension;
import ch.ivyteam.ivy.maven.log.LogCollector;

@MojoTest
@ExtendWith(ProjectExtension.class)
class TestValidateProjectMojo {

  private ValidateProjectMojo mojo;

  @BeforeEach
  @InjectMojo(goal = ValidateProjectMojo.GOAL)
  void beforeEach(ValidateProjectMojo validate) {
    this.mojo = validate;
  }

  @Test
  void validate() throws IOException {
    var dir = mojo.project.getBasedir().toPath().resolve("config");
    Files.createDirectories(dir);

    var file = dir.resolve("users.yaml");
    var content = """
      # yaml-language-server: $schema=https://json-schema.axonivy.com/14.0-dev/config/users.json
      Users:
        - Name: Alex
          Roles:
            - Gangster
      """;
    Files.writeString(file, content);

    file = dir.resolve("roles.yaml");
    content = """
      # yaml-language-server: $schema=https://json-schema.axonivy.com/14.0-dev/config/roles.json
      Roles:
        - Id: HR Manager
          Parent: Manager
      """;
    Files.writeString(file, content);

    file = dir.resolve("webservice-clients.yaml");
    content = """
      WebServiceClients:
        test name:
          Name: Test
        test.name:
          Name: Another
      """;
    Files.writeString(file, content);

    var log = new LogCollector();
    mojo.setLog(log);
    mojo.execute();
    assertThat(log.getWarnings().toString())
        .contains("config/users.yaml: User 'Alex' is configured to have role 'Gangster' which is not defined.")
        .contains("config/webservice-clients.yaml: The web service client key 'test.name' should be sanitized to 'testname' to avoid potential issues. Use the name for a better readability.")
        .contains("config/webservice-clients.yaml: The web service client key 'test name' should be sanitized to 'test-name' to avoid potential issues. Use the name for a better readability.");
    assertThat(log.getErrors().toString())
        .contains("config/roles.yaml: Role 'HR Manager' has an unknown parent 'Manager'.");
  }
}
