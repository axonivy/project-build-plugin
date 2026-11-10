package ch.ivyteam.ivy.maven.report;

import static ch.ivyteam.ivy.IvyConstants.PROCESS_EXTENSION;
import static ch.ivyteam.ivy.ProjectDir.PROCESS;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.codehaus.plexus.util.DirectoryScanner;

import ch.ivyteam.ivy.process.io.JsonProc;
import ch.ivyteam.ivy.process.model.Process;
import ch.ivyteam.ivy.process.model.simpl.ProcessS;
import ch.ivyteam.ivy.process.model.value.ProcessName;

class ProcessReader {

  private static final String[] INCLUDED_PROCESS_FILES = {PROCESS + "/**/*." + PROCESS_EXTENSION};

  private final Path projectDir;

  public ProcessReader(Path path) {
    this.projectDir = path;
  }

  public List<Process> processes() {
    List<Process> processes = new ArrayList<>();
    for (var processFile : processFiles()) {
      Path processPath = projectDir.resolve(processFile);
      try (var input = Files.newInputStream(processPath)) {
        var processName = nameOf(processPath);
        var process = readProcess(input, processName);
        processes.add(process);
      } catch (IOException ex) {
        throw new UncheckedIOException("Failed to read process " + processPath, ex);
      }
    }
    return processes;
  }

  static Process readProcess(InputStream input, ProcessName processName) {
    var process = JsonProc.read(processName.getName(), input);
    if (process instanceof ProcessS internal) {
      internal.setProcessName(processName);
    }
    return process;
  }

  ProcessName nameOf(Path processPath) {
    var relative = projectDir.resolve(PROCESS).relativize(processPath);
    String processName = processPath.getFileName().toString();
    processName = processName.substring(0, processName.length() - PROCESS_EXTENSION.length() - 1);
    String pathRef = Optional.ofNullable(relative.getParent()).map(p -> p.toString().replace('\\', '/')).orElse("");
    return new ProcessName(processName, pathRef);
  }

  private String[] processFiles() {
    var scanner = new DirectoryScanner();
    scanner.setBasedir(projectDir.toFile());
    scanner.setIncludes(INCLUDED_PROCESS_FILES);
    scanner.scan();
    return scanner.getIncludedFiles();
  }
}
