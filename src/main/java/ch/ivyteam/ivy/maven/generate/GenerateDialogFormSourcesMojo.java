package ch.ivyteam.ivy.maven.generate;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;

import ch.ivyteam.ivy.IvyConstants;
import ch.ivyteam.ivy.dialog.form.io.DialogFormIO;
import ch.ivyteam.ivy.dialog.form.jsf.JsfFormRenderer;
import ch.ivyteam.ivy.dialog.form.jsf.build.JsonFormResourceBuilder;
import ch.ivyteam.util.io.resource.FilePath;

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
@Mojo(name = GenerateDialogFormSourcesMojo.GOAL)
public class GenerateDialogFormSourcesMojo extends AbstractMojo {
  public static final String GOAL = "generate-dialog-form-sources";

  /**
   * Set to <code>true</code> to bypass the generation of <b>ivy dialog form sources</b>.
   * @since 13.2.0
   */
  @Parameter(property = "ivy.generate.dialog.form.sources.skip", defaultValue = "false")
  boolean skipGenerateSources;

  @Parameter(property = "project", required = true, readonly = true)
  MavenProject project;

  private static final String[] INCLUDEDS = {IvyConstants.DIRECTORY_SRC_HD + "/**/*" + DialogFormIO.JSON_EXT};

  @Override
  public void execute() {
    if (skipGenerateSources) {
      return;
    }
    getLog().info("Generating Ivy dialog form sources...");
    var jsonFiles = formFiles();
    var projectDir = project.getBasedir().toPath();
    var writer = new NioSourceWriter(projectDir);
    var renderer = new JsfFormRenderer();
    for (var jsonFile : jsonFiles) {
      try (var is = Files.newInputStream(projectDir.resolve(jsonFile))) {
        var form = DialogFormIO.read(is).form();
        new JsonFormResourceBuilder(form, renderer, FilePath.of(jsonFile)).write(writer);
      } catch (IOException ex) {
        throw new UncheckedIOException(ex);
      }
    }
  }

  private String[] formFiles() {
    var scanner = new DirectoryScanner();
    scanner.setBasedir(project.getBasedir());
    scanner.setIncludes(INCLUDEDS);
    scanner.scan();
    return scanner.getIncludedFiles();
  }
}
