package ch.ivyteam.ivy.maven.compile;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.Test;

import ch.ivyteam.ivy.project.validation.ProjectValidatorResult.Message;

class TestValidationMessageFormatter {

  private static final String TEXT = "Invalid value";
  private static final URI BASEDIR = URI.create("file:///project/");

  @Test
  void processPidAndNestedPathAreFormatted() {
    var file = file("process/Test.p.json");
    var path = List.of("1478B7E447CD01B2-S10-f17", "query", "offset");
    var message = message(file, path);
    assertThat(ValidationMessageFormatter.format(message, BASEDIR))
        .isEqualTo("process/Test.p.json:[f17.query.offset] " + TEXT);
  }

  @Test
  void processPidWithoutNestedPathIsFormatted() {
    var message = message(file("process/Test.p.json"), List.of("1478B7E447CD01B2-f0"));
    assertThat(ValidationMessageFormatter.format(message, BASEDIR))
        .isEqualTo("process/Test.p.json:[f0] " + TEXT);
  }

  @Test
  void processPathWithoutPidKeepsAllPathParts() {
    var message = message(file("process/Test.p.json"), List.of("data", "customFields", "name"));
    assertThat(ValidationMessageFormatter.format(message, BASEDIR))
        .isEqualTo("process/Test.p.json:[data.customFields.name] " + TEXT);
  }

  @Test
  void nonProcessPathKeepsOnlyFirstPathPart() {
    var message = message(file("dialog/Test.f.json"), List.of("button3", "action"));
    assertThat(ValidationMessageFormatter.format(message, BASEDIR))
        .isEqualTo("dialog/Test.f.json:[button3] " + TEXT);
  }

  private URI file(String relativePath) {
    return BASEDIR.resolve(relativePath);
  }

  private Message message(URI file, List<String> path) {
    return Message.error(path.toArray(String[]::new))
        .file(file)
        .text(TEXT);
  }
}