package ch.ivyteam.ivy.maven;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Rule;
import org.junit.Test;

public class TestCleanupMojo {

  @Rule
  public ProjectMojoRule<CleanupMojo> clean = new ProjectMojoRule<>(
      Path.of("src/test/resources/base"), CleanupMojo.GOAL);

  @Test
  public void noMavenDepsDir() throws Exception {
    var mojo = clean.getMojo();
    var mvnLibDir = mojo.project.getBasedir().toPath().resolve("lib").resolve("mvn-deps");
    assertThat(mvnLibDir).doesNotExist();
    mojo.execute();
    assertThat(mvnLibDir).doesNotExist();
  }

  @Test
  public void cleanupMavenDepsDir() throws Exception {
    var mojo = clean.getMojo();
    var mvnLibDir = Files
        .createDirectories(mojo.project.getBasedir().toPath().resolve("lib").resolve("mvn-deps"));
    var mvnDep = Files.copy(Path.of("src/test/resources/jjwt-0.9.1.jar"), mvnLibDir.resolve("jjwt-0.9.1.jar"));
    assertThat(mvnLibDir).exists();
    assertThat(mvnDep).exists();
    mojo.execute();
    assertThat(mvnLibDir).doesNotExist();
  }

  @Test
  public void dontCleanManualLibs() throws Exception {
    var mojo = clean.getMojo();
    var libDir = Files.createDirectories(mojo.project.getBasedir().toPath().resolve("lib"));
    var dep = Files.copy(Path.of("src/test/resources/jjwt-0.9.1.jar"), libDir.resolve("jjwt-0.9.1.jar"));
    assertThat(libDir).exists();
    assertThat(dep).exists();
    mojo.execute();
    assertThat(libDir).exists();
    assertThat(dep).exists();
  }

  @Test
  public void sourceFoldersCleanup() throws Exception {
    var mojo = clean.getMojo();
    var projectDir = mojo.project.getBasedir().toPath();
    var srcDataClasses = projectDir.resolve("src_dataClasses");
    var srcWsproc = projectDir.resolve("src_wsproc");
    var srcGenerated = projectDir.resolve("src_generated");
    Files.createDirectories(srcGenerated);
    var srcData = srcDataClasses.resolve("Data.java");
    Files.writeString(srcData, "Hello");
    assertThat(srcDataClasses).exists();
    assertThat(srcWsproc).exists();
    assertThat(srcGenerated).exists();
    assertThat(srcData).exists();

    mojo.execute();

    assertThat(srcDataClasses).doesNotExist();
    assertThat(srcWsproc).doesNotExist();
    assertThat(srcGenerated).doesNotExist();
  }

  @Test
  public void additionalExclusions() throws Exception {
    var mojo = clean.getMojo();
    var projectDir = mojo.project.getBasedir().toPath();
    var srcDataClasses = projectDir.resolve("src_dataClasses");
    var srcWsproc = projectDir.resolve("src_wsproc");
    mojo.cleanupExcludes = new String[] {"src_dataClasses"};
    assertThat(srcDataClasses).exists();
    assertThat(srcWsproc).exists();

    mojo.execute();

    assertThat(srcDataClasses)
        .as("expected to exist, because of cleanupExcludes")
        .exists();
    assertThat(srcWsproc).doesNotExist();
  }

  @Test
  public void additionaInclusions() throws Exception {
    var mojo = clean.getMojo();
    var projectDir = mojo.project.getBasedir().toPath();
    var src = projectDir.resolve("src");
    var srcDataClasses = projectDir.resolve("src_dataClasses");
    mojo.cleanupIncludes = new String[] {"src"};
    assertThat(srcDataClasses).exists();
    assertThat(src).exists();

    mojo.execute();

    assertThat(src)
        .as("expected to be deleted, because of cleanupIncludes")
        .doesNotExist();
    assertThat(srcDataClasses).doesNotExist();
  }
}
