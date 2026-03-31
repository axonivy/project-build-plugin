/*
 * Copyright (C) 2021 Axon Ivy AG
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package ch.ivyteam.ivy.maven.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.api.di.Provides;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import ch.ivyteam.ivy.maven.BaseEngineProjectMojoTest;
import ch.ivyteam.ivy.maven.InstallEngineMojo;
import ch.ivyteam.ivy.maven.engine.MavenProjectBuilderProxy;
import ch.ivyteam.ivy.maven.extension.ProjectExtension;
import ch.ivyteam.ivy.maven.test.SetupIvyTestPropertiesMojo.Property;
import ch.ivyteam.ivy.maven.test.bpm.IvyTestRuntime;
import ch.ivyteam.ivy.maven.test.bpm.IvyTestRuntime.Key;
import ch.ivyteam.ivy.maven.util.ClasspathJar;
import ch.ivyteam.ivy.maven.util.CompilerResult;
import ch.ivyteam.ivy.maven.util.SharedFile;

@MojoTest
@ExtendWith(ProjectExtension.class)
class TestSetupIvyTestPropertiesMojo {

  private SetupIvyTestPropertiesMojo mojo;

  @BeforeEach
  @InjectMojo(goal = InstallEngineMojo.GOAL)
  void setUpEngine(InstallEngineMojo install) throws Exception {
    BaseEngineProjectMojoTest.provideEngine(install);
  }

  @BeforeEach
  @InjectMojo(goal = SetupIvyTestPropertiesMojo.GOAL)
  void setUp(SetupIvyTestPropertiesMojo testProps) throws Exception {
    this.mojo = testProps;
    BaseEngineProjectMojoTest.provideEngine(testProps);
    prepareJar(mojo);
  }

  @Provides
  MavenProject provideMockedComponent() throws IOException {
    return ProjectExtension.project();
  }

  @Test
  void engineClasspathIsSharedAsProperty() throws Exception {
    assertThat(getProperty(Property.IVY_ENGINE_CLASSPATH))
        .as("used classpath has not been evaluated.")
        .isNullOrEmpty();
    assertThat(getProperty(Property.MAVEN_TEST_ARGLINE)).isNullOrEmpty();

    mojo.execute();
    assertThat(getProperty(Property.IVY_ENGINE_CLASSPATH))
        .as("used classpath must be shared as property so that other mojos can access it")
        .isNotEmpty();
  }

  @Test
  void engineModuleHintsSharedAsProperty() throws Exception {
    assertThat(getProperty(Property.MAVEN_TEST_ARGLINE)).isNullOrEmpty();

    mojo.execute();
    assertThat(getProperty(Property.MAVEN_TEST_ARGLINE)).contains(" --add-opens=");
  }

  private String getProperty(String key) {
    return (String) mojo.project.getProperties().get(key);
  }

  @Test
  void engineClasspathIsConfiguredForSurefire() throws Exception {
    mojo.execute();

    MavenProject project = mojo.project;
    assertThat(project.getBuild().getTestOutputDirectory())
        .isEqualTo(project.getBasedir().toPath().resolve("classes-test").toAbsolutePath().toString());
    assertThat(project.getProperties().get(Property.MAVEN_TEST_ADDITIONAL_CLASSPATH))
        .isEqualTo("${" + Property.IVY_TEST_VM_RUNTIME + "},"
            + "${" + Property.IVY_ENGINE_CLASSPATH + "},"
            + "${" + Property.IVY_PROJECT_IAR_CLASSPATH + "}");
  }

  @Test
  void ivyTestRuntimeClasspathResource() throws Exception {
    mojo.execute();

    MavenProject project = mojo.project;
    String vmRtEntry = project.getProperties().getProperty(Property.IVY_TEST_VM_RUNTIME);
    Properties props = new Properties();
    try (var loader = new URLClassLoader(new URL[] {Path.of(vmRtEntry).toUri().toURL()}, null);
        var is = loader.getResourceAsStream(IvyTestRuntime.RUNTIME_PROPS_RESOURCE)) {
      props.load(is);
    }
    assertThat(props.getProperty(Key.PRODUCT_DIR)).isNotEmpty();
    assertThat(props.getProperty(Key.PROJECT_LOCATIONS))
        .isEqualTo("<" + mojo.project.getBasedir().toPath().toUri() + ">");
  }

  @Test
  void ivyTestRuntimeIO() throws IOException {
    var rt = new IvyTestRuntime();
    rt.setProductDir(Path.of("/tmp/myEngine"));
    var ivyTestVm = rt.store(mojo.project);
    assertThat(ivyTestVm.getParent().getFileName().toString()).isEqualTo("target");
  }

  private static void prepareJar(SetupIvyTestPropertiesMojo testProps) throws IOException {
    writeTestClasspathJar(testProps);
    writeTestCompileResult(testProps);
  }

  private static void writeTestClasspathJar(SetupIvyTestPropertiesMojo mojo) throws IOException {
    var classPathJar = new SharedFile(mojo.project).getEngineClasspathJar();
    new ClasspathJar(classPathJar).createFileEntries(List.of(
        Files.createTempFile("dummy", ".jar").toFile(),
        Files.createTempFile("dummy2", ".jar").toFile()));
  }

  private static void writeTestCompileResult(SetupIvyTestPropertiesMojo mojo) throws IOException {
    Map<String, Object> result = new HashMap<>();
    result.put(MavenProjectBuilderProxy.Result.TEST_OUTPUT_DIR, "classes-test");
    CompilerResult.store(result, mojo.project);
  }
}
