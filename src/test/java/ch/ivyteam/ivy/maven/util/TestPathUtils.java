package ch.ivyteam.ivy.maven.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TestPathUtils {

  @Test
  void toExtension_fromFilename() {
    var path = Paths.get("test.pdf");
    assertThat(PathUtils.toExtension(path)).isEqualTo("pdf");
  }

  @Test
  void toExtension_fromPath() {
    var path = Paths.get("/root/folder/test.pdf");
    assertThat(PathUtils.toExtension(path)).isEqualTo("pdf");
  }

  @Test
  void toExtension_missing() {
    var path = Paths.get("test");
    assertThat(PathUtils.toExtension(path)).isEmpty();
  }

  @Test
  void toExtension_string_fromFilename() {
    assertThat(PathUtils.toExtension("test.pdf")).isEqualTo("pdf");
  }

  @Test
  void toExtension_string_fromPath() {
    assertThat(PathUtils.toExtension("/root/folder/test.pdf")).isEqualTo("pdf");
  }

  @Test
  void toExtension_string_missing() {
    assertThat(PathUtils.toExtension("test")).isEmpty();
  }

  @Test
  void delete_null() {
    assertThatNoException().isThrownBy(() -> PathUtils.delete(null));
  }

  @Test
  void delete_nonExisting() {
    assertThatNoException().isThrownBy(() -> PathUtils.delete(Path.of("non-existing-directory")));
  }

  @Test
  void delete_emptyDir(@TempDir Path tempDir) {
    assertThat(tempDir).exists();

    PathUtils.delete(tempDir);
    assertThat(tempDir).doesNotExist();
  }

  @Test
  void delete_file(@TempDir Path tempDir) throws IOException {
    var file = tempDir.resolve("louis.txt");
    Files.writeString(file, "müller");
    assertThat(file).exists();

    PathUtils.delete(file);
    assertThat(file).doesNotExist();
  }

  @Test
  void delete_notEmptyFolder(@TempDir Path tempDir) throws IOException {
    var file = tempDir.resolve("louis.txt");
    Files.writeString(file, "müller");
    assertThat(file).exists();

    var subFolder = tempDir.resolve("subFolder");
    Files.createDirectory(subFolder);
    assertThat(subFolder).exists();

    var subFile = subFolder.resolve("chrischel.txt");
    Files.writeString(subFile, "strebschel");
    assertThat(subFile).exists();

    PathUtils.delete(tempDir);
    assertThat(tempDir).doesNotExist();
  }
}
