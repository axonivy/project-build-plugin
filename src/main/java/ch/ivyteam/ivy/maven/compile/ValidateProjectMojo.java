package ch.ivyteam.ivy.maven.compile;

import java.nio.file.Path;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import ch.ivyteam.ivy.maven.util.MavenDependencies;
import ch.ivyteam.ivy.project.model.Project;
import ch.ivyteam.ivy.project.model.basic.BasicProject;
import ch.ivyteam.ivy.project.model.basic.BasicProjectBuilder;
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

  @Parameter(defaultValue = "${session}", readonly = true)
  MavenSession session;

  @Override
  public void execute() {
    if (skipScriptValidation) {
      getLog().info("Skipping ivy script validation");
      return;
    }
    validateUser();
  }

  private void validateUser() {
    var ctx = ProjectValidatorContext.create(toProject(), toAllProjects());
    var result = new UserProjectValidator().validate(ctx);
    for (var message : result.messages()) {
      log(message);
    }
  }

  private void log(Message message) {
    var mvnProjectUri = project.getBasedir().toURI();
    var path = mvnProjectUri.relativize(message.file()).getPath();
    switch (message.severity()) {
      case ERROR -> getLog().error(path + ": " + message.text());
      case WARN -> getLog().warn(path + ": " + message.text());
      case INFO -> getLog().info(path + ": " + message.text());
      default -> {}
    }
  }

  private Project toProject() {
    return toProject(project)
        .required(toRequiredProjects())
        .dependent(toDependentProjects())
        .build();
  }

  private static BasicProjectBuilder toProject(MavenProject p) {
    return BasicProject.create()
        .id(p.getId())
        .name(p.getName())
        .path(p.getBasedir().toPath());
  }

  private static BasicProjectBuilder toProject(Path p) {
    return BasicProject.create()
        .id(p.toFile().getName())
        .name(p.toFile().getName())
        .path(p);
  }

  private List<Project> toRequiredProjects() {
    return MavenDependencies.of(project).session(session).all().stream().map(path -> toProject(path).build()).toList();
  }

  private List<Project> toDependentProjects() {
    return MavenDependencies.of(project).session(session).dependents().stream().map(mvnProject -> toProject(mvnProject).build())
        .toList();
  }

  private List<Project> toAllProjects() {
    return session.getAllProjects().stream().map(mvnProject -> toProject(mvnProject).build()).toList();
  }

}
