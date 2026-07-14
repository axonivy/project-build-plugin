package ch.ivyteam.ivy.maven;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.model.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import ch.ivyteam.ivy.maven.extension.ProjectExtension;

@MojoTest
@ExtendWith(ProjectExtension.class)
class TestAddResourceMojo {

  AddResourceMojo mojo;
  Path baseDir;

  @BeforeEach
  @InjectMojo(goal = AddResourceMojo.GOAL)
  void setUp(AddResourceMojo add) {
    this.mojo = add;
    mojo.project.getBuild().setResources(new ArrayList<>());
    this.baseDir = mojo.project.getBasedir().toPath();
  }

  @Test
  void noResourceToAdd() throws Exception {
    assertThat(mojo.project.getResources()).isEmpty();
    mojo.execute();
    assertThat(mojo.project.getResources()).isEmpty();
  }

  @Test
  void addResources() throws Exception {
    Files.createDirectories(baseDir.resolve("src_generated/ws/client1/ch/ivy"));
    Files.createDirectories(baseDir.resolve("src_generated/ws/client2"));
    Files.createDirectories(baseDir.resolve("src_generated/rest/client1"));
    Files.createDirectories(baseDir.resolve("src_generated/unknown/client1"));
    assertThat(mojo.project.getResources()).isEmpty();
    mojo.execute();
    var resourceDirectories = mojo.project.getResources().stream().map(Resource::getDirectory);
    assertThat(resourceDirectories).containsExactlyInAnyOrder(
        baseDir.resolve("src_generated/ws/client1").toString(),
        baseDir.resolve("src_generated/ws/client2").toString(),
        baseDir.resolve("src_generated/rest/client1").toString());
    assertThat(mojo.project.getResources().getFirst().getExcludes()).containsExactly("**/*.java");
  }

  @Test
  void skip() throws Exception {
    Files.createDirectories(baseDir.resolve("src_generated/ws/client1"));
    assertThat(mojo.project.getResources()).isEmpty();
    mojo.skipIvyAddResource = true;
    mojo.execute();
    assertThat(mojo.project.getResources()).isEmpty();
  }
}
