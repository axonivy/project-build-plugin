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
public interface MavenDependencies {

  static List<Path> localTransient(MavenProject project) {
    return stream(project.getArtifacts())
        .filter(artifact -> isLocalDep(artifact, project))
        .map(Artifact::getFile)
        .map(File::toPath)
        .filter(Objects::nonNull)
        .toList();
  }

  static List<Path> all(MavenProject project, MavenSession session, String typeFilter) {
    return stream(project.getArtifacts())
        .filter(a -> include(typeFilter, a))
        .map(a -> toFile(session, a))
        .map(File::toPath)
        .collect(Collectors.toList());
  }

  static List<Path> all(MavenProject project, String typeFilter) {
    return all(project, null, typeFilter);
  }

  private static boolean isLocalDep(Artifact artifact, MavenProject project) {
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

  private static File toFile(MavenSession session, Artifact artifact) {
    return findReactorProject(session, artifact)
        .map(MavenProject::getBasedir)
        .orElse(artifact.getFile());
  }

  private static boolean include(String typeFilter, Artifact artifact) {
    if (typeFilter == null) {
      return true;
    }
    if (artifact == null) {
      return false;
    }
    return typeFilter.equals(artifact.getType());
  }

  private static Optional<MavenProject> findReactorProject(MavenSession session, Artifact artifact) {
    if (session == null) {
      return Optional.empty();
    }
    return session.getAllProjects().stream()
        .filter(p -> p.getArtifact().equals(artifact))
        .findAny();
  }
}
