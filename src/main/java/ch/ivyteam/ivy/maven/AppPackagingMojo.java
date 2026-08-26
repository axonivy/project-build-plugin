package ch.ivyteam.ivy.maven;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.util.DefaultFileSet;
import org.codehaus.plexus.archiver.zip.ZipArchiver;

import ch.ivyteam.ivy.maven.util.MavenDependencies;

/**
 * Creates an application zip containing the packaged project and its IAR dependencies.
 * The resulting zip is written to the Maven build directory.
 * If the project contains a {@code config/app} directory, its contents are included in the zip.
 *
 * @since 14.0.0
 */
@Mojo(name = AppPackagingMojo.GOAL, requiresDependencyResolution = ResolutionScope.RUNTIME_PLUS_SYSTEM, threadSafe = true)
public class AppPackagingMojo extends AbstractMojo {
  public static final String GOAL = "pack-app";

  @Parameter(property = "project", required = true, readonly = true)
  MavenProject project;

  /**
   * Whether to create the application zip. Defaults to {@code false}.
   * When enabled, the archive contains the project IAR, all runtime IAR
   * dependencies, and the optional {@code config/app} project directory.
   */
  @Parameter(property = "ivy.run.pack.app", defaultValue = "false")
  boolean runPackApp;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (!runPackApp) {
      return;
    }
    var appZip = getAppZipFile();
    createAppArchive(appZip);
    getLog().info("Created app zip at " + appZip.toAbsolutePath());
  }

  private Path getAppZipFile() {
    var appZipName = project.getBuild().getFinalName() + ".zip";
    return Path.of(project.getBuild().getDirectory()).resolve(appZipName);
  }

  private void createAppArchive(Path appZip) throws MojoExecutionException {
    var archiver = new ZipArchiver();
    archiver.setDestFile(appZip.toFile());

    var deps = MavenDependencies.of(project)
        .typeFilter("iar")
        .all().stream()
        .map(Path::toFile);

    Stream.concat(Stream.of(project.getArtifact().getFile()), deps)
        .filter(file -> file.isFile() && file.getName().endsWith(".iar"))
        .forEach(file -> archiver.addFile(file, file.getName()));

    var appConfigDir = project.getBasedir().toPath().resolve("config").resolve("app");
    if (Files.isDirectory(appConfigDir)) {
      archiver.addFileSet(DefaultFileSet.fileSet(appConfigDir.toFile())
          .includeEmptyDirs(false));
    }

    try {
      archiver.createArchive();
    } catch (ArchiverException | IOException ex) {
      throw new MojoExecutionException("Failed to create app zip: " + appZip.toAbsolutePath(), ex);
    }
  }
}
