package ch.ivyteam.ivy.maven.generate;

import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;

/**
 * Unpacks libraries into the output directory.
 * By default, the following JARs are unpacked:
 * <code>lib/generated/rest/*.jar<code> and <code>lib_ws/client/*.jar<code>.
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
      "lib/generated/rest/*.jar",
      "lib_ws/client/*.jar"
  };

  /**
   * Set to <code>true</code> to bypass the unpack step.
   */
  @Parameter(property = "ivy.unpack.libs.skip", defaultValue = "false")
  boolean skipUnpackLibs;

  /**
   * Define exclusions for unpacking with ANT-style.
   *
   *
   * Sample:
   * <pre>
   * <code>&lt;unpackExcludes&gt;
   * &nbsp;&nbsp;&lt;unpackExclude&gt;lib/generated/rest/myClient.jar&lt;/unpackExclude&gt;
   * &lt;/unpackExcludes&gt;</code>
   * </pre>
   */
  @Parameter
  String[] unpackExcludes;

  /**
   * Define inclusions for unpacking with ANT-style.
   *
   *
   * Sample:
   * <pre>
   * <code>&lt;unpackIncludes&gt;
   * &nbsp;&nbsp;&lt;unpackInclude&gt;lib/.*jar&lt;/unpackInclude&gt;
   * &lt;/unpackIncludes&gt;</code>
   * </pre>
   */
  @Parameter
  String[] unpackIncludes;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (skipUnpackLibs) {
      getLog().info("Skipping unpacking libs.");
      return;
    }
    var scanner = new DirectoryScanner();
    scanner.setBasedir(project.getBasedir());
    scanner.setIncludes(ArrayUtils.addAll(INCLUDED_LIBS, unpackIncludes));
    scanner.setExcludes(unpackExcludes);
    scanner.scan();
    var projectDir = project.getBasedir().toPath();
    var outDir = projectDir.resolve(project.getBuild().getOutputDirectory());
    var libs = Arrays.stream(scanner.getIncludedFiles())
        .map(projectDir::resolve)
        .toList();
    try {
      new JarUnpacker(outDir, getLog()).unpack(libs);
    } catch (IOException ex) {
      throw new MojoExecutionException("Failed to unpacks libs", ex);
    }
  }
}
