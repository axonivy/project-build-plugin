package ch.ivyteam.ivy.maven.generate;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Rule;
import org.junit.Test;

import ch.ivyteam.ivy.maven.compile.LocalRepoMojoRule;
import ch.ivyteam.ivy.maven.util.PathUtils;

public class TestGenerateDialogFormSourcesMojo {

  @Rule
  public LocalRepoMojoRule<GenerateDialogFormSourcesMojo> generate = new LocalRepoMojoRule<>(GenerateDialogFormSourcesMojo.GOAL);

  @Test
  public void generateFormSources() throws Exception {
    var targetSrcHd = generate.project.getBasedir().toPath().resolve("target").resolve("src_hd");
    PathUtils.delete(targetSrcHd);

    assertThat(targetSrcHd).doesNotExist();

    generate.getMojo().execute();

    assertThat(targetSrcHd)
        .isDirectoryRecursivelyContaining(f -> f.getFileName().toString().endsWith("myForm.xhtml"));
  }

  @Test
  public void skipGenerateSources() throws Exception {
    var targetSrcHd = generate.project.getBasedir().toPath().resolve("target").resolve("src_hd");
    PathUtils.delete(targetSrcHd);

    assertThat(targetSrcHd).doesNotExist();

    generate.getMojo().skipGenerateSources = true;
    generate.getMojo().execute();

    assertThat(targetSrcHd).doesNotExist();
  }
}
