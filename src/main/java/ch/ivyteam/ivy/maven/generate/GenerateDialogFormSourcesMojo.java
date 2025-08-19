package ch.ivyteam.ivy.maven.generate;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import ch.ivyteam.ivy.maven.compile.AbstractEngineInstanceMojo;
import ch.ivyteam.ivy.maven.engine.MavenProjectBuilderProxy;

@Mojo(name = GenerateDialogFormSourcesMojo.GOAL)
public class GenerateDialogFormSourcesMojo extends AbstractEngineInstanceMojo {
  public static final String GOAL = "generate-dialog-form-sources";

  /**
   * Set to <code>true</code> to bypass the generation of <b>ivy dialog form sources</b>.
   * @since 13.2.0
   */
  @Parameter(property = "ivy.generate.dialog.form.sources.skip", defaultValue = "false")
  boolean skipGenerateSources;

  @Override
  protected void engineExec(MavenProjectBuilderProxy projectBuilder) throws Exception {
    if (skipGenerateSources) {
      return;
    }
    getLog().info("Generating Ivy dialog form sources...");
    projectBuilder.generateSources(project.getBasedir(), "DialogFormSourceGenerator");
  }
}
