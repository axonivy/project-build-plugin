package ch.ivyteam.ivy.maven.util;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

public class ReactorSession {

  private final MavenSession session;

  public ReactorSession(MavenSession session) {
    this.session = session;
  }

  public Optional<MavenProject> project(Artifact artifact) {
    if (session == null) {
      return Optional.empty();
    }
    return session.getAllProjects().stream()
        .filter(project -> project.getArtifact().equals(artifact))
        .findAny();
  }

  public Path toPathBasedir(Artifact artifact) {
    return project(artifact)
        .map(p -> p.getBasedir().toPath())
        .orElse(artifact.getFile().toPath());
  }

  public Optional<Path> toPathIAR(Artifact artifact) {
    var reactorProject = project(artifact);
    if (reactorProject.isPresent()) {
      return Optional.ofNullable(reactorProject.get().getArtifact().getFile()).map(File::toPath);
    }
    if (artifact.getFile() != null) { // fallback; use local repo
      return Optional.of(artifact.getFile().toPath());
    }
    return Optional.empty();
  }

}
