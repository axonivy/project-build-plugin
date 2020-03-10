package ch.ivyteam.ivy.maven;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

public abstract class AbstractIntegrationTestMojo extends AbstractEngineMojo
{
  /**
   * When set to <code>true</code> the defined engine in {@link #engineDirectory} is copied to the target folder.
   * Enable this when you want to start each integration test cycle with a clean engine.
   */
  @Parameter(property = "ivy.engine.to.target", defaultValue="false")
  boolean engineToTarget;

  public File getEngineDir(MavenProject project) throws MojoExecutionException
  {
    if (engineToTarget && project != null)
    {
      return new File(project.getBuild().getDirectory(), "ivyEngine");
    }
    return findMatchingEngineInCacheDirectory();
  }
}
