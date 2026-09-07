package ch.ivyteam.ivy.maven;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;
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
@Mojo(name = MavenDependencyMojo.GOAL, requiresDependencyResolution = ResolutionScope.RUNTIME_PLUS_SYSTEM, threadSafe = true)
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
    var targetDir = Path.of(project.getBuild().getDirectory());
    var mvnLibDir = targetDir.resolve("lib").resolve("mvn-deps");

    if (isM2eBuild()) {
      cleanupDependencies(mvnLibDir, deps);
    }

    getLog().info("Copy maven dependencies...");
    if (deps.isEmpty()) {
      getLog().info("No maven dependencies were found.");
      return;
    }
    try {
      Files.createDirectories(mvnLibDir);
      copyDependency(mvnLibDir, deps);
    } catch (IOException ex) {
      throw new MojoExecutionException("Failed to create mvn-deps directory", ex);
    }
  }

  private void copyDependency(Path mvnLibDir, List<Artifact> deps) {
    var count = 0;
    for (var dep : deps) {
      var depFilePath = dep.getFile().toPath();
      try {
        if (isM2eBuild() && Files.isDirectory(depFilePath)) {
          handleWorkspaceDependency(dep, mvnLibDir);
          continue;
        }
        Files.copy(depFilePath, mvnLibDir.resolve(depFilePath.getFileName().toString()));
        getLog().debug("Copied dependency: " + depFilePath.getFileName());
        count++;
      } catch (FileAlreadyExistsException _) {
        getLog().debug("Ignore dependecy '" + depFilePath.getFileName() + "' as it already exists at: " + mvnLibDir);
      } catch (IOException ex) {
        getLog().warn("Couldn't copy depedency '" + deps + "' to: " + mvnLibDir, ex);
      }
    }
    getLog().info("Maven dependecies: " + count + " copied.");
  }

  private boolean isM2eBuild() {
    return "EclipseBuildContext".equals(buildContext.getClass().getSimpleName());
  }

  protected static void handleWorkspaceDependency(Artifact artifact, Path mvnLibDir) throws IOException {
    var targetDir = artifact.getFile().toPath().getParent(); // in default case the parent should point to target folder
    var expectedJarName = expectedJarName(artifact);
    if (expectedJarName == null) {
      return;
    }
    try (var targetPaths = Files.walk(targetDir)) {
      targetPaths.filter(Files::isRegularFile)
          .filter(path -> path.toString().endsWith(expectedJarName))
          .forEach(jar -> {
            try {
              Files.copy(jar, mvnLibDir.resolve(jar.getFileName().toString()));
            } catch (IOException e) {
              throw new UncheckedIOException(e);
            }
          });
    }
  }

  private static String expectedJarName(Artifact artifact) {
    if (artifact.getArtifactId() == null || artifact.getVersion() == null) {
      return null;
    }
    var jarName = artifact.getArtifactId() + "-" + artifact.getVersion();
    if (artifact.getClassifier() != null && !artifact.getClassifier().isBlank()) {
      jarName += "-" + artifact.getClassifier();
    }
    return jarName + ".jar";
  }

  protected static void cleanupDependencies(Path mvnLibDir, List<Artifact> deps) {
    if (!Files.isDirectory(mvnLibDir)) {
      return;
    }
    var expectedJars = deps.stream()
        .map(Artifact::getFile)
        .map(File::toPath)
        .map(Path::getFileName)
        .map(Path::toString)
        .toList();
    var scanner = new DirectoryScanner();
    scanner.setBasedir(mvnLibDir.toFile());
    scanner.scan();
    Stream.of(scanner.getIncludedFiles())
        .filter(jar -> !expectedJars.contains(jar))
        .map(jar -> mvnLibDir.resolve(jar))
        .forEach(jar -> {
          try {
            Files.delete(jar);
          } catch (IOException _) {}
        });
  }

}
