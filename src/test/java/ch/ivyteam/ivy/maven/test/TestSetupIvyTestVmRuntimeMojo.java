package ch.ivyteam.ivy.maven.test;

import static ch.ivyteam.ivy.maven.test.SetupIvyTestVmRuntimeMojo.IVY_TEST_VM_RUNTIME;
import static ch.ivyteam.ivy.maven.test.SetupIvyTestVmRuntimeMojo.MAVEN_TEST_ADDITIONAL_CLASSPATH;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.maven.api.di.Provides;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import ch.ivyteam.ivy.maven.extension.ProjectExtension;
import ch.ivyteam.ivy.maven.test.bpm.IvyTestRuntime;

@MojoTest
@ExtendWith(ProjectExtension.class)
class TestSetupIvyTestVmRuntimeMojo {

  SetupIvyTestVmRuntimeMojo mojo;

  @BeforeEach
  @InjectMojo(goal = SetupIvyTestVmRuntimeMojo.GOAL)
  void setUp(SetupIvyTestVmRuntimeMojo vmMojo) {
    this.mojo = vmMojo;
  }

  @Provides
  MavenProject provideMockedComponent() throws IOException {
    return ProjectExtension.project();
  }

  @Test
  void ivyTestVmRuntime() throws Exception {
    assertThat(getProperty(IVY_TEST_VM_RUNTIME)).isNull();
    assertThat(getProperty(MAVEN_TEST_ADDITIONAL_CLASSPATH)).isNull();

    mojo.execute();

    assertThat(getProperty(IVY_TEST_VM_RUNTIME)).endsWith("ivyTestVm");
    assertThat(getProperty(MAVEN_TEST_ADDITIONAL_CLASSPATH)).isEqualTo("${ivy.test.vm.runtime}");

    var propertyFile = Path.of(getProperty(IVY_TEST_VM_RUNTIME))
        .resolve(IvyTestRuntime.RUNTIME_PROPS_RESOURCE);
    assertThat(propertyFile).content()
        .contains("product.dir=")
        .contains("productDir")
        .contains("project.locations=<file\\:");
  }

  String getProperty(String key) {
    return (String) mojo.project.getProperties().get(key);
  }
}
