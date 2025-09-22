package ch.ivyteam.ivy.maven.generate;

import java.io.IOException;
import java.nio.file.Files;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;

import ch.ivyteam.ivy.IvyConstants;
import ch.ivyteam.ivy.scripting.dataclass.internal.serialization.DataClassSerializer;
import ch.ivyteam.ivy.scripting.dataclass.mapper.IvyScriptClassInfoMapper;
import ch.ivyteam.ivy.scripting.dataclass.restricted.codegen.DataClassJavaSource;

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
public class GenerateDataClassSourcesMojo extends AbstractMojo {
  public static final String GOAL = "generate-data-class-sources";

  /**
   * Set to <code>true</code> to bypass the generation of <b>ivy data classes</b>.
   * @since 13.2.0
   */
  @Parameter(property = "ivy.generate.data.class.sources.skip", defaultValue = "false")
  boolean skipGenerateSources;

  @Parameter(property = "project", required = true, readonly = true)
  MavenProject project;

  public static final String[] INCLUDEDS = {
      IvyConstants.DIRECTORY_DATACLASSES + "/**/*." + IvyConstants.DATA_CLASS_EXTENSION,
      IvyConstants.DIRECTORY_SRC_HD + "/**/*." + IvyConstants.DATA_CLASS_EXTENSION
  };

  @Override
  public void execute() {
    if (skipGenerateSources) {
      return;
    }
    getLog().info("Generating Ivy data class sources...");
    var scanner = new DirectoryScanner();
    scanner.setBasedir(project.getBasedir());
    scanner.setIncludes(INCLUDEDS);
    scanner.scan();
    var jsonFiles = scanner.getIncludedFiles();
    var projectDir = project.getBasedir().toPath();
    var writer = new NioSourceWriter(projectDir);
    for (var jsonFile : jsonFiles) {
      try {
        var model = DataClassSerializer.load(Files.newInputStream(projectDir.resolve(jsonFile))).model();
        var classInfo = IvyScriptClassInfoMapper.toIvyScriptClassInfo(model);
        new DataClassJavaSource(classInfo).write(writer);
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
    }
  }
}
