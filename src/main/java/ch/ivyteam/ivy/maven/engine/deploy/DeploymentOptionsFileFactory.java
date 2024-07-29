package ch.ivyteam.ivy.maven.engine.deploy;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.apache.maven.shared.filtering.MavenFilteringException;

import ch.ivyteam.ivy.maven.util.PathUtils;

public class DeploymentOptionsFileFactory {

  private static final String YAML = "yaml";
  private final Path deployableArtifact;

  public DeploymentOptionsFileFactory(Path deployableArtifact) {
    this.deployableArtifact = deployableArtifact;
  }

  public Path createFromTemplate(Path optionsFile, MavenProject project, MavenSession session,
          MavenFileFilter fileFilter) throws MojoExecutionException {
    if (!isOptionsFile(optionsFile.toFile())) {
      return null;
    }

    var ext = PathUtils.toExtension(optionsFile);
    var targetFile = getTargetFile(ext);
    try {
      fileFilter.copyFile(optionsFile.toFile(), targetFile.toFile(), true, project, Collections.emptyList(), false,
              StandardCharsets.UTF_8.name(), session);
    } catch (MavenFilteringException ex) {
      throw new MojoExecutionException("Failed to resolve templates in options file", ex);
    }
    return targetFile;
  }

  private static boolean isOptionsFile(File optionsFile) {
    return optionsFile != null &&
            optionsFile.exists() &&
            optionsFile.isFile() &&
            optionsFile.canRead();
  }

  public Path createFromConfiguration(String options) throws MojoExecutionException {
    var yamlOptionsFile = getTargetFile(YAML);
    try {
      java.nio.file.Files.writeString(yamlOptionsFile, options);
    } catch (IOException ex) {
      throw new MojoExecutionException("Failed to write options file '" + yamlOptionsFile + "'.", ex);
    }
    return yamlOptionsFile;
  }

  private Path getTargetFile(String fileFormat) {
    var prefix = deployableArtifact.getFileName().toString() + ".options.";
    var targetFileName = prefix + fileFormat;
    return deployableArtifact.resolveSibling(targetFileName);
  }
}
