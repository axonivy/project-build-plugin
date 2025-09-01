package ch.ivyteam.ivy.maven.generate;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Rule;
import org.junit.Test;

import ch.ivyteam.ivy.maven.ProjectMojoRule;
import ch.ivyteam.ivy.maven.util.PathUtils;

public class TestUnpackLibsMojo {

  @Rule
  public ProjectMojoRule<UnpackLibsMojo> unpack = new ProjectMojoRule<>(
      Path.of("src/test/resources/base"), UnpackLibsMojo.GOAL);

  Path jar = Path.of("src/test/resources/lib.jar");

  @Test
  public void unpackLib() throws Exception {
    var mojo = unpack.getMojo();
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
  public void unpackLibExlusions() throws Exception {
    var mojo = unpack.getMojo();
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
  public void unpackLibInclusions() throws Exception {
    var mojo = unpack.getMojo();
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
