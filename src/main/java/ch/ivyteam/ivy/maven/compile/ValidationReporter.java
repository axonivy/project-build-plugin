package ch.ivyteam.ivy.maven.compile;

import java.net.URI;
import java.util.EnumMap;
import java.util.Map;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import ch.ivyteam.ivy.project.validation.ProjectValidatorResult.Message;
import ch.ivyteam.ivy.project.validation.ProjectValidatorResult.Severity;

class ValidationReporter {

  private static final String RULE = "-".repeat(72);

  private final Log log;
  private final URI basedir;
  private final Map<Severity, Integer> counts = new EnumMap<>(Severity.class);

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
  }

  void logSummary(String projectName, long durationMs) {
    log.info(RULE);
    log.info("Project validation summary: " + projectName);
    log.info(RULE);
    log.info("  Errors:    " + count(Severity.ERROR));
    log.info("  Warnings:  " + count(Severity.WARN));
    log.info("  Info:      " + count(Severity.INFO));
    log.info("  Total:     " + total());
    log.info("  Duration:  " + durationMs + " ms");
    log.info(RULE);
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
