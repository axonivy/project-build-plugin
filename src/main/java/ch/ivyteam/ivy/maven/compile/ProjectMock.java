package ch.ivyteam.ivy.maven.compile;

import java.nio.file.Path;
import java.util.stream.Stream;

import ch.ivyteam.ivy.project.model.Project;
import ch.ivyteam.ivy.project.model.ProjectDependencies;
import ch.ivyteam.ivy.project.model.ProjectFileSystem;

public class ProjectMock implements Project {

  private final String name;
  private final ProjectFileSystem fs;

  public ProjectMock(String name, Path projectRootPath) {
    this.name = name;
    this.fs = ProjectFileSystemMock.of(projectRootPath);
  }

  @Override
  public String id() {
    return "testSecurityContext$testApp$1$" + name();
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public ProjectFileSystem fs() {
    return fs;
  }

  @Override
  public ProjectDependencies debs() {
    return new ProjectDependencies(){

      @Override
      public Stream<Project> allRequired() {
        return Stream.of();
      }

      @Override
      public Stream<Project> allRelated() {
        return Stream.of();
      }

      @Override
      public Stream<Project> allDependent() {
        return Stream.of();
      }
    };
  }
}
