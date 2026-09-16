package ch.ivyteam.ivy.maven.util;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

public class ReactorClasspath {

  private static final Map<String, List<String>> CLASSPATHS = new ConcurrentHashMap<>();

  private final ReactorSession reactorSession;

  public ReactorClasspath(MavenSession session) {
    this.reactorSession = new ReactorSession(session);
  }

  public void addProject(MavenProject project, Set<String> classpath) {
    var projectClasspath = new LinkedHashSet<String>();
    addCompileClasspath(project, projectClasspath);
    CLASSPATHS.put(projectKey(project), List.copyOf(projectClasspath));
    classpath.addAll(projectClasspath);
  }

  public void addRequiredProjects(Iterable<Artifact> artifacts, Set<String> classpath) {
    for (var artifact : artifacts) {
      if (artifact.getType().contains("iar")) {
        reactorSession.project(artifact).ifPresent(project -> addReactorClasspath(project, classpath));
      }
    }
  }

  private void addReactorClasspath(MavenProject project, Set<String> classpath) {
    var cached = CLASSPATHS.get(projectKey(project));
    if (cached == null) {
      addProject(project, classpath);
    } else {
      classpath.addAll(cached);
    }
  }

  private void addCompileClasspath(MavenProject project, Set<String> classpath) {
    try {
      classpath.addAll(project.getCompileClasspathElements());
      project.getArtifactMap().values().stream()
          .map(Artifact.class::cast)
          .map(this.reactorSession::toPathIAR)
          .flatMap(Optional::stream)
          .map(Path::toString)
          .forEach(classpath::add);
    } catch (DependencyResolutionRequiredException ex) {
      throw new RuntimeException(ex);
    }
  }

  private String projectKey(MavenProject project) {
    return project.getGroupId() + ":" + project.getArtifactId() + ":" + project.getVersion();
  }

}
