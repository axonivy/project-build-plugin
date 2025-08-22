package ch.ivyteam.ivy.maven.generate;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import ch.ivyteam.ivy.maven.compile.AbstractEngineInstanceMojo;
import ch.ivyteam.ivy.maven.engine.MavenProjectBuilderProxy;

/**
 * <p>
 * Generates Web Services Process source files.
 * </p>
 *
 * <p>
 * Command line invocation is supported.
 * </p>
 *
 * <pre>
 * mvn com.axonivy.ivy.ci:project-build-plugin:generate-web-service-sources
 * </pre>
 *
 * <p>
 * Source generation can be skipped using:
 * </p>
 *
 * <pre>
 * mvn com.axonivy.ivy.ci:project-build-plugin:generate-web-service-sources
 * -Divy.generate.web.service.sources.skip=true
 * </pre>
 *
 * @since 13.2.0
 */
@Mojo(name = GenerateWebServiceSourcesMojo.GOAL)
public class GenerateWebServiceSourcesMojo extends AbstractEngineInstanceMojo {
  public static final String GOAL = "generate-web-service-sources";

  /**
   * Set to <code>true</code> to bypass the generation of <b>ivy web service sources</b>.
   * @since 13.2.0
   */
  @Parameter(property = "ivy.generate.web.service.sources.skip", defaultValue = "false")
  boolean skipGenerateSources;

  @Override
  protected void engineExec(MavenProjectBuilderProxy projectBuilder) throws Exception {
    if (skipGenerateSources) {
      return;
    }
    getLog().info("Generating Ivy web service process sources...");
    projectBuilder.generateSources(project.getBasedir().toPath(), "WebServiceProcessSourceGenerator");
  }
}
