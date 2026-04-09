package ch.ivyteam.ivy.maven.compile;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;

import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
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
  void setUp(ValidateProjectMojo validate) {
    this.mojo = validate;
  }

  @Test
  void invalideUser() throws IOException, MojoExecutionException, MojoFailureException {
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
    assertThat(log.getWarnings().toString()).contains("User 'Alex' is configured to have role 'Gangster' which is not defined.");
  }
}
