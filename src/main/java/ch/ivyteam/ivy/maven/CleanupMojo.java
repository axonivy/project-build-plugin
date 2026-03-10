package ch.ivyteam.ivy.maven;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;

import ch.ivyteam.ivy.maven.util.PathUtils;

/**
 * Deletes directories that contain generated sources.
 * By default, the following directories are removed:
 * <code>src_generated/dataclass</code>, <code>src_generated/wsprocess</code> and
 * <code>src_generated/repo</code>.
 *
 * @since 13.2.0
 */
@Mojo(name = CleanupMojo.GOAL)
public class CleanupMojo extends AbstractMojo {

  public static final String GOAL = "ivy-cleanup";

  @Parameter(property = "project", required = true, readonly = true)
  MavenProject project;

  public static final String[] INCLUDED_DIRECTORIES = {
      "src_generated/dataclass",
      "src_generated/wsprocess",
      "src_generated/repo"
  };

  /**
   * Set to <code>true</code> to bypass the deletion step.
   */
  @Parameter(property = "ivy.cleanup.skip", defaultValue = "false")
  boolean skipIvyDependencyCleanup;

  /**
   * Define directories to be excluded from cleanup with ANT-style exclusions.
   *
   *
   * Sample:
   * <pre>
   * <code>&lt;cleanupExcludes&gt;
   * &nbsp;&nbsp;&lt;cleanupExclude&gt;src_generated/dataclass&lt;/cleanupExclude&gt;
   * &nbsp;&nbsp;&lt;cleanupExclude&gt;src_generated/wsprocess&lt;/cleanupExclude&gt;
   * &lt;/cleanupExcludes&gt;</code>
   * </pre>
   */
  @Parameter
  String[] cleanupExcludes;

  /**
   * Define directories to be deleted during cleanup with ANT-style inclusions.
   *
   *
   * Sample:
   * <pre>
   * <code>&lt;cleanupIncludes&gt;
   * &nbsp;&nbsp;&lt;cleanupInclude&gt;custom/path&lt;/cleanupInclude&gt;
   * &lt;/cleanupIncludes&gt;</code>
   * </pre>
   */
  @Parameter
  String[] cleanupIncludes;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (skipIvyDependencyCleanup) {
      return;
    }
    var scanner = new DirectoryScanner();
    scanner.setBasedir(project.getBasedir());
    scanner.setIncludes(ArrayUtils.addAll(INCLUDED_DIRECTORIES, cleanupIncludes));
    scanner.setExcludes(cleanupExcludes);
    scanner.scan();
    var dirs = scanner.getIncludedDirectories();
    for (var dir : dirs) {
      var dirToDelete = project.getBasedir().toPath().resolve(dir);
      try {
        getLog().info("Deleting: " + dirToDelete);
        PathUtils.delete(dirToDelete);
      } catch (Exception ex) {
        getLog().warn("Couldn't delete: " + dirToDelete, ex);
      }
    }
  }
}
