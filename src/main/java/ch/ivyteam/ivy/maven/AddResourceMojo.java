package ch.ivyteam.ivy.maven;

import java.io.File;
import java.util.Arrays;
import java.util.stream.Stream;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;

/**
 * Dynamically adds resource directories to the Maven build.
 * The directories are resolved using ANT-style glob patterns relative to the project base directory.
 * By default, all direct subdirectories of <code>src_generated/rest/</code> and
 * <code>src_generated/ws/</code> are added as resource directories.
 *
 * @since 14.0.0
 */
@Mojo(name = AddResourceMojo.GOAL, defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true)
public class AddResourceMojo extends AbstractMojo {

  public static final String GOAL = "ivy-add-resource";

  @Parameter(property = "project", required = true, readonly = true)
  MavenProject project;

  /**
   * Set to <code>true</code> to skip this mojo entirely.
   */
  @Parameter(property = "ivy.add.resource.skip", defaultValue = "false")
  boolean skipIvyAddResource;

  /**
   * ANT-style glob patterns used to scan for resource directories to add.
   * Patterns are resolved relative to the project base directory.
   * Each matched directory is registered as a Maven resource directory.
   */
  @Parameter(defaultValue = "src_generated/rest/*,src_generated/ws/*")
  String[] ivyResourcesToAdd;

  /**
   * File patterns to exclude from each added resource directory.
   * Applied to all directories matched by {@link #ivyResourcesToAdd}.
   */
  @Parameter(defaultValue = "**/*.java")
  String[] ivyResourceExcludes;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (skipIvyAddResource) {
      return;
    }
    resourceDirectoriesToAdd()
        .forEach(this::addResource);
  }

  private void addResource(String dir) {
    var resourceDir = new File(project.getBasedir(), dir);
    var resource = new Resource();
    resource.setDirectory(resourceDir.getAbsolutePath());
    resource.setExcludes(Arrays.asList(ivyResourceExcludes));
    getLog().info("Adding resource directory: " + resource.getDirectory());
    project.addResource(resource);
  }

  private Stream<String> resourceDirectoriesToAdd() {
    var scanner = new DirectoryScanner();
    scanner.setBasedir(project.getBasedir());
    scanner.setIncludes(ivyResourcesToAdd);
    scanner.scan();
    return Stream.of(scanner.getIncludedDirectories());
  }
}
