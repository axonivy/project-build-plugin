package ch.ivyteam.ivy.maven.extension;

import java.nio.file.Path;

import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;

public class LocalRepoTest {

  public static MavenArtifactRepository repo() {
    var layout = new DefaultRepositoryLayout();
    String string = path().toUri().toString();
    return new MavenArtifactRepository("user", string, layout, null, null);
  }

  public static Path path() {
    String repoOverride = System.getProperty("maven.repo.local");
    if (repoOverride != null) {
      return Path.of(repoOverride);
    }
    var home = SystemUtils.getUserHomePath();
    return home.resolve(".m2").resolve("repository");
  }

}
