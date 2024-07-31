package ch.ivyteam.ivy.maven.engine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.plugin.logging.Log;

public class EngineModuleHints {

  private final Path engineDir;
  private final Log log;

  public EngineModuleHints(Path engineDir, Log log) {
    this.engineDir = engineDir;
    this.log = log;
  }

  public String asString() {
    return asStream().collect(Collectors.joining(" ", " ", " "));
  }

  public Stream<String> asStream() {
    var jvmOptionsFile = engineDir.resolve("bin").resolve("jvm-module.options");
    if (!Files.exists(jvmOptionsFile)) {
      log.warn("Couldn't load jvm module options from '" + jvmOptionsFile + "' file.");
      return Stream.empty();
    }

    try {
      return Files.readAllLines(jvmOptionsFile).stream()
              .filter(line -> !line.isBlank())
              .filter(line -> !line.matches("^\\s*#.*$"));
    } catch (IOException ex) {
      log.warn("Couldn't read the jvm module options from '" + jvmOptionsFile + "' file.", ex);
      return Stream.empty();
    }
  }
}
