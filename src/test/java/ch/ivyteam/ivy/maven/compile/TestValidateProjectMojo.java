package ch.ivyteam.ivy.maven.compile;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import ch.ivyteam.ivy.Advisor;
import ch.ivyteam.ivy.maven.extension.ProjectExtension;
import ch.ivyteam.ivy.maven.log.LogCollector;
import ch.ivyteam.ivy.project.model.ProjectVersion;

@MojoTest
@ExtendWith(EnglishLocaleExtension.class)
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
    assertThat(log.getWarnings()).hasSize(8);
    assertThat(log.getWarnings().toString())
        .contains("config/users.yaml:[Alex] User 'Alex' is configured to have role 'Gangster' which is not defined.")
        .contains("config/webservice-clients.yaml:[test.name] The web service client key 'test.name' is not sanitized. Use 'testname' instead to avoid potential issues")
        .contains("config/webservice-clients.yaml:[test name] The web service client key 'test name' is not sanitized. Use 'test-name' instead to avoid potential issues")
        .contains("config/rest-clients.yaml:[test.name] The REST client key 'test.name' is not sanitized. Use 'testname' instead to avoid potential issues.")
        .contains("dataclass/validation/BusinessProcessData.d.json:[Test] The name 'Test' does not follow the Data Class Attribute naming convention")
        .contains("process/validation/TestIvyScript.p.json:[f6.output.code] Method expiryActivator of class ch.ivyteam.ivy.workflow.ITask is deprecated")
        .contains("config/rest-clients.yaml:[test name] The REST client key 'test name' is not sanitized. Use 'test-name' instead to avoid potential issues.")
        .contains("dialog/validation/TestDialog/TestDialog.xhtml:[9,9] Unknown element 'fooorm' in namespace 'h'");

    assertThat(log.getErrors()).hasSize(19);
    assertThat(log.getErrors().toString())
        .contains("config/roles.yaml:[HR Manager] Role 'HR Manager' has an unknown parent 'Manager'")
        .contains("config/variables.yaml:[Test] Variable 'Test' is defined multiple times in variables.yaml")
        .contains("dataclass/validation/BusinessProcessData.d.json:[#class] The namespace 'invalid' does not match the directory of the Data Class")
        .contains("process/validation/TestProcess.p.json:[f0.signature] Invalid character in signature name at position 1")
        .contains("process/validation/TestIvyScript.p.json:[f4.output.code] Variable 'wrong' cannot be resolved")        
        .contains("dialog/validation/TestForm/TestForm.f.json:[button1] Button action cannot be empty")
        .contains("config/databases.yaml:[testdb] The database connection key 'testdb' is duplicated in project 'empty-project'")
        .contains("dialog/validation/TestDialog/TestDialog.xhtml:[9,7] The element type \"h:form\" must be terminated by the matching end-tag \"</h:form>\"");
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
        .contains("Project is outdated (version: 140013). Convert it to the current version (" + ProjectVersion.CURRENT + ").");
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
        .contains("Skipping validator 'role'");
    assertThat(log.getErrors().toString())
        .doesNotContain("Role 'HR Manager' has an unknown parent 'Manager'")
        .contains("config/variables.yaml:[Test] Variable 'Test' is defined multiple times in variables.yaml");
  }

  @Test
  void excludedValidators_isCaseInsensitiveAndMatchesSimpleName() {
    mojo.excludeValidators = List.of("role", "webServiceClient");
    var log = new LogCollector();
    mojo.setLog(log);
    mojo.execute();

    assertThat(log.getErrors().toString())
        .doesNotContain("Role 'HR Manager' has an unknown parent 'Manager'");
    assertThat(log.getWarnings().toString())
        .doesNotContain("config/webservice-clients.yaml:[test.name] The web service client key 'test.name' is not sanitized. Use 'testname' instead to avoid potential issues");
  }

  @Test
  void summary_logsAtErrorLevel_whenErrorsArePresent() {
    var log = new LogCollector();
    mojo.setLog(log);
    mojo.execute();

    assertThat(log.getErrors().toString())
        .contains("Project validation summary")
        .contains("Errors:    ")
        .contains("Warnings:  8")
        .contains("Info:      0")
        .contains("Total:     ")
        .contains("Validation Codes: "
            + Advisor.getAdvisor().getDocBaseUrl() + "/en/reference/validation/index.html");
    assertThat(log.getWarnings().toString()).doesNotContain("Project validation summary");
    assertThat(log.getInfos().toString()).doesNotContain("Project validation summary");
  }

  @Test
  void summary_logsAtWarnLevel_whenOnlyWarningsArePresent() {
    mojo.excludeValidators = List.of("role", "variable", "dataclass", "process", "form", "database", "xhtml");
    var log = new LogCollector();
    mojo.setLog(log);
    mojo.execute();

    assertThat(log.getErrors()).isEmpty();
    assertThat(log.getWarnings().toString())
        .contains("Project validation summary")
        .contains("Errors:    0")
        .contains("Warnings:  5")
        .contains("Skipped validators (7):");
    assertThat(log.getInfos().toString()).doesNotContain("Project validation summary");
  }

  @Test
  void summary_logsAtInfoLevel_whenNoErrorsOrWarningsArePresent() {
    mojo.excludeValidators = List.of(
        "role", "database", "form", "dataclass", "process",
        "restclient", "webserviceclient", "variable", "user", "xhtml");
    var log = new LogCollector();
    mojo.setLog(log);
    mojo.execute();

    assertThat(log.getErrors()).isEmpty();
    assertThat(log.getWarnings()).isEmpty();
    assertThat(log.getInfos().toString())
        .contains("Project validation finished with no issues")
        .contains("Project validation summary")
        .contains("Errors:    0")
        .contains("Warnings:  0")
        .contains("Total:     0")
        .contains("Skipped validators (10):")
        .contains("- role")
        .contains("- database")
        .contains("- form")
        .contains("- dataclass")
        .contains("- process")
        .contains("- restclient")
        .contains("- webserviceclient")
        .contains("- variable")
        .contains("- xhtml")
        .contains("- user");
  }

  @Test
  void summary_isPrecededByPlainInfoLine_soJenkinsDoesNotMisattributeTheFailure() {
    var log = new LogCollector();
    mojo.setLog(log);
    mojo.execute();

    assertThat(log.getInfos().toString())
        .contains("Project validation finished with")
        .contains("files with findings");
  }

  @Test
  void summary_doesNotListSkippedValidators_whenNoneAreExcluded() {
    var log = new LogCollector();
    mojo.setLog(log);
    mojo.execute();

    assertThat(log.getErrors().toString()).doesNotContain("Skipped validators");
  }

}
