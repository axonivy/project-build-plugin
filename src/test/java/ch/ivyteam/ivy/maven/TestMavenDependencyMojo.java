package ch.ivyteam.ivy.maven;

import static ch.ivyteam.ivy.maven.extension.ProjectExtension.TEST_BASE;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.api.di.Provides;
import org.apache.maven.api.plugin.testing.Basedir;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoExtension;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.testing.ArtifactStubFactory;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import ch.ivyteam.ivy.maven.util.MavenDependencies;
import ch.ivyteam.ivy.maven.util.PathUtils;

@MojoTest
class TestMavenDependencyMojo {

  private MavenDependencyMojo mojo;
  private final Path projectDir = Path.of(TEST_BASE);

  @BeforeEach
  @InjectMojo(goal = MavenDependencyMojo.GOAL)
  void setUp(MavenDependencyMojo dependency) {
    this.mojo = dependency;
  }

  @AfterEach
  void tearDown() {
    artifacts.clear();
    var mvnLibDir = projectDir.resolve("lib").resolve("mvn-deps");
    PathUtils.delete(mvnLibDir);
  }

  @Provides
  MavenProject provideMockedComponent() throws IOException {
    MavenProject pom = Mockito.mock(MavenProject.class);
    var self = new ArtifactStubFactory().createArtifact("ch.ivyteam.project.test", "base", "1.0.0", "iar");
    Mockito.lenient().when(pom.getArtifact()).thenReturn(self);
    Mockito.lenient().when(pom.getArtifacts()).thenReturn(artifacts);
    Mockito.lenient().when(pom.getGroupId()).thenReturn("ch.ivyteam.project.test");
    Mockito.lenient().when(pom.getArtifactId()).thenReturn("base");
    Mockito.lenient().when(pom.getBasedir())
        .thenReturn(Paths.get(MojoExtension.getBasedir()).toFile());
    return pom;
  }

  private final Set<Artifact> artifacts = new HashSet<>();

  @Test
  @Basedir(TEST_BASE)
  void noMavenDeps() throws Exception {
    var mvnLibDir = projectDir.resolve("lib").resolve("mvn-deps");
    assertThat(mvnLibDir).doesNotExist();
    mojo.execute();
    assertThat(mvnLibDir).doesNotExist();
  }

  @Test
  @Basedir(TEST_BASE)
  void exportMavenDepsToLibDir() throws Exception {
    var mvnLibDir = projectDir.resolve("lib").resolve("mvn-deps");
    assertThat(mvnLibDir).doesNotExist();
    Artifact artifact = new ArtifactStubFactory().createArtifact("io.jsonwebtoken", "jjwt", "0.9.1");

    var self = new ArtifactStubFactory().createArtifact("ch.ivyteam.project.test", "base", "1.0.0", "iar");
    artifact.setDependencyTrail(List.of(self.toString()));
    artifact.setFile(Path.of("src/test/resources/jjwt-0.9.1.jar").toFile());

    this.artifacts.add(artifact);
    mojo.execute();
    assertThat(mvnLibDir).exists();
    List<String> libs = getMavenLibs(mvnLibDir);
    assertThat(libs).contains("jjwt-0.9.1.jar");
  }

  @Test
  @Basedir(TEST_BASE)
  void onlyLocalDeps() throws Exception {
    var mvnLibDir = projectDir.resolve("lib").resolve("mvn-deps");
    assertThat(mvnLibDir).doesNotExist();
    Artifact artifact = new ArtifactStubFactory().createArtifact("io.jsonwebtoken", "jjwt", "0.9.1");
    artifact.setFile(Path.of("src/test/resources/jjwt-0.9.1.jar").toFile());
    artifact.setDependencyTrail(
        List.of(mojo.project.getArtifact().toString(), "other.group:other.artifact:iar:1.0.0"));
    artifacts.add(artifact);
    mojo.execute();
    assertThat(getMavenLibs(mvnLibDir))
        .as("libs provided through a dependent 'iar' should not be packed.")
        .isEmpty();
  }

  @Test
  @Basedir(TEST_BASE)
  void m2eDepsHint(@TempDir Path tempDir) throws Exception {
    var m2eDeps = tempDir.resolve("m2e.deps");
    assertThat(m2eDeps).doesNotExist();
    MavenDependencyMojo.writeM2eDependencyHint(tempDir, MavenDependencies.of(mojo.project).localTransient());
    assertThat(m2eDeps).content().isEmpty();

    var artifact = new ArtifactStubFactory().createArtifact("io.jsonwebtoken", "jjwt", "0.9.1");
    var self = new ArtifactStubFactory().createArtifact("ch.ivyteam.project.test", "base", "1.0.0", "iar");
    artifact.setDependencyTrail(List.of(self.toString()));
    var jar = Path.of("src/test/resources/jjwt-0.9.1.jar");
    artifact.setFile(jar.toFile());
    this.artifacts.add(artifact);

    MavenDependencyMojo.writeM2eDependencyHint(tempDir, MavenDependencies.of(mojo.project).localTransient());
    assertThat(m2eDeps).content().isEqualToIgnoringNewLines(jar.toString());
  }

  private static List<String> getMavenLibs(Path mvnLibDir) throws IOException {
    if (!Files.isDirectory(mvnLibDir)) {
      return Collections.emptyList();
    }
    try (var walker = Files.walk(mvnLibDir, 1)) {
      return walker
          .filter(p -> !p.equals(mvnLibDir))
          .map(p -> p.getFileName().toString())
          .collect(Collectors.toList());
    }
  }

}
