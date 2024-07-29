package ch.ivyteam.ivy.maven.util;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import org.apache.commons.lang3.StringUtils;

public interface PathUtils {

  public static String toExtension(Path path) {
    return toExtension(path.getFileName().toString());
  }

  public static String toExtension(String path) {
    return StringUtils.substringAfterLast(path, ".");
  }

  public static void delete(Path path) {
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

  public static void clean(Path path) {
    delete(path);
    try {
      Files.createDirectories(path);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }
}