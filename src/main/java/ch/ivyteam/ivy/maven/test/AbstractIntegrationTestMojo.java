package ch.ivyteam.ivy.maven.test;

import java.nio.file.Path;
import java.util.Objects;

import org.apache.commons.lang3.Strings;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import ch.ivyteam.ivy.maven.AbstractEngineMojo;

public abstract class AbstractIntegrationTestMojo extends AbstractEngineMojo {
  /**
   * Configure if the test engine gets copied to the maven target folder. With
   * this you can start each test cycle with a clean engine. Integration tests
   * may leave resources like deployed projects behind which may lead to
   * unwanted side effects on the next test cycle.
   * <ul>
   * <li><code>COPY_FROM_CACHE</code> = copy the engine if it comes from the
   * {@link #engineCacheDirectory}.</li>
   * <li><code>MODIFY_EXISTING</code> = don't copy the engine, this could lead
   * to unforeseen behaviour if the same engine is used multiple times.</li>
   * <li><code>COPY_FROM_TEMPLATE</code> = always copy the engine. If you have a
   * preconfigured engine in the {@link #engineDirectory} it will be copied as
   * well.
   * <ul>
   * <li><b style="color:yellow">Note</b>: that we advise you to move the
   * configuration of such engine to the build cycle itself instead of using a
   * preconfigured one.</li></li>
   * </ul>
   * </ul>
   * @since 8.0.4
   */
  @Parameter(property = "ivy.test.engine", defaultValue = TestEngineLocation.COPY_FROM_CACHE)
  String testEngine;

  public final Path getEngineDir(MavenProject project) throws MojoExecutionException {
    if (engineToTarget()) {
      return getTargetDir(project);
    }
    return findMatchingEngineInCacheDirectory();
  }

  protected final boolean engineToTarget() throws MojoExecutionException {
    return isLocation(TestEngineLocation.COPY_FROM_TEMPLATE) ||
        isLocation(TestEngineLocation.COPY_FROM_CACHE) && isCachedEngine();
  }

  private boolean isLocation(String location) {
    return Strings.CI.equals(testEngine, location);
  }

  private boolean isCachedEngine() throws MojoExecutionException {
    var engineDir = identifyAndGetEngineDirectory();
    if (engineDir == null) {
      return false;
    }
    return Objects.equals(engineDir.getParent(), engineCacheDirectory);
  }

  Path getTargetDir(MavenProject project) {
    return Path.of(project.getBuild().getDirectory()).resolve("ivyEngine");
  }

  interface TestEngineLocation {
    String COPY_FROM_CACHE = "COPY_FROM_CACHE";
    String COPY_FROM_TEMPLATE = "COPY_FROM_TEMPLATE";
    String MODIFY_EXISTING = "MODIFY_EXISTING";
  }
}
