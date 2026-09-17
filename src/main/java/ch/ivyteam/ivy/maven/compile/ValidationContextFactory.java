package ch.ivyteam.ivy.maven.compile;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import ch.ivyteam.ivy.java.config.index.JavaIndex;
import ch.ivyteam.ivy.maven.util.MavenDependencies;
import ch.ivyteam.ivy.maven.util.ReactorClasspath;
import ch.ivyteam.ivy.maven.util.ReactorSession;
import ch.ivyteam.ivy.project.model.ProjectModel;
import ch.ivyteam.ivy.project.model.basic.BasicProject;
import ch.ivyteam.ivy.project.model.basic.BasicProjectBuilder;
import ch.ivyteam.ivy.project.validation.ProjectValidatorContext;

class ValidationContextFactory {

  private final MavenProject project;
  private final MavenSession session;
  private final MavenDependencies dependencies;
  private final ReactorClasspath reactorClasspath;
  private final Log log;

  ValidationContextFactory(MavenProject project, MavenSession session, Log log) {
    this.project = project;
    this.session = session;
    this.dependencies = MavenDependencies.of(project).session(session);
    this.reactorClasspath = new ReactorClasspath(session);
    this.log = log;
  }

  ProjectValidatorContext create() {
    var rootProject = toProject();
    if (log.isDebugEnabled()) {
      log.debug("[graph] Project graph for: " + project.getId());
      dumpTree(rootProject, 0);
    }
    var ctx = ProjectValidatorContext.create()
        .project(rootProject)
        .isMaven(true)
        .allProjects(toAllProjects());
    ctx.javaIndex(JavaIndex.of(rootProject, toClassLoader()));
    return ctx.toContext();
  }

  private void dumpTree(ProjectModel p, int depth) {
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

  private ProjectModel toProject() {
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
        .path(new ReactorSession(session).toPathBasedir(artifact));
  }

  private List<ProjectModel> toRequiredProjects() {
    return dependencies.required().stream()
        .map(this::toProject)
        .map(BasicProjectBuilder::build)
        .toList();
  }

  private List<ProjectModel> toDependentProjects() {
    return dependencies.dependent().stream()
        .map(this::toProject)
        .map(BasicProjectBuilder::build)
        .toList();
  }

  private List<ProjectModel> toAllProjects() {
    return session.getAllProjects().stream()
        .map(this::toProject)
        .map(BasicProjectBuilder::build)
        .toList();
  }

  private ClassLoader toClassLoader() {
    var classpath = new LinkedHashSet<String>();
    reactorClasspath.addProject(project, classpath);
    reactorClasspath.addRequiredProjects(dependencies.required(), classpath);
    addValidationRuntimeClasses(classpath);
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
  }

  private void addValidationRuntimeClasses(Set<String> classpath) {
    addClassLocation("ch.ivyteam.ivy.process.intermediateevent.beans.FileIntermediateEventBean", classpath);
    addClassLocation("ch.ivyteam.ivy.process.extension.beans.Wait", classpath);
  }

  private void addClassLocation(String className, Set<String> classpath) {
    try {
      var location = Class.forName(className, false, getClass().getClassLoader())
          .getProtectionDomain().getCodeSource().getLocation();
      classpath.add(Path.of(location.toURI()).toString());
    } catch (Exception e) {
      throw new IllegalStateException("Cannot add validation runtime class: " + className, e);
    }
  }
}
