/*
 * Copyright (C) 2018 AXON Ivy AG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.ivyteam.ivy.maven;

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

import ch.ivyteam.ivy.maven.SetupIvyTestPropertiesMojo.Property;
import ch.ivyteam.ivy.maven.bpm.test.IvyTestRuntime;
import ch.ivyteam.ivy.maven.bpm.test.IvyTestRuntime.Key;
import ch.ivyteam.ivy.maven.engine.MavenProjectBuilderProxy;
import ch.ivyteam.ivy.maven.util.ClasspathJar;
import ch.ivyteam.ivy.maven.util.CompilerResult;
import ch.ivyteam.ivy.maven.util.SharedFile;

public class TestSetupIvyTestPropertiesMojo extends BaseEngineProjectMojoTest
{
  @Test
  public void engineClasspathIsSharedAsProperty() throws Exception
  {
    SetupIvyTestPropertiesMojo mojo = rule.getMojo();
    assertThat(getEngineClasspathProperty())
      .as("used classpath has not been evaluated.")
      .isNullOrEmpty();
    
    mojo.execute();
    
    assertThat(getEngineClasspathProperty())
      .as("used classpath must be shared as property so that other mojos can access it")
      .isNotEmpty();
  }

  private String getEngineClasspathProperty()
  {
    return (String)rule.getMojo().project.getProperties()
            .get(Property.IVY_ENGINE_CLASSPATH);
  }

  @Test
  public void engineClasspathIsConfiguredForSurefire() throws Exception
  {
    rule.getMojo().execute();
    
    MavenProject project = rule.getMojo().project;
    assertThat(project.getBuild().getTestOutputDirectory())
      .isEqualTo(new File(project.getBasedir(), "classes-test").getAbsolutePath());
    assertThat(project.getProperties().get(Property.MAVEN_TEST_ADDITIONAL_CLASSPATH))
      .isEqualTo("${"+Property.IVY_TEST_VM_RUNTIME+"},"
              + "${"+Property.IVY_ENGINE_CLASSPATH+"},"
              + "${"+Property.IVY_PROJECT_IAR_CLASSPATH+"}");
  }
  
  @Test
  public void ivyTestRuntimeClasspathResource() throws Exception
  {
    rule.getMojo().execute();
    
    MavenProject project = rule.getMojo().project;
    String vmRtEntry = project.getProperties().getProperty(Property.IVY_TEST_VM_RUNTIME);
    Properties props = new Properties();
    try(
      URLClassLoader loader = new URLClassLoader(new URL[]{new File(vmRtEntry).toURI().toURL()}, null);
      InputStream is = loader.getResourceAsStream(IvyTestRuntime.RUNTIME_PROPS_RESOURCE)
    )
    {
      props.load(is);
    }
    assertThat(props.getProperty(Key.PRODUCT_DIR)).isNotEmpty();
    assertThat(props.getProperty(Key.PROJECT_LOCATIONS))
      .isEqualTo("<"+rule.getMojo().project.getBasedir().toURI()+">");
  }

  @Rule
  public ProjectMojoRule<SetupIvyTestPropertiesMojo> rule = new TestPropertyMojoRule();

  private static class TestPropertyMojoRule extends EngineMojoRule<SetupIvyTestPropertiesMojo>
  {
    private TestPropertyMojoRule()
    {
      super(SetupIvyTestPropertiesMojo.GOAL);
    }
    
    @Override
    protected void before() throws Throwable
    {
      super.before();
      writeTestClasspathJar();
      writeTestCompileResult();
    }

    private void writeTestClasspathJar() throws IOException
    {
      File classPathJar = new SharedFile(getMojo().project).getEngineClasspathJar();
      new ClasspathJar(classPathJar).createFileEntries(Arrays.asList(
              Files.createTempFile("dummy", ".jar").toFile(),
              Files.createTempFile("dummy2", ".jar").toFile()));
    }
    
    private void writeTestCompileResult() throws IOException
    {
      Map<String, Object> result = new HashMap<>();
      result.put(MavenProjectBuilderProxy.Result.TEST_OUTPUT_DIR, "classes-test");
      CompilerResult.store(result, getMojo().project);
    }
  }
}
