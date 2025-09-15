package ch.ivyteam.ivy.maven;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

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

  @Override
  public void execute() throws MojoExecutionException {
    if (skipMvnDependency) {
      return;
    }
    getLog().info("Copy maven dependencies...");

    var deps = MavenDependencies.of(project).localTransient();
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

}
