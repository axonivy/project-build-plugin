package ch.ivyteam.ivy.maven;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
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
   * Whether to skip the creation of the application zip. Defaults to {@code true}.
   */
  @Parameter(property = "ivy.pack.app.skip", defaultValue = "true")
  boolean skipPackApp;

  @Inject
  private MavenProjectHelper projectHelper;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (skipPackApp) {
      return;
    }
    var appZip = getAppZipFile();
    createAppArchive(appZip);
    projectHelper.attachArtifact(project, "zip", appZip);
    getLog().info("Created app zip at " + appZip);
  }

  private File getAppZipFile() {
    var appZipName = project.getBuild().getFinalName() + ".zip";
    return Path.of(project.getBuild().getDirectory()).resolve(appZipName).toFile();
  }

  private void createAppArchive(File appZip) throws MojoExecutionException {
    var archiver = new ZipArchiver();
    archiver.setDestFile(appZip);

    var deps = MavenDependencies.of(project)
        .typeFilter("iar")
        .all().stream()
        .map(Path::toFile);

    Stream.concat(Stream.of(project.getArtifact().getFile()), deps)
        .filter(Objects::nonNull)
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
      throw new MojoExecutionException("Failed to create app zip: " + appZip, ex);
    }
  }
}
