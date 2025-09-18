package ch.ivyteam.ivy.maven.generate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BiFunction;

import ch.ivyteam.ivy.builder.SourceWriter;

public class DataClassSourceWriter implements SourceWriter {

  private final Path projectDir;

  DataClassSourceWriter(Path projectDir) {
    this.projectDir = projectDir;
  }

  @Override
  public BiFunction<Path, String, Path> writer() {
    return (path, content) -> {
      var dataClass = projectDir.resolve(path);
      try {
        Files.createDirectories(dataClass.getParent());
        Files.writeString(dataClass, content);
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
      return dataClass;
    };
  }

  @Override
  public String read(Path projectRelativePath) {
    try {
      return Files.readString(projectDir.resolve(projectRelativePath));
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public boolean exists(Path projectRelativePath) {
    return Files.exists(projectDir.resolve(projectRelativePath));
  }

}
