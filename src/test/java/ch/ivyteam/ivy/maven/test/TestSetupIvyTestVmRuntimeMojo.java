package ch.ivyteam.ivy.maven.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.Rule;
import org.junit.Test;

import ch.ivyteam.ivy.maven.ProjectMojoRule;
import ch.ivyteam.ivy.maven.test.SetupIvyTestPropertiesMojo.Property;
import ch.ivyteam.ivy.maven.test.bpm.IvyTestRuntime;

public class TestSetupIvyTestVmRuntimeMojo {

  @Rule
  public ProjectMojoRule<SetupIvyTestVmRuntimeMojo> rule = new ProjectMojoRule<>(
      Path.of("src/test/resources/base"), SetupIvyTestVmRuntimeMojo.GOAL);

  @Test
  public void ivyTestVmRuntime() throws Exception {
    assertThat(getProperty(Property.IVY_TEST_VM_RUNTIME)).isNull();
    assertThat(getProperty(Property.MAVEN_TEST_ADDITIONAL_CLASSPATH)).isNull();

    rule.getMojo().execute();

    assertThat(getProperty(Property.IVY_TEST_VM_RUNTIME)).endsWith("ivyTestVm");
    assertThat(getProperty(Property.MAVEN_TEST_ADDITIONAL_CLASSPATH)).isEqualTo("${ivy.test.vm.runtime}");

    var propertyFile = Path.of(getProperty(Property.IVY_TEST_VM_RUNTIME))
        .resolve(IvyTestRuntime.RUNTIME_PROPS_RESOURCE);
    assertThat(propertyFile).content()
        .contains("product.dir=")
        .contains("productDir")
        .contains("project.locations=<file\\:");
  }

  private String getProperty(String key) {
    return (String) rule.getMojo().project.getProperties().get(key);
  }
}
