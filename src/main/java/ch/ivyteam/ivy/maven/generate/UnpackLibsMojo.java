package ch.ivyteam.ivy.maven.generate;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Unpack libs to the output directory.
 * By default lib/generated/rest/.*jar and lib_ws/client/*.jar are unpacked.
 *
 *
 * @since 13.2.0
 */
@Mojo(name = UnpackLibsMojo.GOAL)
public class UnpackLibsMojo extends AbstractMojo {

  public static final String GOAL = "unpack-libs";

  @Parameter(property = "project", required = true, readonly = true)
  MavenProject project;

  public static final String[] INCLUDED_LIBS = {
      "lib/generated/rest/.*jar",
      "lib_ws/client/*.jar"
  };

  /**
   * Set to <code>true</code> to bypass the deletion step.
   */
  @Parameter(property = "ivy.unpack.libs.skip", defaultValue = "false")
  boolean skipUnpackLibs;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (skipUnpackLibs) {
      getLog().info("Skipping unpacking libs.");
      return;
    }
    var projectDir = project.getBasedir().toPath();
    var outDir = project.getBuild().getOutputDirectory();

    // TODO Auto-generated method stub

  }

}
