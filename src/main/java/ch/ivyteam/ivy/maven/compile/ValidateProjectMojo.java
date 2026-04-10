package ch.ivyteam.ivy.maven.compile;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

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

  @Override
  public void execute() {
    if (skipScriptValidation) {
      getLog().info("Skipping ivy script validation");
      return;
    }
  }
}
