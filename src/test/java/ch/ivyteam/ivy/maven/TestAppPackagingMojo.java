package ch.ivyteam.ivy.maven;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
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

  private AppPackagingMojo mojo;

  @BeforeEach
  @InjectMojo(goal = AppPackagingMojo.GOAL)
  void setUp(AppPackagingMojo pack) {
    this.mojo = pack;
  }

  @Provides
  MavenProject provideMockedComponent() throws IOException {
    return ProjectExtension.project();
  }

  @Test
  void packsIarDependenciesOnly() throws Exception {
    var basedir = mojo.project.getBasedir().toPath();
    var iarDep = Files.createFile(basedir.resolve("dep-one.iar"));
    var jarDep = Files.createFile(basedir.resolve("dep-two.jar"));

    var artifacts = new HashSet<Artifact>();
    artifacts.add(artifact("dep-one", "1.0.0", "iar", iarDep));
    artifacts.add(artifact("dep-two", "1.0.0", "jar", jarDep));

    Mockito.lenient().when(mojo.project.getArtifacts()).thenReturn(artifacts);

    mojo.execute();

    var appZip = Path.of(mojo.project.getBuild().getDirectory()).resolve("app.zip");
    assertThat(appZip).exists();
    try (var zip = new ZipFile(appZip.toFile())) {
      assertThat(zip.getEntry("dep-one.iar")).isNotNull();
      assertThat(zip.getEntry("dep-two.jar")).isNull();
    }
  }

  @Test
  void packsConfigDirectoryIfPresent() throws Exception {
    Mockito.when(mojo.project.getArtifacts()).thenReturn(Set.of());
    var configFile = mojo.project.getBasedir().toPath().resolve("config/app.yaml");
    Files.createDirectories(configFile.getParent());
    Files.writeString(configFile, "name: demo");

    mojo.execute();

    var appZip = Path.of(mojo.project.getBuild().getDirectory()).resolve("app.zip");
    assertThat(appZip).exists();
    try (var zip = new ZipFile(appZip.toFile())) {
      assertThat(zip.getEntry("config/app.yaml")).isNotNull();
    }
  }

  @Test
  void packsReactorDependenciesFromPackedIar() throws Exception {
    var basedir = mojo.project.getBasedir().toPath();
    var reactorProject = basedir.resolve("reactor-project");
    var packedIar = reactorProject.resolve("target/reactor-project.iar");
    Files.createDirectories(packedIar.getParent());
    Files.writeString(packedIar, "iar");

    Mockito.when(mojo.project.getArtifacts())
        .thenReturn(Set.of(artifact("reactor-project", "1.0.0", "iar", reactorProject)));

    mojo.execute();

    var appZip = Path.of(mojo.project.getBuild().getDirectory()).resolve("app.zip");
    assertThat(appZip).exists();
    try (var zip = new ZipFile(appZip.toFile())) {
      assertThat(zip.getEntry("reactor-project.iar")).isNotNull();
    }
  }

  private static Artifact artifact(String artifactId, String version, String type, Path file) throws IOException {
    var artifact = new ArtifactStubFactory()
        .createArtifact("ch.ivyteam.project.test", artifactId, version, "runtime", type, "");
    artifact.setFile(file.toFile());
    return artifact;
  }
}
