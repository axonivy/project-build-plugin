package ch.ivyteam.ivy.maven.compile;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

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
        .contains("config/users.yaml [Alex]: User 'Alex' is configured to have role 'Gangster' which is not defined.")
        .contains("config/webservice-clients.yaml [test.name]: The web service client key 'test.name' should be sanitized to 'testname' to avoid potential issues. Use the name for a better readability.")
        .contains("config/webservice-clients.yaml [test name]: The web service client key 'test name' should be sanitized to 'test-name' to avoid potential issues. Use the name for a better readability.")
        .contains("config/rest-clients.yaml [test.name]: The rest client key 'test.name' should be sanitized to 'testname' to avoid potential issues. Use the name for a better readability.")
        .contains("dataclasses/validation/BusinessProcessData.d.json [Test]: The name of the Attribute 'Test' starts with an uppercasename. It should not start with an uppercase or a single lowercase letter.")
        .contains("config/rest-clients.yaml [test name]: The rest client key 'test name' should be sanitized to 'test-name' to avoid potential issues. Use the name for a better readability.");

    assertThat(log.getErrors().toString())
        .contains("config/roles.yaml [HR Manager]: Role 'HR Manager' has an unknown parent 'Manager'.")
        .contains("config/variables.yaml [Test]: Variable 'Test' is defined multiple times in variables.yaml.")
        .contains("dataclasses/validation/BusinessProcessData.d.json [#class]: The namespace 'invalid' does not match the directory of the Data Class.")
        .contains("processes/validation/TestProcess.p.json [19F039C4FF9700FD-f0]: Invalid character in signaturename at position 1")
        .contains("src_hd/validation/TestForm/TestForm.f.json [button1]: Button action cannot be empty")
        .contains("config/databases.yaml [testdb]: The database connection key 'testdb' is duplicated in the same project");
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
    assertThat(log.getErrors().toString())
        .contains("Project is outdated (version: 140013). Convert the project to the latest version.");
  }

  @Test
  void skip_completelySkipsValidation() {
    mojo.skipProjectValidation = true;
    var log = new LogCollector();
    mojo.setLog(log);
    mojo.execute();

    assertThat(log.getInfos().toString()).contains("Skipping ivy project validation");
    assertThat(log.getErrors()).isEmpty();
    assertThat(log.getWarnings()).isEmpty();
  }

  @SuppressWarnings("removal")
  @Test
  void deprecatedSkipScriptValidation_stillSkipsEntireValidation() {
    mojo.skipScriptValidation = true;
    var log = new LogCollector();
    mojo.setLog(log);
    mojo.execute();

    assertThat(log.getWarnings().toString())
        .contains("The parameter 'ivy.script.validation.skip' is deprecated");
    assertThat(log.getInfos().toString()).contains("Skipping ivy project validation");
    assertThat(log.getErrors()).isEmpty();
  }

  @Test
  void excludedValidators_excludesConfiguredValidatorByKeyword() {
    mojo.excludeValidators = List.of("Role");
    var log = new LogCollector();
    mojo.setLog(log);
    mojo.execute();

    assertThat(log.getInfos().toString())
        .contains("Skipping validator 'role' (excluded by configuration)");
    assertThat(log.getErrors().toString())
        .doesNotContain("Role 'HR Manager' has an unknown parent 'Manager'.")
        // other validators still run
        .contains("config/variables.yaml [Test]: Variable 'Test' is defined multiple times in variables.yaml.");
  }

  @Test
  void excludedValidators_isCaseInsensitiveAndMatchesSimpleName() {
    mojo.excludeValidators = List.of("role", "webServiceClient");
    var log = new LogCollector();
    mojo.setLog(log);
    mojo.execute();

    assertThat(log.getErrors().toString())
        .doesNotContain("Role 'HR Manager' has an unknown parent 'Manager'.");
    assertThat(log.getWarnings().toString())
        .doesNotContain("config/webservice-clients.yaml [test.name]: The web service client key 'test.name' should be sanitized to 'testname' to avoid potential issues. Use the name for a better readability.");
  }

}
