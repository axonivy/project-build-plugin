package ch.ivyteam.ivy.maven.compile;

import java.util.List;

import org.apache.maven.artifact.Artifact;
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
import ch.ivyteam.ivy.project.validation.ProjectValidator;
import ch.ivyteam.ivy.project.validation.ProjectValidatorContext;
import ch.ivyteam.ivy.project.validation.ProjectValidatorResult.Message;
import ch.ivyteam.ivy.rest.client.config.impl.RestClientProjectValidator;
import ch.ivyteam.ivy.role.impl.RoleProjectValidator;
import ch.ivyteam.ivy.user.impl.UserProjectValidator;
import ch.ivyteam.ivy.vars.impl.VariableProjectValidator;
import ch.ivyteam.ivy.webservice.datamodel.impl.WebServiceClientProjectValidator;

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
    validateProject();
  }

  private void validateProject() {
    var time = System.currentTimeMillis();
    var ctx = new ContextBuilder(project, session).build();
    for (var validator : validators()) {
      var result = validator.validate(ctx);
      for (var message : result.messages()) {
        log(message);
      }
    }
    var duration = System.currentTimeMillis() - time;
    getLog().info("Validation finished in " + duration + "ms");
  }

  private List<ProjectValidator<?>> validators() {
    return List.of(
        new UserProjectValidator(),
        new RoleProjectValidator(),
        new WebServiceClientProjectValidator(),
        new VariableProjectValidator(),
        new RestClientProjectValidator());
  }

  private void log(Message message) {
    var mvnProjectUri = project.getBasedir().toURI();
    var path = mvnProjectUri.relativize(message.file()).getPath();
    switch (message.severity()) {
      case ERROR -> getLog().error(path + ": " + message.text());
      case WARN -> getLog().warn(path + ": " + message.text());
      case INFO -> getLog().info(path + ": " + message.text());
    }
  }

  private static class ContextBuilder {

    private final MavenProject project;
    private final MavenSession session;
    private final MavenDependencies dependencies;

    private ContextBuilder(MavenProject project, MavenSession session) {
      this.project = project;
      this.session = session;
      this.dependencies = MavenDependencies.of(project).session(session);
    }

    private ProjectValidatorContext build() {
      return ProjectValidatorContext.create().project(toProject()).allProjects(toAllProjects()).toContext();
    }

    private Project toProject() {
      return toProject(project)
          .required(toRequiredProjects())
          .dependent(toDependentProjects())
          .build();
    }

    private BasicProjectBuilder toProject(MavenProject p) {
      return BasicProject.create()
          .id(p.getId())
          .name(p.getName())
          .path(p.getBasedir().toPath());
    }

    private BasicProjectBuilder toProject(Artifact artifact) {
      return BasicProject.create()
          .id(artifact.getId())
          .name(artifact.getId())
          .path(dependencies.toPath(artifact));
    }

    private List<Project> toRequiredProjects() {
      return dependencies.required().stream()
          .map(this::toProject)
          .map(BasicProjectBuilder::build)
          .toList();
    }

    private List<Project> toDependentProjects() {
      return dependencies.dependent().stream()
          .map(this::toProject)
          .map(BasicProjectBuilder::build)
          .toList();
    }

    private List<Project> toAllProjects() {
      return session.getAllProjects().stream()
          .map(this::toProject)
          .map(BasicProjectBuilder::build)
          .toList();
    }
  }
}
