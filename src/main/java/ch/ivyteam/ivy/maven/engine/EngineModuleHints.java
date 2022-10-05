package ch.ivyteam.ivy.maven.engine;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.logging.Log;

public class EngineModuleHints {

  public static void loadFromJvmOptionsFile(EngineMojoContext context, CommandLine cli) {
    loadJvmOptions(context.engineDirectory, context.log).stream().forEach(option -> cli.addArgument(option));
  }

  public static String loadFromJvmOptionsFile(File identifyAndGetEngineDirectory, Log log) {
    return loadJvmOptions(identifyAndGetEngineDirectory, log).stream().collect(Collectors.joining(" ", " ", " "));
  }

  private static List<String> loadJvmOptions(File engineDir, Log log) {
    File jvmOptionsFile = new File(engineDir, "configuration/jvm-module.options");
    if (!jvmOptionsFile.exists()) {
      log.warn("Couldn't load jvm module options from '" + jvmOptionsFile + "' file.");
      return Collections.emptyList();
    }

    try {
      return FileUtils.readLines(jvmOptionsFile, StandardCharsets.UTF_8).stream()
              .filter(line -> !line.isBlank())
              .filter(line -> !line.matches("^\\s*#.*$"))
              .collect(Collectors.toList());
    } catch (IOException ex) {
      log.warn("Couldn't read the jvm module options from '" + jvmOptionsFile + "' file.", ex);
      return Collections.emptyList();
    }
  }
}