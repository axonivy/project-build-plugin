package ch.ivyteam.ivy.maven.engine;

import java.io.File;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import ch.ivyteam.ivy.maven.util.SharedFile;

public class EngineMojoContext
{
  public final File engineDirectory;
  public final MavenProject project;
  public final Log log;
  public final EngineVmOptions vmOptions;
  public final String engineClasspathJar;
  public final MavenProperties properties;
  public final File engineLogFile;

  public EngineMojoContext(File engineDirectory, MavenProject project, Log log, File engineLogFile, EngineVmOptions vmOptions)
  {
    this.engineDirectory = engineDirectory;
    this.project = project;
    this.log = log;
    this.engineLogFile = engineLogFile;
    this.vmOptions = vmOptions;

    this.engineClasspathJar = new SharedFile(project).getEngineClasspathJar().getAbsolutePath();
    this.properties = new MavenProperties(project, log);

    if (!(new File(engineClasspathJar).exists()))
    {
      throw new RuntimeException("Engine ClasspathJar " + engineClasspathJar + " does not exist.");
    }
    if (!(engineDirectory.exists()))
    {
      throw new RuntimeException("Engine Directory " + engineDirectory + " does not exist.");
    }
  }
}