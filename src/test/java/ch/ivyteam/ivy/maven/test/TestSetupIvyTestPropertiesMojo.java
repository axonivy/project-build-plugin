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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.project.MavenProject;
import org.junit.Rule;
import org.junit.Test;

import ch.ivyteam.ivy.maven.BaseEngineProjectMojoTest;
import ch.ivyteam.ivy.maven.ProjectMojoRule;
import ch.ivyteam.ivy.maven.engine.MavenProjectBuilderProxy;
import ch.ivyteam.ivy.maven.test.SetupIvyTestPropertiesMojo.Property;
import ch.ivyteam.ivy.maven.test.bpm.IvyTestRuntime;
import ch.ivyteam.ivy.maven.test.bpm.IvyTestRuntime.Key;
import ch.ivyteam.ivy.maven.util.ClasspathJar;
import ch.ivyteam.ivy.maven.util.CompilerResult;
import ch.ivyteam.ivy.maven.util.SharedFile;

public class TestSetupIvyTestPropertiesMojo extends BaseEngineProjectMojoTest {
  @Test
  public void engineClasspathIsSharedAsProperty() throws Exception {
    SetupIvyTestPropertiesMojo mojo = rule.getMojo();
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
  public void engineModuleHintsSharedAsProperty() throws Exception {
    SetupIvyTestPropertiesMojo mojo = rule.getMojo();
    assertThat(getProperty(Property.MAVEN_TEST_ARGLINE)).isNullOrEmpty();

    mojo.execute();

    assertThat(getProperty(Property.MAVEN_TEST_ARGLINE)).contains(" --add-opens ");
  }

  private String getProperty(String key) {
    return (String) rule.getMojo().project.getProperties().get(key);
  }

  @Test
  public void engineClasspathIsConfiguredForSurefire() throws Exception {
    rule.getMojo().execute();

    MavenProject project = rule.getMojo().project;
    assertThat(project.getBuild().getTestOutputDirectory())
            .isEqualTo(new File(project.getBasedir(), "classes-test").getAbsolutePath());
    assertThat(project.getProperties().get(Property.MAVEN_TEST_ADDITIONAL_CLASSPATH))
            .isEqualTo("${" + Property.IVY_TEST_VM_RUNTIME + "},"
                    + "${" + Property.IVY_ENGINE_CLASSPATH + "},"
                    + "${" + Property.IVY_PROJECT_IAR_CLASSPATH + "}");
  }

  @Test
  public void ivyTestRuntimeClasspathResource() throws Exception {
    rule.getMojo().execute();

    MavenProject project = rule.getMojo().project;
    String vmRtEntry = project.getProperties().getProperty(Property.IVY_TEST_VM_RUNTIME);
    Properties props = new Properties();
    try (
            URLClassLoader loader = new URLClassLoader(new URL[] {new File(vmRtEntry).toURI().toURL()}, null);
            InputStream is = loader.getResourceAsStream(IvyTestRuntime.RUNTIME_PROPS_RESOURCE)) {
      props.load(is);
    }
    assertThat(props.getProperty(Key.PRODUCT_DIR)).isNotEmpty();
    assertThat(props.getProperty(Key.PROJECT_LOCATIONS))
            .isEqualTo("<" + rule.getMojo().project.getBasedir().toURI() + ">");
  }

  @Test
  public void ivyTestRuntimeIO() throws IOException {
    IvyTestRuntime rt = new IvyTestRuntime();
    rt.setProductDir(new File("/tmp/myEngine"));
    File ivyTestVm = rt.store(rule.project);
    assertThat(ivyTestVm.getParentFile().getName()).isEqualTo("target");
  }

  @Rule
  public ProjectMojoRule<SetupIvyTestPropertiesMojo> rule = new TestPropertyMojoRule();

  private static class TestPropertyMojoRule extends EngineMojoRule<SetupIvyTestPropertiesMojo> {
    private TestPropertyMojoRule() {
      super(SetupIvyTestPropertiesMojo.GOAL);
    }

    @Override
    protected void before() throws Throwable {
      super.before();
      writeTestClasspathJar();
      writeTestCompileResult();
    }

    private void writeTestClasspathJar() throws IOException {
      File classPathJar = new SharedFile(getMojo().project).getEngineClasspathJar();
      new ClasspathJar(classPathJar).createFileEntries(Arrays.asList(
              Files.createTempFile("dummy", ".jar").toFile(),
              Files.createTempFile("dummy2", ".jar").toFile()));
    }

    private void writeTestCompileResult() throws IOException {
      Map<String, Object> result = new HashMap<>();
      result.put(MavenProjectBuilderProxy.Result.TEST_OUTPUT_DIR, "classes-test");
      CompilerResult.store(result, getMojo().project);
    }
  }
}
