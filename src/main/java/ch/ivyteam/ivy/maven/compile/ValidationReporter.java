package ch.ivyteam.ivy.maven.compile;

import java.net.URI;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import ch.ivyteam.ivy.project.validation.ProjectValidatorResult.Message;
import ch.ivyteam.ivy.project.validation.ProjectValidatorResult.Severity;

class ValidationReporter {

  private static final String RULE = "-".repeat(72);

  private final Log log;
  private final URI basedir;
  private final Map<Severity, Integer> counts = new EnumMap<>(Severity.class);
  private final Set<URI> filesWithMessages = new HashSet<>();

  ValidationReporter(Log log, MavenProject project) {
    this.log = log;
    this.basedir = project.getBasedir().toURI();
  }

  void report(Message message) {
    var line = format(message);
    switch (message.severity()) {
      case ERROR -> log.error(line);
      case WARN -> log.warn(line);
      case INFO -> log.info(line);
    }
    counts.merge(message.severity(), 1, Integer::sum);
    if (message.file() != null) {
      filesWithMessages.add(message.file());
    }
  }

  void logSummary(String projectName, long durationMs, List<String> skippedValidators) {
    log.info("Project validation finished with " + findingsSummary());
    var logger = summaryLogger();
    logger.accept(RULE);
    logger.accept("Project validation summary: " + projectName);
    logger.accept(RULE);
    logger.accept("  Errors:    " + count(Severity.ERROR));
    logger.accept("  Warnings:  " + count(Severity.WARN));
    logger.accept("  Info:      " + count(Severity.INFO));
    logger.accept("  Total:     " + total());
    logger.accept("  Duration:  " + durationMs + " ms");
    logSkipped(logger, skippedValidators);
    logger.accept(RULE);
  }

  private String findingsSummary() {
    if (total() == 0) {
      return "no issues";
    }
    var fileCount = filesWithMessages.size();
    return fileCount + " file" + (fileCount > 1 ? "s" : "") + " with findings";
  }

  private void logSkipped(Consumer<CharSequence> logger, List<String> skippedValidators) {
    if (skippedValidators == null || skippedValidators.isEmpty()) {
      return;
    }
    logger.accept("  Skipped validators (" + skippedValidators.size() + "):");
    for (var id : skippedValidators) {
      logger.accept("    - " + id);
    }
  }

  private Consumer<CharSequence> summaryLogger() {
    if (count(Severity.ERROR) > 0) {
      return log::error;
    }
    if (count(Severity.WARN) > 0) {
      return log::warn;
    }
    return log::info;
  }

  boolean hasErrors() {
    return count(Severity.ERROR) > 0;
  }

  private int count(Severity severity) {
    return counts.getOrDefault(severity, 0);
  }

  private int total() {
    return counts.values().stream().mapToInt(Integer::intValue).sum();
  }

  private String format(Message message) {
    var sb = new StringBuilder();
    sb.append(relativize(message.file()));

    var path = message.location() == null ? null : message.location().path();
    if (path != null && !path.parts().isEmpty()) {
      sb.append(" ").append("[").append(path.parts().getFirst()).append(']');
    }
    sb.append(':').append(' ').append(message.text());
    return sb.toString();
  }

  private String relativize(URI file) {
    if (file == null) {
      return "<unknown file>";
    }
    return basedir.relativize(file).getPath();
  }

}
