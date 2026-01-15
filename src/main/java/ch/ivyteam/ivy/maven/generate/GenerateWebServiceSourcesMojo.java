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
import ch.ivyteam.ivy.process.io.JsonProc;
import ch.ivyteam.ivy.process.model.WebserviceProcess;
import ch.ivyteam.ivy.webservice.process.restricted.WebserviceSourceGenerator;

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
public class GenerateWebServiceSourcesMojo extends AbstractMojo {
  public static final String GOAL = "generate-web-service-sources";

  /**
   * Set to <code>true</code> to bypass the generation of <b>ivy web service sources</b>.
   * @since 13.2.0
   */
  @Parameter(property = "ivy.generate.web.service.sources.skip", defaultValue = "false")
  boolean skipGenerateSources;

  @Parameter(property = "project", required = true, readonly = true)
  MavenProject project;

  private static final String[] INCLUDEDS = {IvyConstants.DIRECTORY_PROCESSES + "/**/*." + IvyConstants.PROCESS_EXTENSION};

  @Override
  public void execute() {
    if (skipGenerateSources) {
      return;
    }
    getLog().info("Generating Ivy web service process sources...");
    var jsonFiles = processFiles();
    var projectDir = project.getBasedir().toPath();
    var writer = new NioSourceWriter(projectDir);
    for (var jsonFile : jsonFiles) {
      try (var is = Files.newInputStream(projectDir.resolve(jsonFile))) {
        var process = JsonProc.read("srcGenProcess", is);
        if (process instanceof WebserviceProcess wsProcess) {
          WebserviceSourceGenerator.generate(wsProcess, writer);
        }
      } catch (IOException ex) {
        throw new UncheckedIOException(ex);
      }
    }
  }

  private String[] processFiles() {
    var scanner = new DirectoryScanner();
    scanner.setBasedir(project.getBasedir());
    scanner.setIncludes(INCLUDEDS);
    scanner.scan();
    return scanner.getIncludedFiles();
  }
}
