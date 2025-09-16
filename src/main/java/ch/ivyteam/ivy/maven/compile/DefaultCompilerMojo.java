package ch.ivyteam.ivy.maven.compile;

import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.compiler.CompilationFailureException;
import org.apache.maven.plugin.compiler.CompilerMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

@Mojo(
    name = "default-compile",
    defaultPhase = LifecyclePhase.COMPILE,
    threadSafe = true,
    requiresDependencyResolution = ResolutionScope.COMPILE)
public class DefaultCompilerMojo extends CompilerMojo {

  @Parameter(defaultValue = "${project.compileSourceRoots}", readonly = false, required = true)
  private List<String> compileSourceRoots;

  @Parameter(property = "project", required = true, readonly = true)
  MavenProject project;

  @Parameter(property = "ivy.compiler.skip", defaultValue = "false")
  boolean skipCompilation;

  @Parameter(property = "ivy.compiler.use.default", defaultValue = "false")
  boolean useDefaultCompiler;

  @Override
  protected List<String> getCompileSourceRoots() {
    getLog().info("Compile source roots: " + compileSourceRoots);
    return compileSourceRoots;
  }

  @Override
  public void execute() throws MojoExecutionException, CompilationFailureException {
    if (skipCompilation || !useDefaultCompiler) {
      return;
    }
    super.execute();
  }

}
