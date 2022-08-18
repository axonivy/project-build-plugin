package ch.ivyteam.ivy.maven.engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;
import java.util.UUID;

import org.junit.Test;

import ch.ivyteam.ivy.maven.engine.EngineClassLoaderFactory.OsgiDir;

public class TestEngineVersionEvaluator {

  @Test
  public void isOSGiEngine_invalid() {
    File tempDir = createTempDir();
    assertThat(EngineVersionEvaluator.isOSGiEngine(tempDir)).isFalse();
  }

  @Test
  public void isOSGiEngine_valid() {
    File tempDir = createTempDir();
    File systemDir = new File(tempDir, OsgiDir.INSTALL_AREA);
    systemDir.mkdir();
    systemDir.deleteOnExit();
    assertThat(EngineVersionEvaluator.isOSGiEngine(tempDir)).isTrue();
  }

  private static File createTempDir() {
    try {
      File tmpDir = Files.createTempDirectory(UUID.randomUUID().toString()).toFile();
      tmpDir.deleteOnExit();
      return tmpDir;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
