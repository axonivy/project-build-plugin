package ch.ivyteam.ivy.maven.util;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

/**
 * @since 9.2.2
 */
public class MavenDependencies {
  private final MavenProject project;
  private final MavenSession session;
  private String typeFilter;

  public MavenDependencies(MavenProject project, MavenSession session) {
    this.project = project;
    this.session = session;
  }

  public MavenDependencies type(String newTypeFilter) {
    this.typeFilter = newTypeFilter;
    return this;
  }

  public List<File> localTransient() {
    return stream(project.getArtifacts())
      .filter(this::isLocalDep)
      .filter(this::include)
      .map(this::toFile)
      .collect(Collectors.toList());
  }

  private boolean isLocalDep(Artifact artifact) {
    return artifact.getDependencyTrail().stream()
      .filter(dep -> dep.contains(":iar")) // iar or iar-integration-test
      .filter(dep -> !dep.startsWith(project.getGroupId() + ":" + project.getArtifactId() + ":"))
      .findAny()
      .isEmpty();
  }

  public List<File> all() {
    return stream(project.getArtifacts())
      .filter(this::include)
      .map(this::toFile)
      .collect(Collectors.toList());
  }

  private static Stream<Artifact> stream(Set<Artifact> deps) {
    if (deps == null) {
      return Stream.empty();
    }
    return deps.stream();
  }

  private File toFile(Artifact artifact) {
    return findReactorProject(artifact)
      .map(MavenProject::getBasedir)
      .orElse(artifact.getFile());
  }

  private boolean include(Artifact artifact) {
    if (typeFilter == null) {
      return true;
    }
    if (artifact == null) {
      return false;
    }
    return typeFilter.equals(artifact.getType());
  }

  private Optional<MavenProject> findReactorProject(Artifact artifact) {
    if (session == null) {
      return Optional.empty();
    }
    var projects = session.getProjectMap();
    if (projects == null) {
      return Optional.empty();
    }
    String artifactKey = artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion();
    MavenProject reactorProject = projects.get(artifactKey);
    return Optional.ofNullable(reactorProject);
  }

}
