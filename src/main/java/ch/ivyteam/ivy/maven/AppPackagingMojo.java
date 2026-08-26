package ch.ivyteam.ivy.maven;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
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
 * Packs all IAR dependencies into an application zip.
 *
 * @since 14.0.0
 */
@Mojo(name = AppPackagingMojo.GOAL, requiresDependencyResolution = ResolutionScope.RUNTIME_PLUS_SYSTEM, threadSafe = true)
public class AppPackagingMojo extends AbstractMojo {
  public static final String GOAL = "pack-app";

  @Parameter(property = "project", required = true, readonly = true)
  MavenProject project;

  /**
   * Directory containing the generated app zip.
   */
  @Parameter(defaultValue = "${project.build.directory}", property = "ivy.app.output.directory")
  Path appOutputDirectory;

  /**
   * Name of the generated app zip without extension.
   */
  @Parameter(defaultValue = "app", property = "ivy.app.final.name")
  String finalName;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    var appZip = getAppZipFile();
    createAppArchive(appZip);
    getLog().info("Created app archive at " + appZip.toAbsolutePath());
  }

  private Path getAppZipFile() {
    var basedir = appOutputDirectory != null ? appOutputDirectory : Path.of(project.getBuild().getDirectory());
    var resultFinalName = finalName != null ? finalName : "app";
    var fileName = resultFinalName + ".zip";
    return basedir.resolve(fileName);
  }

  private void createAppArchive(Path appZip) throws MojoExecutionException {
    var deps = MavenDependencies.of(project)
        .typeFilter("iar")
        .all();

    var archiver = new ZipArchiver();
    archiver.setDestFile(appZip.toFile());

    for (var dep : deps) {
      var dependencyFile = dep.toFile();
      if (dependencyFile.isFile() && dependencyFile.getName().endsWith(".iar")) {
        archiver.addFile(dependencyFile, dependencyFile.getName());
      } else if (dependencyFile.isDirectory()) {
        try {
          var packedIar = findPackedIar(dep);
          if (packedIar.isPresent()) {
            var iar = packedIar.get().toFile();
            archiver.addFile(iar, iar.getName());
          } else {
            getLog().warn("Cannot add dependency to app zip '" + dep
                + "'. Dependency type is 'iar' but no packed IAR was found in its target directory.");
          }
        } catch (IOException ex) {
          throw new MojoExecutionException("Failed while searching packed IAR in " + dep, ex);
        }
      } else {
        getLog().warn("Cannot add dependency to app zip '" + dep + "'. Dependency is not a file or directory.");
      }
    }

    var configDir = project.getBasedir().toPath().resolve("config");
    if (Files.isDirectory(configDir)) {
      archiver.addFileSet(DefaultFileSet.fileSet(configDir.toFile())
          .prefixed("config/")
          .includeEmptyDirs(false));
    }

    try {
      archiver.createArchive();
    } catch (ArchiverException | IOException ex) {
      throw new MojoExecutionException("Failed to create app zip: " + appZip.toAbsolutePath(), ex);
    }
  }

  static Optional<Path> findPackedIar(Path dep) throws IOException {
    var target = dep.resolve("target");
    if (!Files.isDirectory(target)) {
      return Optional.empty();
    }
    try (Stream<Path> find = Files.find(target, 1,
        (p, _) -> p.getFileName().toString().endsWith(".iar"))) {
      return find.findAny();
    }
  }
}
