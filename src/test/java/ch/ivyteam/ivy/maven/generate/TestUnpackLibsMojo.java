package ch.ivyteam.ivy.maven.generate;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.api.di.Provides;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import ch.ivyteam.ivy.maven.extension.ProjectExtension;
import ch.ivyteam.ivy.maven.util.PathUtils;

@MojoTest
@ExtendWith(ProjectExtension.class)
class TestUnpackLibsMojo {

  private final Path jar = Path.of("src/test/resources/lib.jar");
  private UnpackLibsMojo mojo;

  @BeforeEach
  @InjectMojo(goal = UnpackLibsMojo.GOAL)
  void setUp(UnpackLibsMojo unpack) {
    this.mojo = unpack;
  }

  @Provides
  MavenProject provideMockedComponent() throws IOException {
    return ProjectExtension.project();
  }

  @Test
  void unpackLib() throws Exception {
    var projectDir = mojo.project.getBasedir().toPath();
    var restJar = projectDir.resolve("lib/generated/rest/lib.jar");
    Files.createDirectories(restJar.getParent());
    Files.copy(jar, restJar);
    var outDir = projectDir.resolve(mojo.project.getBuild().getOutputDirectory());
    PathUtils.delete(outDir);
    assertThat(outDir).doesNotExist();
    mojo.execute();
    assertThat(outDir).exists();
    assertThat(outDir.resolve("openapi.json")).exists();
    assertThat(outDir.resolve("api/v3/client/Address.class")).exists();
    assertThat(outDir.resolve("api/v3/client/Address.java")).doesNotExist();
    assertThat(outDir.resolve("META-INF")).doesNotExist();
  }

  @Test
  void unpackLibExlusions() throws Exception {
    var projectDir = mojo.project.getBasedir().toPath();
    var restJar = projectDir.resolve("lib/generated/rest/lib.jar");
    Files.createDirectories(restJar.getParent());
    Files.copy(jar, restJar);
    var outDir = projectDir.resolve(mojo.project.getBuild().getOutputDirectory());
    PathUtils.delete(outDir);
    assertThat(outDir).doesNotExist();
    mojo.unpackExcludes = new String[] {"**/lib.jar"};
    mojo.execute();
    assertThat(outDir)
        .as("remain empty because no jar is processed due to excludes")
        .doesNotExist();
  }

  @Test
  void unpackLibInclusions() throws Exception {
    var projectDir = mojo.project.getBasedir().toPath();
    var libJar = projectDir.resolve("lib/lib.jar");
    Files.copy(jar, libJar);
    var outDir = projectDir.resolve(mojo.project.getBuild().getOutputDirectory());
    PathUtils.delete(outDir);
    assertThat(outDir).doesNotExist();
    mojo.unpackIncludes = new String[] {"lib/lib.jar"};
    mojo.execute();
    assertThat(outDir)
        .as("jar is processed due to includes")
        .exists();
    assertThat(outDir.resolve("openapi.json")).exists();
    assertThat(outDir.resolve("api/v3/client/Address.class")).exists();
    assertThat(outDir.resolve("api/v3/client/Address.java")).doesNotExist();
    assertThat(outDir.resolve("META-INF")).doesNotExist();
  }
}
