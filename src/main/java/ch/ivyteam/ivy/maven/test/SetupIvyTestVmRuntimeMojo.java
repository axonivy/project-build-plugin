package ch.ivyteam.ivy.maven.test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import ch.ivyteam.ivy.maven.test.bpm.IvyTestRuntime;
import ch.ivyteam.ivy.maven.util.MavenDependencies;
import ch.ivyteam.ivy.maven.util.MavenProperties;

/**
 * Provides a property file containing project-related information such as dependent projects.
 * Tries to auto-configure 'maven-surefire-plugin' so that the test VM can access the provided information.
 *
 * @since 13.2.0
 */
@Mojo(name = SetupIvyTestVmRuntimeMojo.GOAL, requiresDependencyResolution = ResolutionScope.TEST)
public class SetupIvyTestVmRuntimeMojo extends AbstractMojo {
  public static final String GOAL = "ivy-test-vm-runtime";
  public static final String IVY_TEST_VM_RUNTIME = "ivy.test.vm.runtime";
  public static final String MAVEN_TEST_ADDITIONAL_CLASSPATH = "maven.test.additionalClasspath";

  @Parameter(property = "project", required = true, readonly = true)
  MavenProject project;

  @Parameter(defaultValue = "${session}", readonly = true)
  private MavenSession session;

  /**
   * Set to <code>true</code> to bypass property set-up.
   * @since 6.1.0
   */
  @Parameter(defaultValue = "false", property = "maven.test.skip")
  boolean skipTest;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (skipTest) {
      return;
    }
    var properties = new MavenProperties(project, getLog());
    var tstVmDir = createTestVmRuntime();
    properties.setMavenProperty(IVY_TEST_VM_RUNTIME, tstVmDir.toAbsolutePath().toString());
    properties.setMavenProperty(MAVEN_TEST_ADDITIONAL_CLASSPATH, "${" + IVY_TEST_VM_RUNTIME + "}");
  }

  private Path createTestVmRuntime() throws MojoExecutionException {
    var testRuntime = new IvyTestRuntime();
    testRuntime.setProjectLocations(projectLocations());
    var productDir = Path.of(project.getBuild().getDirectory()).resolve("productDir");
    var webappsIvyDir = productDir.resolve("webapps/ivy");
    try {
      Files.createDirectories(webappsIvyDir);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
    testRuntime.setProductDir(productDir);
    try {
      return testRuntime.store(project);
    } catch (IOException ex) {
      throw new MojoExecutionException("Failed to write ivy test vm settings.", ex);
    }
  }

  private List<URI> projectLocations() {
    return Stream.concat(Stream.of(project.getBasedir().toPath()),
        MavenDependencies.of(project)
            .session(session)
            .typeFilter("iar")
            .all()
            .stream())
        .map(Path::toUri)
        .toList();
  }
}
