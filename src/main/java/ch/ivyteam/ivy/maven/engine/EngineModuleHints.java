package ch.ivyteam.ivy.maven.engine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.exec.CommandLine;
import org.apache.maven.plugin.logging.Log;

public class EngineModuleHints {

  public static void loadFromJvmOptionsFile(EngineMojoContext context, CommandLine cli) {
    loadJvmOptions(context.engineDirectory, context.log).stream().forEach(option -> cli.addArgument(option));
  }

  public static String loadFromJvmOptionsFile(Path identifyAndGetEngineDirectory, Log log) {
    return loadJvmOptions(identifyAndGetEngineDirectory, log).stream().collect(Collectors.joining(" ", " ", " "));
  }

  private static List<String> loadJvmOptions(Path engineDir, Log log) {
    var jvmOptionsFile = engineDir.resolve("bin").resolve("jvm-module.options");
    if (!Files.exists(jvmOptionsFile)) {
      log.warn("Couldn't load jvm module options from '" + jvmOptionsFile + "' file.");
      return List.of();
    }

    try {
      return Files.readAllLines(jvmOptionsFile).stream()
              .filter(line -> !line.isBlank())
              .filter(line -> !line.matches("^\\s*#.*$"))
              .collect(Collectors.toList());
    } catch (IOException ex) {
      log.warn("Couldn't read the jvm module options from '" + jvmOptionsFile + "' file.", ex);
      return List.of();
    }
  }
}
