package ch.ivyteam.ivy.maven.test;

import static ch.ivyteam.ivy.maven.test.SetupIvyTestVmRuntimeMojo.IVY_TEST_VM_RUNTIME;
import static ch.ivyteam.ivy.maven.test.SetupIvyTestVmRuntimeMojo.MAVEN_TEST_ADDITIONAL_CLASSPATH;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.apache.maven.api.di.Provides;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.testing.ArtifactStubFactory;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import ch.ivyteam.ivy.maven.extension.ProjectExtension;
import ch.ivyteam.ivy.maven.project.ReactorSession;
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

  @Test
  void dependencyLocationUsesReactorIarAfterPackaging(@TempDir Path projectDir) throws IOException {
    Artifact artifact = new ArtifactStubFactory()
        .createArtifact("com.axonivy.test", "dependency", "1.0.0", "compile", "iar", "");
    var reactorProject = Mockito.mock(MavenProject.class);
    Mockito.when(reactorProject.getArtifact()).thenReturn(artifact);
    Mockito.when(reactorProject.getBasedir()).thenReturn(projectDir.toFile());

    var session = Mockito.mock(MavenSession.class);
    Mockito.when(session.getAllProjects()).thenReturn(List.of(reactorProject));
    var reactor = new ReactorSession(session);

    artifact.setFile(projectDir.resolve("target/classes").toFile());
    assertThat(SetupIvyTestVmRuntimeMojo.dependencyLocation(artifact, reactor)).isEqualTo(projectDir);

    var iar = projectDir.resolve("target/dependency-1.0.0.iar");
    artifact.setFile(iar.toFile());
    assertThat(SetupIvyTestVmRuntimeMojo.dependencyLocation(artifact, reactor)).isEqualTo(iar);
  }

  String getProperty(String key) {
    return (String) mojo.project.getProperties().get(key);
  }
}
