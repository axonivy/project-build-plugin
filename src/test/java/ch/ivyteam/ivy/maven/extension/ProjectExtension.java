package ch.ivyteam.ivy.maven.extension;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.apache.maven.model.Build;
import org.apache.maven.plugin.testing.ArtifactStubFactory;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.Mockito;

import ch.ivyteam.ivy.maven.util.PathUtils;

public class ProjectExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback {

  public static final String TEST_BASE = "src/test/resources/base/";

  private static Path project;
  private static Path workspace;

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    workspace = Files.createTempDirectory("copySpace");
    project = workspace.resolve("base_" + context.getDisplayName());
    System.setProperty("basedir", project.toString());
  }

  @Override
  public void afterAll(ExtensionContext context) throws Exception {
    PathUtils.delete(workspace);
  }

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    copyDirectory(Path.of(TEST_BASE), project);
  }

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    PathUtils.delete(project);
  }

  public static MavenProject project() throws IOException {
    MavenProject pom = Mockito.mock(MavenProject.class);
    var self = new ArtifactStubFactory()
        .createArtifact("ch.ivyteam.project.test", "base", "1.0.0", "iar");
    Mockito.lenient().when(pom.getArtifact()).thenReturn(self);
    Mockito.lenient().when(pom.getGroupId()).thenReturn("ch.ivyteam.project.test");
    Mockito.lenient().when(pom.getArtifactId()).thenReturn("base");
    Mockito.lenient().when(pom.getVersion()).thenReturn("1.0.0");

    var build = new Build();
    var target = project.resolve("target");
    build.setDirectory(target.toString());
    build.setOutputDirectory(target.resolve("classes").toString());

    Mockito.lenient().when(pom.getBuild()).thenReturn(build);
    Mockito.lenient().when(pom.getProperties())
        .thenReturn(new Properties());
    return pom;
  }

  private static void copyDirectory(Path source, Path target) {
    try (var walker = Files.walk(source)) {
      walker.forEach(fileToCopy -> {
        if (fileToCopy.equals(source)) {
          return;
        }
        var destination = source.relativize(fileToCopy);
        var fileToMove = target.resolve(destination);

        try {
          var dirToMove = fileToMove.getParent();
          if (!Files.exists(dirToMove)) {
            Files.createDirectories(dirToMove);
          }
          Files.copy(fileToCopy, fileToMove);
        } catch (IOException ex) {
          throw new UncheckedIOException(ex);
        }
      });
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

}
