package ch.ivyteam.ivy.maven.generate;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import ch.ivyteam.ivy.maven.compile.AbstractEngineInstanceMojo;
import ch.ivyteam.ivy.maven.engine.MavenProjectBuilderProxy;

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
    projectBuilder.generateSources(project.getBasedir(), "WebServiceProcessSourceGenerator");
  }
}
