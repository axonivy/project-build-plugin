package ch.ivyteam.ivy.maven;

import java.io.File;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

public abstract class AbstractIntegrationTestMojo extends AbstractEngineMojo
{
  /**
   * Configure if the cached engine should be copied into the target folder of the maven project.
   * This enables you to start each testing cycle with a clean engine. Integration tests may leave resources like deployed projects behind which may
   * lead to unwanted side effects on the next test cycle.
   * <ul>
   *   <li><code>TARGET</code> = always copy the engine into the maven project target folder, this provides you with a clean engine in each test cycle.</li>
   *   <li><code>CACHE</code> = if the engine comes from the {@link #engineCacheDirectory} copy it to the maven target folder.</li>
   *   <li><code>IGNORE</code> = don't copy the engine, this could lead to unforeseen behaviour if the engine is used multiple times.</li>
   * </ul>
   * @since 8.0.4
   */
  @Parameter(property = "ivy.test.engine.location", defaultValue=TestEngineLocation.CACHE)
  String testEngineLocation;

  protected final File getEngineDir(MavenProject project) throws MojoExecutionException
  {
    if (engineToTarget())
    {
      return new File(project.getBuild().getDirectory(), "ivyEngine");
    }
    return findMatchingEngineInCacheDirectory();
  }
  
  protected final boolean engineToTarget() throws MojoExecutionException
  {
    return
        isLocation(TestEngineLocation.TARGET) ||
        isLocation(TestEngineLocation.CACHE) && isCachedEngine();
  }
  
  private boolean isLocation(String location)
  {
    return StringUtils.equalsIgnoreCase(testEngineLocation, location);
  }
  
  private boolean isCachedEngine() throws MojoExecutionException
  {
    File engineDir = identifyAndGetEngineDirectory();
    if (engineDir == null)
    {
      return false;
    }
    return Objects.equals(engineDir.getParentFile(), engineCacheDirectory);
  }
  
  static interface TestEngineLocation
  {
    String TARGET = "TARGET";
    String CACHE = "CACHE";
    String IGNORE = "IGNORE";
  }
}
