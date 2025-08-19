package ch.ivyteam.ivy.maven.generate;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import ch.ivyteam.ivy.maven.compile.AbstractEngineInstanceMojo;
import ch.ivyteam.ivy.maven.engine.MavenProjectBuilderProxy;

@Mojo(name = GenerateDataClassSourcesMojo.GOAL)
public class GenerateDataClassSourcesMojo extends AbstractEngineInstanceMojo {
  public static final String GOAL = "generate-data-class-sources";

  /**
   * Set to <code>true</code> to bypass the generation of <b>ivy data classes</b>.
   * @since 13.2.0
   */
  @Parameter(property = "ivy.generate.data.class.sources.skip", defaultValue = "false")
  boolean skipGenerateSources;

  @Override
  protected void engineExec(MavenProjectBuilderProxy projectBuilder) throws Exception {
    if (skipGenerateSources) {
      return;
    }
    getLog().info("Generating Ivy data class sources...");
    projectBuilder.generateSources(project.getBasedir(), "DataClassSourceGenerator");
  }
}
