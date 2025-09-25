package ch.ivyteam.ivy.maven.generate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.common.base.Objects;

import ch.ivyteam.util.io.generate.SourceWriter;

public class NioSourceWriter implements SourceWriter {

  private final Path projectDir;

  NioSourceWriter(Path projectDir) {
    this.projectDir = projectDir;
  }

  @Override
  public void write(String projectRelativePath, String content) {
    var srcFile = projectDir.resolve(projectRelativePath);
    try {
      if (!Files.exists(srcFile) || !Objects.equal(Files.readString(srcFile), content)) {
        Files.createDirectories(srcFile.getParent());
        Files.writeString(srcFile, content);
      }
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public boolean exists(String projectRelativePath) {
    return Files.exists(projectDir.resolve(projectRelativePath));
  }

  @Override
  public void delete(String projectRelativePath) {
    throw new UnsupportedOperationException();
  }
}
