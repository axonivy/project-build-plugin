package ch.ivyteam.ivy.maven.generate;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import ch.ivyteam.ivy.maven.compile.AbstractProjectCompileMojo;
import ch.ivyteam.ivy.maven.engine.MavenProjectBuilderProxy;

@Mojo(name = GenerateProjectSourcesMojo.GOAL)
public class GenerateProjectSourcesMojo extends AbstractProjectCompileMojo {
  public static final String GOAL = "generate-sources";

  /**
   * Set to <code>true</code> to bypass the generation of <b>ivy data
   * classes</b>+<b>webservice processes</b>.
   * @since 13.2.0
   */
  @Parameter(property = "ivy.generate.sources.skip", defaultValue = "false")
  boolean skipGenerateSources;

  @Override
  public void engineExec(MavenProjectBuilderProxy projectBuilder) throws Exception {
    if (skipGenerateSources) {
      return;
    }
    getLog().info("Generating ivy Project sources...");
    projectBuilder.generateSources(project.getBasedir());
  }
}
