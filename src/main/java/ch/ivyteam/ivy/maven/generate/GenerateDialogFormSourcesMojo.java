package ch.ivyteam.ivy.maven.generate;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import ch.ivyteam.ivy.maven.compile.AbstractEngineInstanceMojo;
import ch.ivyteam.ivy.maven.engine.MavenProjectBuilderProxy;

/**
 * <p>
 * Generates Dialog Form source files.
 * </p>
 *
 * <p>
 * Command line invocation is supported.
 * </p>
 *
 * <pre>
 * mvn com.axonivy.ivy.ci:project-build-plugin:generate-dialog-form-sources
 * </pre>
 *
 * <p>
 * Source generation can be skipped using:
 * </p>
 *
 * <pre>
 * mvn com.axonivy.ivy.ci:project-build-plugin:generate-dialog-form-sources
 * -Divy.generate.dialog.form.sources.skip=true
 * </pre>
 *
 * @since 13.2.0
 */
@Mojo(name = GenerateDialogFormSourcesMojo.GOAL, threadSafe = true)
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
    projectBuilder.generateSources(project.getBasedir().toPath(), "DialogFormSourceGenerator");
  }
}
