package ch.ivyteam.ivy.maven.compile;

import java.net.URI;

import ch.ivyteam.ivy.IvyConstants;
import ch.ivyteam.ivy.process.model.value.PID;
import ch.ivyteam.ivy.project.validation.ProjectValidatorResult.Location;
import ch.ivyteam.ivy.project.validation.ProjectValidatorResult.Message;
import ch.ivyteam.ivy.project.validation.ProjectValidatorResult.PositionRange;

final class ValidationMessageFormatter {

  private ValidationMessageFormatter() {
  }

  static String format(Message message, URI basedir) {
    var sb = new StringBuilder();
    sb.append(relativize(message.file(), basedir));

    var location = message.location();
    var position = location == null ? null : location.position();

    if (position != null) {
      appendPosition(sb, position);
    } else {
      appendPath(sb, location, message.file());
    }

    sb.append(' ').append(message.text());
    return sb.toString();
  }

  private static void appendPosition(StringBuilder sb, PositionRange position) {
    var start = position.start();
    sb.append(":[")
        .append(start.line())
        .append(',')
        .append(start.column())
        .append(']');
  }

  private static void appendPath(StringBuilder sb, Location location, URI file) {
    if (location == null || location.path() == null || location.path().parts().isEmpty()) {
      return;
    }

    var parts = location.path().parts();
    var displayPath = parts.getFirst();
    var filePath = file == null ? null : file.getPath();
    if (filePath != null && filePath.endsWith(IvyConstants.PROCESS_EXTENSION)) {
      displayPath = shortenProcessPid(displayPath);
      var jsonPath = String.join(".", parts.subList(1, parts.size()));
      if (!jsonPath.isEmpty()) {
        displayPath = displayPath.isEmpty() ? jsonPath : displayPath + "." + jsonPath;
      }
    }
    if (!displayPath.isEmpty()) {
      sb.append(":[").append(displayPath).append(']');
    }
  }

  private static String shortenProcessPid(String pid) {
    var fieldId = PID.of(pid).getFieldId();
    return fieldId.isEmpty() ? pid : fieldId;
  }

  private static String relativize(URI file, URI basedir) {
    if (file == null) {
      return "<unknown file>";
    }
    return basedir.relativize(file).getPath();
  }
}