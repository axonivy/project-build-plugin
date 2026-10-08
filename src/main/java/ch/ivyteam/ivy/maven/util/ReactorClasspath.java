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
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;

public class ReactorClasspath {

  private static final Map<MavenSession, Map<String, List<String>>> CLASSPATHS = new ConcurrentHashMap<>();

  private final ReactorSession reactorSession;
  private final MavenSession session;
  private final ProjectBuilder projectBuilder;
  private final Log log;
  private final Map<String, List<String>> sessionClasspaths;


  public ReactorClasspath(MavenSession session, ProjectBuilder projectBuilder, Log log) {
    this.session = session;
    this.log = log;
    this.reactorSession = new ReactorSession(session);
    this.projectBuilder = projectBuilder;
    this.sessionClasspaths = sessionClasspaths(session);
  }

  public void addProject(MavenProject project, Set<String> classpath) {
    var projectClasspath = new LinkedHashSet<String>();
    addCompileClasspath(project, projectClasspath);
    sessionClasspaths.put(projectKey(project), List.copyOf(projectClasspath));
    classpath.addAll(projectClasspath);
  }

  public void addRequiredProjects(Iterable<Artifact> artifacts, Set<String> classpath) {
    for (var artifact : artifacts) {
      if (artifact.getType().contains("iar")) {
        reactorSession.project(artifact)
          .or(() -> resolveProjectFromRepo(artifact))
          .ifPresent(p -> addReactorClasspath(p, classpath));
      }
    }
  }

  private Optional<MavenProject> resolveProjectFromRepo(Artifact artifact) {
    try {
      var pomArtifact = artifact.getArtifactId() + "-" + artifact.getBaseVersion() + ".pom";
      var pom = artifact.getFile().toPath().resolveSibling(pomArtifact);
      var request = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
      request.setResolveDependencies(true);
      var project = projectBuilder.build(pom.toFile(), request).getProject();
      return Optional.of(project);
    } catch (ProjectBuildingException ex) {
      log.error("Failed to resolve artifact POM for artifact: " + artifact, ex);
      return Optional.empty();
    }
  }

  private void addReactorClasspath(MavenProject project, Set<String> classpath) {
    var cached = sessionClasspaths.get(projectKey(project));
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

  private static Map<String, List<String>> sessionClasspaths(MavenSession session) {
    return CLASSPATHS.computeIfAbsent(session, _ -> new ConcurrentHashMap<>());
  }

}
