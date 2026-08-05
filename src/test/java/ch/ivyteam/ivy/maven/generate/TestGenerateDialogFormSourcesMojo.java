package ch.ivyteam.ivy.maven.generate;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import ch.ivyteam.ivy.maven.extension.ProjectExtension;
import ch.ivyteam.ivy.maven.util.PathUtils;

@MojoTest
@ExtendWith(ProjectExtension.class)
class TestGenerateDialogFormSourcesMojo {

  private GenerateDialogFormSourcesMojo mojo;

  @BeforeEach
  @InjectMojo(goal = GenerateDialogFormSourcesMojo.GOAL)
  void setUp(GenerateDialogFormSourcesMojo form) {
    this.mojo = form;
  }

  @Test
  void generateFormSources() {
    var targetDialog = mojo.project.getBasedir().toPath().resolve("target").resolve("dialog");
    PathUtils.delete(targetDialog);

    assertThat(targetDialog).doesNotExist();

    mojo.execute();

    assertThat(targetDialog)
        .isDirectoryRecursivelyContaining(f -> f.getFileName().toString().endsWith("myForm.xhtml"))
        .isDirectoryRecursivelyContaining(f -> f.getFileName().toString().startsWith("dialog"));
  }

  @Test
  void skipGenerateSources() {
    var targetDialog = mojo.project.getBasedir().toPath().resolve("target").resolve("dialog");
    PathUtils.delete(targetDialog);

    assertThat(targetDialog).doesNotExist();

    mojo.skipGenerateSources = true;
    mojo.execute();

    assertThat(targetDialog).doesNotExist();
  }
}
