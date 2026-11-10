package ch.ivyteam.ivy.maven.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import ch.ivyteam.ivy.maven.extension.ProjectExtension;

@MojoTest
@ExtendWith(ProjectExtension.class)
public class TestComponentsMojo {

  private ComponentsMojo mojo;

  @BeforeEach
  @InjectMojo(goal = ComponentsMojo.GOAL)
  void beforeEach(ComponentsMojo components) {
    this.mojo = components;
    this.mojo.loader = testLoader();
  }

  public static Function<String, Type> testLoader() {
    return typeName -> {
      try {
        return TestComponentsMojo.class.getClassLoader().loadClass(typeName);
      } catch (ClassNotFoundException ex) {
        throw new RuntimeException("Failed to load " + typeName, ex);
      }
    };
  }

  @Test
  void execute() throws Exception {
    mojo.skipComponentsReport = false;
    mojo.execute();

    Path components = mojo.componentReportDir.resolve("process.json");
    String jsonRaw = Files.readString(components);
    assertThat(jsonRaw)
        .contains("elements");
  }

  @Test
  void processReader() {
    Path base = mojo.mavenProject.getBasedir().toPath();
    var reader = new ProcessReader(base);
    var name = reader.nameOf(base.resolve("process/mySpace/myProcess.p.json"));
    assertThat(name.getSimpleName()).isEqualTo("myProcess");
    assertThat(name.getPath()).isEqualTo("mySpace");
  }
}
