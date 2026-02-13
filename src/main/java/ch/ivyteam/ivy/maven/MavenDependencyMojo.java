package ch.ivyteam.ivy.maven;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.inject.Inject;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.sonatype.plexus.build.incremental.BuildContext;

import ch.ivyteam.ivy.maven.util.MavenDependencies;

/**
 * Copy <a href="https://maven.apache.org/pom.html#Dependencies">maven
 * dependencies</a> to a specific folder.
 *
 * <p>
 * To reduce the size of your ivy archives, make sure that your dependencies are
 * configured correctly:
 * </p>
 * <ul>
 * <li>Mark test dependencies with the scope <b>test</b></li>
 * <li><a href="https://maven.apache.org/pom.html#exclusions">Exclude transient
 * dependencies</a> which are already delivered by the core</li>
 * </ul>
 *
 * @since 9.2.0
 */
@Mojo(name = MavenDependencyMojo.GOAL, requiresDependencyResolution = ResolutionScope.RUNTIME_PLUS_SYSTEM)
public class MavenDependencyMojo extends AbstractMojo {
  public static final String GOAL = "maven-dependency";

  /**
   * Set to <code>true</code> to bypass the copy of <b>maven dependencies</b>.
   */
  @Parameter(property = "ivy.mvn.dep.skip", defaultValue = "false")
  boolean skipMvnDependency;

  @Parameter(property = "project", required = true, readonly = true)
  MavenProject project;

  @Inject
  private BuildContext buildContext;

  @Override
  public void execute() throws MojoExecutionException {
    if (skipMvnDependency) {
      return;
    }

    var deps = MavenDependencies.of(project).localTransient();

    if (isM2eEBuild()) {
      writeM2eDependencyHint(Path.of(project.getBuild().getDirectory()), deps);
      return;
    }

    getLog().info("Copy maven dependencies...");
    if (deps.isEmpty()) {
      getLog().info("No maven dependencies were found.");
      return;
    }
    try {
      var mvnLibDir = Files.createDirectories(project.getBasedir().toPath().resolve("lib").resolve("mvn-deps"));
      copyDependency(mvnLibDir, deps);
    } catch (IOException ex) {
      throw new MojoExecutionException("Failed to create mvn-deps directory", ex);
    }
  }

  private void copyDependency(Path mvnLibDir, List<Path> deps) {
    var count = 0;
    for (var dep : deps) {
      try {
        Files.copy(dep, mvnLibDir.resolve(dep.getFileName().toString()));
        getLog().debug("Copied dependency: " + dep.getFileName());
        count++;
      } catch (FileAlreadyExistsException ex) {
        getLog().debug("Ignore dependecy '" + dep.getFileName() + "' as it already exists at: " + mvnLibDir);
      } catch (IOException ex) {
        getLog().warn("Couldn't copy depedency '" + deps + "' to: " + mvnLibDir, ex);
      }
    }
    getLog().info("Maven dependecies: " + count + " copied.");
  }

  private boolean isM2eEBuild() {
    return "EclipseBuildContext".equals(buildContext.getClass().getSimpleName());
  }

  protected static void writeM2eDependencyHint(Path targetDir, List<Path> deps) throws MojoExecutionException {
    var m2eDepsPath = targetDir.resolve("m2e.deps");
    try {
      Files.createDirectories(m2eDepsPath.getParent());
      Files.write(m2eDepsPath, deps.stream().map(Path::toString).toList());
    } catch (IOException ex) {
      throw new MojoExecutionException("Failed to create m2e.deps file", ex);
    }
  }

}
