package ch.ivyteam.ivy.maven.compile;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import ch.ivyteam.ivy.project.model.Project;
import ch.ivyteam.ivy.project.validation.ProjectValidatorContext;
import ch.ivyteam.ivy.project.validation.ProjectValidatorResult.Message;
import ch.ivyteam.ivy.user.impl.UserProjectValidator;

/**
 * Validates an ivy Project with an ivyEngine.
 */
@Mojo(name = ValidateProjectMojo.GOAL, requiresDependencyResolution = ResolutionScope.COMPILE)
public class ValidateProjectMojo extends AbstractMojo {
  public static final String GOAL = "validateProject";
  /**
   * Set to <code>false</code> to perform the validation of ivyScript code
   * within ivy processes.
   * @since 8.0.3
   */
  @Parameter(property = "ivy.script.validation.skip", defaultValue = "false")
  boolean skipScriptValidation;

  @Parameter(property = "project", required = true, readonly = true)
  MavenProject project;

  @Override
  public void execute() {
    if (skipScriptValidation) {
      getLog().info("Skipping ivy script validation");
      return;
    }
    validateUser();
  }

  private void validateUser() {
    Project mockProject = new ProjectMock("test", project.getBasedir().toPath());
    var ctx = ProjectValidatorContext.create(mockProject);
    var result = new UserProjectValidator().validate(ctx);
    for (var message : result.messages()) {
      log(message);
    }
  }

  private void log(Message message) {
    switch (message.severity()) {
      case ERROR -> getLog().error(message.text());
      case WARN -> getLog().warn(message.text());
      default -> {}
    }
  }
}
