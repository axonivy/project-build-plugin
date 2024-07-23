package ch.ivyteam.ivy.maven.util;

import java.nio.file.Path;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

public class MavenRuntime {

  public static List<Path> getDependencies(MavenProject project, String type) {
    return getDependencies(project, null, type);
  }

  public static List<Path> getDependencies(MavenProject project, MavenSession session, String type) {
    return new MavenDependencies(project, session).type(type).all();
  }
}
