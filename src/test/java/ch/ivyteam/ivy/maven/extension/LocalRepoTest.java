package ch.ivyteam.ivy.maven.extension;

import java.nio.file.Path;

import org.apache.commons.lang3.SystemUtils;

public class LocalRepoTest {

  public static Path path() {
    String repoOverride = System.getProperty("maven.repo.local");
    if (repoOverride != null) {
      return Path.of(repoOverride);
    }
    var home = SystemUtils.getUserHomePath();
    return home.resolve(".m2").resolve("repository");
  }
}
