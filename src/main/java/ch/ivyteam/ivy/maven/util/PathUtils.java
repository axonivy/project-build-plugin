package ch.ivyteam.ivy.maven.util;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import org.apache.commons.lang3.StringUtils;

public interface PathUtils {

  static String toExtension(Path path) {
    return toExtension(path.getFileName().toString());
  }

  static String toExtension(String path) {
    return StringUtils.substringAfterLast(path, ".");
  }

  static void delete(Path path) {
    if (path == null) {
      return;
    }
    if (!Files.exists(path)) {
      return;
    }

    try (var stream = Files.walk(path)) {
      stream.sorted(Comparator.reverseOrder())
          .map(Path::toFile)
          .forEach(File::delete);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  static void clean(Path path) {
    delete(path);
    try {
      Files.createDirectories(path);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }
}
