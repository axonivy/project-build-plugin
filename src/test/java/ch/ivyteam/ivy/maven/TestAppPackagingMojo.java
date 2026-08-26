package ch.ivyteam.ivy.maven;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.zip.ZipFile;

import org.apache.maven.api.di.Provides;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.testing.ArtifactStubFactory;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import ch.ivyteam.ivy.maven.extension.ProjectExtension;

@MojoTest
@ExtendWith(ProjectExtension.class)
class TestAppPackagingMojo {

  AppPackagingMojo mojo;
  Path appZip;

  @BeforeEach
  @InjectMojo(goal = AppPackagingMojo.GOAL)
  void setUp(AppPackagingMojo pack) throws IOException {
    mojo = pack;
    mojo.runPackApp = true;
    var selfIar = Files.createFile(mojo.project.getBasedir().toPath().resolve("self.iar"));
    mojo.project.getArtifact().setFile(selfIar.toFile());
    appZip = Path.of(mojo.project.getBuild().getDirectory()).resolve("base-1.0.0.zip");
  }

  @Provides
  MavenProject provideMockedComponent() throws IOException {
    return ProjectExtension.project();
  }

  @Test
  void skipPackApp() throws Exception {
    mojo.runPackApp = false;
    assertThat(appZip).doesNotExist();
    mojo.execute();
    assertThat(appZip).doesNotExist();
  }

  @Test
  void packApp() throws Exception {
    assertThat(appZip).doesNotExist();
    mojo.execute();
    assertThat(appZip).exists();
    try (var zip = new ZipFile(appZip.toFile())) {
      assertThat(zip.getEntry("self.iar")).isNotNull();
      assertThat(zip.getEntry("config")).isNull();
    }
  }

  @Test
  void packAppWithDeps() throws Exception {
    var basedir = mojo.project.getBasedir().toPath();
    var iarDep = Files.createFile(basedir.resolve("dep-one.iar"));
    var jarDep = Files.createFile(basedir.resolve("dep-two.jar"));

    var artifacts = Set.of(artifact("dep-one", "1.0.0", "iar", iarDep),
        artifact("dep-two", "1.0.0", "jar", jarDep));
    Mockito.lenient().when(mojo.project.getArtifacts()).thenReturn(artifacts);

    mojo.execute();

    assertThat(appZip).exists();
    try (var zip = new ZipFile(appZip.toFile())) {
      assertThat(zip.getEntry("dep-one.iar")).isNotNull();
      assertThat(zip.getEntry("self.iar")).isNotNull();
      assertThat(zip.getEntry("dep-two.jar")).isNull();
      assertThat(zip.getEntry("config")).isNull();
    }
  }

  @Test
  void packAppWithConfig() throws Exception {
    var appYaml = mojo.project.getBasedir().toPath().resolve("config/app/config/app.yaml");
    Files.createDirectories(appYaml.getParent());
    Files.writeString(appYaml, "test: value");
    mojo.execute();
    assertThat(appZip).exists();
    try (var zip = new ZipFile(appZip.toFile())) {
      assertThat(zip.getEntry("self.iar")).isNotNull();
      assertThat(zip.getEntry("config/app.yaml")).isNotNull();
    }
  }

  static Artifact artifact(String artifactId, String version, String type, Path file) throws IOException {
    var artifact = new ArtifactStubFactory()
        .createArtifact("ch.ivyteam.project.test", artifactId, version, "runtime", type, "");
    artifact.setFile(file.toFile());
    return artifact;
  }
}
