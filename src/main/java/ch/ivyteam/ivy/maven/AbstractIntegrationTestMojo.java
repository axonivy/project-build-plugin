package ch.ivyteam.ivy.maven;

import java.io.File;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

public abstract class AbstractIntegrationTestMojo extends AbstractEngineMojo
{
  /**
   * Configure if the cached engine should be copied into the target folder of the maven project.
   * This enables you to start each testing cycle with a clean engine.
   * <ul>
   *   <li><code>TARGET</code> = copy the engine into the maven project target folder, this provides you with a clean engine in each test cycle.</li>
   *   <li><code>CACHE</code> = use the cached engine, could lead to unforeseen behaviour if executed multiple times.</li>
   *   <li><code>IGNORE</code> = if you willingly want to use the engine from {@link #engineDirectory} to execute your tests on. This will disable warnings of this feature.</li>
   * </ul>
   */
  @Parameter(property = "ivy.test.engine.location", defaultValue=TestEngineLocation.CACHE)
  String testEngineLocation;

  public File getEngineDir(MavenProject project) throws MojoExecutionException
  {
    if (isLocation(TestEngineLocation.TARGET))
    {
      return new File(project.getBuild().getDirectory(), "ivyEngine");
    }
    return findMatchingEngineInCacheDirectory();
  }
  
  public static interface TestEngineLocation
  {
    String TARGET = "TARGET";
    String CACHE = "CACHE";
    String IGNORE = "IGNORE";
  }
  
  public boolean isLocation(String location)
  {
    return StringUtils.equalsIgnoreCase(testEngineLocation, location);
  }
}
