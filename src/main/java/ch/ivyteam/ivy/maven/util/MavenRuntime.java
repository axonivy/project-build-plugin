package ch.ivyteam.ivy.maven.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

public class MavenRuntime
{
  public static List<File> getDependencies(MavenProject project, String type)
  {
    return getDependencies(project, null, type);
  }
  
  public static List<File> getDependencies(MavenProject project, MavenSession session, String type)
  {
    Set<Artifact> dependencies = project.getArtifacts();
    if (dependencies == null)
    {
      return Collections.emptyList();
    }
    
    List<File> deps = new ArrayList<>();
    for(Artifact artifact : dependencies)
    {
      MavenProject reactorProject = findReactorProject(session, artifact);
      if (reactorProject != null && reactorProject.getArtifact().getType().equals(type))
      {
        deps.add(reactorProject.getBasedir());
      }
      else if (artifact.getType().equals(type))
      {
        deps.add(artifact.getFile());
      }
    }
    return deps;
  }

  private static MavenProject findReactorProject(MavenSession session, Artifact artifact)
  {
    if (session == null)
    {
      return null;
    }
    String artifactKey = artifact.getGroupId()+":"+artifact.getArtifactId()+":"+artifact.getVersion();
    MavenProject reactorProject = session.getProjectMap().get(artifactKey);
    return reactorProject;
  }
}
