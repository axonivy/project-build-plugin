package ch.ivyteam.ivy.maven.generate;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import ch.ivyteam.ivy.maven.compile.AbstractEngineInstanceMojo;
import ch.ivyteam.ivy.maven.engine.MavenProjectBuilderProxy;

/**
 * <p>
 * Generates Data Class Java source files.
 * </p>
 *
 * <p>
 * Command line invocation is supported.
 * </p>
 *
 * <pre>
 * mvn com.axonivy.ivy.ci:project-build-plugin:generate-data-class-sources
 * </pre>
 *
 * <p>
 * Source generation can be skipped using:
 * </p>
 *
 * <pre>
 * mvn com.axonivy.ivy.ci:project-build-plugin:generate-data-class-sources
 * -Divy.generate.data.class.sources.skip=true
 * </pre>
 *
 * @since 13.2.0
 */
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
