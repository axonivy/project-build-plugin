package ch.ivyteam.ivy.maven.compile;

import java.nio.file.Path;

import ch.ivyteam.ivy.project.model.ProjectFileSystem;
import ch.ivyteam.util.io.resource.FileSystem;
import ch.ivyteam.util.io.resource.RootFolder;
import ch.ivyteam.util.io.resource.nio.NioFileSystemProvider;

public class ProjectFileSystemMock implements ProjectFileSystem {

  private final Path rootPath;
  private final FileSystem fs;

  ProjectFileSystemMock(Path rootPath) {
    this.rootPath = rootPath;
    this.fs = NioFileSystemProvider.create(rootPath);
  }

  public static ProjectFileSystemMock of(Path projectRootPath) {
    return new ProjectFileSystemMock(projectRootPath);
  }

  @Override
  public RootFolder root() {
    return fs.root();
  }

  @Override
  public boolean isIar() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void close() {
    fs.close();
  }

  @Override
  public Path rootPath() {
    return rootPath;
  }

}
