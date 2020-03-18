package ch.ivyteam.ivy.maven.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.maven.project.MavenProject;

public class MavenRuntime
{
  public static List<File> getDependencies(MavenProject project, String type)
  {
    Set<org.apache.maven.artifact.Artifact> dependencies = project.getArtifacts();
    if (dependencies == null)
    {
      return Collections.emptyList();
    }
    
    List<File> deps = new ArrayList<>();
    for(org.apache.maven.artifact.Artifact artifact : dependencies)
    {
      if (artifact.getType().equals(type))
      {
        deps.add(artifact.getFile());
      }
    }
    return deps;
  }
}
