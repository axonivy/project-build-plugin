package ch.ivyteam.ivy.maven.engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ch.ivyteam.ivy.maven.engine.EngineClassLoaderFactory.OsgiDir;

class TestEngineVersionEvaluator {

  @TempDir
  Path tempDir;

  @Test
  void isOSGiEngine_invalid() {
    assertThat(EngineVersionEvaluator.isOSGiEngine(tempDir)).isFalse();
  }

  @Test
  void isOSGiEngine_valid() throws IOException {
    var systemDir = tempDir.resolve(OsgiDir.INSTALL_AREA);
    Files.createDirectories(systemDir);
    assertThat(EngineVersionEvaluator.isOSGiEngine(tempDir)).isTrue();
  }
}
