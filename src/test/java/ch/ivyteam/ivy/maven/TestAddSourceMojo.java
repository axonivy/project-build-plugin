package ch.ivyteam.ivy.maven;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import ch.ivyteam.ivy.maven.extension.ProjectExtension;

@MojoTest
@ExtendWith(ProjectExtension.class)
class TestAddSourceMojo {

  AddSourceMojo mojo;
  Path baseDir;

  @BeforeEach
  @InjectMojo(goal = AddSourceMojo.GOAL)
  void setUp(AddSourceMojo add) {
    this.mojo = add;
    var defaultSrc = mojo.project.getCompileSourceRoots().getFirst();
    mojo.project.removeCompileSourceRoot(defaultSrc);
    this.baseDir = mojo.project.getBasedir().toPath();
  }

  @Test
  void noSourceToAdd() throws Exception {
    assertThat(mojo.project.getCompileSourceRoots()).isEmpty();
    mojo.execute();
    assertThat(mojo.project.getCompileSourceRoots()).isEmpty();
  }

  @Test
  void addSources() throws Exception {
    Files.createDirectories(baseDir.resolve("src_generated/ws/client1/ch/ivy"));
    Files.createDirectories(baseDir.resolve("src_generated/ws/client2"));
    Files.createDirectories(baseDir.resolve("src_generated/rest/client1"));
    Files.createDirectories(baseDir.resolve("src_generated/unknown/client1"));
    assertThat(mojo.project.getCompileSourceRoots()).isEmpty();
    mojo.execute();
    var sourceDirectories = mojo.project.getCompileSourceRoots();
    assertThat(sourceDirectories).containsExactlyInAnyOrder(
        baseDir.resolve("src_generated/ws/client1").toString(),
        baseDir.resolve("src_generated/ws/client2").toString(),
        baseDir.resolve("src_generated/rest/client1").toString());
  }

  @Test
  void skip() throws Exception {
    Files.createDirectories(baseDir.resolve("src_generated/ws/client1"));
    assertThat(mojo.project.getCompileSourceRoots()).isEmpty();
    mojo.skipIvyAddSource = true;
    mojo.execute();
    assertThat(mojo.project.getCompileSourceRoots()).isEmpty();
  }
}
