package ch.ivyteam.ivy.maven.compile;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import ch.ivyteam.ivy.maven.engine.MavenProjectBuilderProxy;
import ch.ivyteam.ivy.maven.util.MavenDependencies;

/**
 * Validates an ivy Project with an ivyEngine.
 *
 */
@Mojo(name = ValidateProjectMojo.GOAL, requiresDependencyResolution = ResolutionScope.COMPILE)
public class ValidateProjectMojo extends AbstractEngineInstanceMojo {
  public static final String GOAL = "validateProject";

  /**
   * Set to <code>false</code> to perform the validation of ivyScript code
   * within ivy processes.
   * @since 8.0.3
   */
  @Parameter(property = "ivy.script.validation.skip", defaultValue = "false")
  boolean skipScriptValidation;

  @Override
  protected void engineExec(MavenProjectBuilderProxy projectBuilder) throws Exception {
    if (skipScriptValidation) {
      getLog().info("Skipping ivy script validation");
      return;
    }
    var iarDependencies = getDependencies("iar");
    var dependencyClassPath = getDependencyClasspath();
    projectBuilder.validate(project.getBasedir().toPath(), iarDependencies, dependencyClassPath);
  }

  private String getDependencyClasspath() {
    var jarDepPaths = getDependencies("jar")
        .stream()
        .map(Path::toFile)
        .map(File::getAbsolutePath)
        .toList();
    return StringUtils.join(jarDepPaths, File.pathSeparatorChar);
  }

  private List<Path> getDependencies(String type) {
    return MavenDependencies.of(project).typeFilter(type).all();
  }

}
