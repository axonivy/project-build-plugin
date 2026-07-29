package ch.ivyteam.ivy.maven;

import java.util.stream.Stream;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;

/**
 * Dynamically adds source directories to the Maven build.
 * All direct subdirectories of <code>src_generated/rest/</code> and
 * <code>src_generated/ws/</code> are added as source directories.
 *
 * @since 14.0.0
 */
@Mojo(name = AddSourceMojo.GOAL, defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
public class AddSourceMojo extends AbstractMojo {

  public static final String GOAL = "ivy-add-generated-source-roots";

  @Parameter(property = "project", required = true, readonly = true)
  MavenProject project;

  /**
   * Set to <code>true</code> to skip this mojo entirely.
   */
  @Parameter(property = "ivy.add.source.skip", defaultValue = "false")
  boolean skipIvyAddSource;

  private static final String[] INCLUDES = {"src_generated/rest/*", "src_generated/ws/*"};

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (skipIvyAddSource) {
      return;
    }
    sourceDirectoriesToAdd()
        .forEach(srcDir -> {
          getLog().info("Adding source directory: " + srcDir);
          project.addCompileSourceRoot(srcDir);
        });

  }

  private Stream<String> sourceDirectoriesToAdd() {
    var scanner = new DirectoryScanner();
    scanner.setBasedir(project.getBasedir());
    scanner.setIncludes(INCLUDES);
    scanner.scan();
    return Stream.of(scanner.getIncludedDirectories());
  }
}
