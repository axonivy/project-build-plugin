package ch.ivyteam.ivy.maven.compile;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import ch.ivyteam.ivy.java.config.index.JavaIndex;
import ch.ivyteam.ivy.maven.util.MavenDependencies;
import ch.ivyteam.ivy.project.model.Project;
import ch.ivyteam.ivy.project.model.ProjectVersion;
import ch.ivyteam.ivy.project.model.basic.BasicProject;
import ch.ivyteam.ivy.project.model.basic.BasicProjectBuilder;
import ch.ivyteam.ivy.project.validation.ProjectValidator;
import ch.ivyteam.ivy.project.validation.ProjectValidatorContext;
import ch.ivyteam.ivy.project.validation.ProjectValidatorResult.Message;

/**
 * Validates an ivy Project with an ivyEngine.
 */
@Mojo(name = ValidateProjectMojo.GOAL, requiresDependencyResolution = ResolutionScope.COMPILE, threadSafe = true)
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
    var ctx = new ContextBuilder(project, session, getLog()).build();
    var version = ProjectVersion.of(ctx.project());
    if (!version.isLatest()) {
      getLog().error("Project is outdated (version: " + version + "). Convert the project to the latest version.");
      return;
    }
    for (var validator : ProjectValidator.all()) {
      var result = validator.validate(ctx);
      for (var message : result.messages()) {
        log(message);
      }
    }
    var duration = System.currentTimeMillis() - time;
    getLog().info("Validation finished in " + duration + "ms");
  }

  private void log(Message message) {
    var mvnProjectUri = project.getBasedir().toURI();
    var path = project.getName() + " - " + mvnProjectUri.relativize(message.file()).getPath();
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
    private final Log log;

    private ContextBuilder(MavenProject project, MavenSession session, Log log) {
      this.project = project;
      this.session = session;
      this.dependencies = MavenDependencies.of(project).session(session);
      this.log = log;
    }

    private ProjectValidatorContext build() {
      var rootProject = toProject();
      if (log.isDebugEnabled()) {
        log.debug("[graph] Project graph for: " + project.getId());
        dumpTree(rootProject, 0);
      }

      var ctx = ProjectValidatorContext.create()
          .project(rootProject)
          .isMaven(true)
          .allProjects(toAllProjects());
      var cl = toClassLoader();
      ctx.javaIndex(JavaIndex.of(cl));

      return ctx.toContext();
    }

    private void dumpTree(Project p, int depth) {
      var indent = "  ".repeat(depth);
      var required = p.debs().allRequired().toList();
      var dependent = p.debs().allDependent().toList();

      log.debug("[graph] " + indent + p.id()
          + "  [required=" + required.size() + ", dependent=" + dependent.size() + "]");
      if (!required.isEmpty()) {
        log.debug("[graph] " + indent + "  required:");
        for (var r : required) {
          dumpTree(r, depth + 2);
        }
      }
      if (!dependent.isEmpty()) {
        log.debug("[graph] " + indent + "  dependent:");
        for (var d : dependent) {
          dumpTree(d, depth + 2);
        }
      }
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

    private ClassLoader toClassLoader() {
      try {
        var classpath = project.getCompileClasspathElements();
        var urls = classpath.stream()
            .map(path -> {
              try {
                return Path.of(path).toUri().toURL();
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
            })
            .toArray(URL[]::new);
        if (log.isDebugEnabled()) {
          for (var url : urls) {
            log.debug("Classpath URL: " + url);
          }
        }
        return new URLClassLoader(urls);
      } catch (DependencyResolutionRequiredException ex) {
        throw new RuntimeException(ex);
      }
    }
  }
}
