package ch.ivyteam.ivy.maven.util;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
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
  private MavenSession session;
  private String typeFilter;

  private MavenDependencies(MavenProject project) {
    this.project = project;
  }

  public static MavenDependencies of(MavenProject project) {
    return new MavenDependencies(project);
  }

  public MavenDependencies session(MavenSession mvnSession) {
    this.session = mvnSession;
    return this;
  }

  public MavenDependencies typeFilter(String filter) {
    this.typeFilter = filter;
    return this;
  }

  public List<Path> localTransient() {
    return stream(project.getArtifacts())
        .filter(this::isLocalDep)
        .map(Artifact::getFile)
        .map(File::toPath)
        .filter(Objects::nonNull)
        .toList();
  }

  public List<MavenProject> dependent() {
    if (session == null) {
      return List.of();
    }
    return session.getAllProjects().stream()
        .filter(p -> p.getArtifacts().stream()
            .anyMatch(a -> a.getGroupId().equals(project.getGroupId())
                && a.getArtifactId().equals(project.getArtifactId()) && "iar".equals(a.getType())))
        .toList();
  }

  public List<Artifact> required() {
    return stream(project.getArtifacts())
        .filter(this::include)
        .toList();
  }

  public List<Path> all() {
    return stream(project.getArtifacts())
        .filter(this::include)
        .map(this::toPath)
        .collect(Collectors.toList());
  }

  private boolean isLocalDep(Artifact artifact) {
    return artifact.getDependencyTrail().stream()
        .filter(dep -> dep.contains(":iar")) // iar or iar-integration-test
        .filter(dep -> !dep.startsWith(project.getGroupId() + ":" + project.getArtifactId() + ":"))
        .findAny()
        .isEmpty();
  }

  private static Stream<Artifact> stream(Set<Artifact> deps) {
    if (deps == null) {
      return Stream.empty();
    }
    return deps.stream();
  }

  public Path toPath(Artifact artifact) {
    return findReactorProject(artifact)
        .map(p -> p.getBasedir().toPath())
        .orElse(artifact.getFile().toPath());
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
    return session.getAllProjects().stream()
        .filter(p -> p.getArtifact().equals(artifact))
        .findAny();
  }
}
