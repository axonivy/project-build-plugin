package ch.ivyteam.ivy.maven.compile;

import java.util.List;

import javax.inject.Inject;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;

import ch.ivyteam.ivy.java.config.index.JavaIndex;
import ch.ivyteam.ivy.maven.project.ProjectContextFactory;
import ch.ivyteam.ivy.maven.project.ProjectContextFactory.ProjectCtxt;
import ch.ivyteam.ivy.project.model.ProjectVersion;
import ch.ivyteam.ivy.project.validation.ProjectValidator;
import ch.ivyteam.ivy.project.validation.ProjectValidatorContext;

/**
 * Validates an ivy Project.
 */
@Mojo(name = ValidateProjectMojo.GOAL, requiresDependencyResolution = ResolutionScope.COMPILE, threadSafe = true)
public class ValidateProjectMojo extends AbstractMojo {
  public static final String GOAL = "validateProject";

  /**
   * Set to <code>true</code> to completely skip the project validation.
   * @since 14.0.0
   */
  @Parameter(property = "ivy.validation.skip", defaultValue = "false")
  boolean skipProjectValidation;

  /**
   * Set to <code>true</code> to fail the build if project validation reports errors. 
   * 
   * @since 14.0.2
   */
  @Parameter(property = "ivy.validation.failOnError", defaultValue = "false")
  boolean failOnError;

  /**
   * Set to <code>true</code> to fail the build if project validation reports warnings,
   *
   * @see #failOnError
   * @since 14.0.2
   */
  @Parameter(property = "ivy.validation.failOnWarning", defaultValue = "false")
  boolean failOnWarning;

  /**
   * Set to <code>false</code> to perform the validation of ivyScript code
   * within ivy processes.
   *
   * @deprecated use {@link #skipProjectValidation} (property <code>ivy.validation.skip</code>)
   *             instead, which skips the validation entirely rather than
   *             just the script validation part. This parameter will be
   *             removed in a future version.
   * @since 8.0.3
   */
  @Deprecated(since = "14.0.0", forRemoval = true)
  @Parameter(property = "ivy.script.validation.skip", defaultValue = "false")
  boolean skipScriptValidation;

  /**
   * List of validators to deliberately exclude from the validation run.
   * The following are currently included:
   * <ul>
   * <li><code>role</code></li>
   * <li><code>database</code></li>
   * <li><code>form</code></li>
   * <li><code>dataclass</code></li>
   * <li><code>process</code></li>
   * <li><code>restclient</code></li>
   * <li><code>webserviceclient</code></li>
   * <li><code>variable</code></li>
   * <li><code>user</code></li>
   * <li><code>xhtml</code></li>
   * </ul>
   *
   * Configure a fixed set in the POM:
   * <pre>
   * &lt;configuration&gt;
   * &lt;excludeValidators&gt;
   * &lt;validator&gt;process&lt;/validator&gt;
   * &lt;validator&gt;role&lt;/validator&gt;
   * &lt;/excludeValidators&gt;
   * &lt;/configuration&gt;
   * </pre>
   *
   * Or set it directly from the command line as a comma-separated list,
   * without touching the POM:
   * <pre>
   * mvn install -Divy.validation.excludeValidators=role,webservice
   * </pre>
   * @since 14.0.0
   */
  @Parameter(property = "ivy.validation.excludeValidators")
  List<String> excludeValidators;

  @Parameter(property = "project", required = true, readonly = true)
  MavenProject project;

  @Parameter(defaultValue = "${session}", readonly = true)
  MavenSession session;

  @Inject
  ProjectBuilder projectBuilder;

  @Override
  public void execute() throws MojoFailureException {
    if (isSkip()) {
      getLog().info("Skipping ivy project validation");
      return;
    }
    validateProject();
  }

  private boolean isSkip() {
    if (skipScriptValidation) {
      getLog().warn("The parameter 'ivy.script.validation.skip' is deprecated and will be removed "
          + "in a future version. Use 'ivy.validation.skip' instead.");
      return true;
    }
    return skipProjectValidation;
  }

  private void validateProject() throws MojoFailureException {
    var start = System.currentTimeMillis();
    var projectCtx = new ProjectContextFactory(project, session, projectBuilder, getLog()).create();

    var version = ProjectVersion.of(projectCtx.project());
    if (!version.isLatest()) {
      if (version.isRunnable()) {
        var message = "Project is runnable (version: " + version + ") but not the latest version: " + ProjectVersion.CURRENT;
        getLog().warn(message);
        if (failOnWarning) {
          throw new MojoFailureException(message);
        }
      } else {
        var message = "Project is outdated (version: " + version + "). Convert it to the current version (" + ProjectVersion.CURRENT + ").";
        getLog().error(message);
        if (failOnError) {
          throw new MojoFailureException(message);
        }
      }
      return;
    }

    var filter = new ValidatorFilter(excludeValidators, getLog());
    var validators = filter.apply(ProjectValidator.all());

    var reporter = new ValidationReporter(getLog(), project);
    var ctx = validationContextOf(projectCtx);
    for (var validator : validators) {
      validator.validate(ctx).messages().forEach(reporter::report);
    }
    reporter.logSummary(project.getName(), System.currentTimeMillis() - start, filter.skipped());
    if (failOnError && reporter.hasErrors()) {
      throw new MojoFailureException("Project validation failed because of validation errors. See the validation summary above.");
    }
    if (failOnWarning && reporter.hasWarnings()) {
      throw new MojoFailureException("Project validation failed because of validation warnings. See the validation summary above.");
    }
  }

  private static ProjectValidatorContext validationContextOf(ProjectCtxt p) {
    return ProjectValidatorContext.create()
        .project(p.project())
        .isMaven(true)
        .allProjects(p.all())
        .javaIndex(JavaIndex.of(p.project(), p.classLoader()))
        .toContext();
  }

}
