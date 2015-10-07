package ch.ivyteam.ivy.maven;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.project.MavenProject;

/**
 * Simple rule that can provide a real set-up MOJO that works on a copy of the given projectDirectory.
 * This simplifies TEST dramatically whenever your MOJO relies on real Maven Models like (Project, Artifact, ...)
 * 
 * @author Reguel Wermelinger
 * @since 03.10.2014
 * @param <T>
 */
public class ProjectMojoRule<T extends Mojo> extends MojoRule
{
  private File projectDir;
  private T mojo;
  private String mojoName;
  private File templateProjectDir;
  
  public ProjectMojoRule(File srcDir, String mojoName)
  {
    this.templateProjectDir = srcDir;
    this.mojoName = mojoName;
  }
  
  @Override
  @SuppressWarnings("unchecked")
  protected void before() throws Throwable 
  {
    projectDir = Files.createTempDirectory("MyBaseProject").toFile();
    FileUtils.copyDirectory(templateProjectDir, projectDir);
    MavenProject project = readMavenProject(projectDir);
    mojo = (T) lookupConfiguredMojo(project, mojoName);
  }
  
  @Override
  protected void after() 
  {
    try
    {
      FileUtils.deleteDirectory(projectDir);
    }
    catch (IOException ex)
    {
      throw new RuntimeException(ex);
    }  
  }

  public T getMojo()
  {
    return mojo;
  }
}