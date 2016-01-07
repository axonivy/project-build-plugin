/*
 * Copyright (C) 2016 AXON IVY AG
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

import static org.fest.assertions.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

import org.apache.maven.project.MavenProject;
import org.junit.Rule;
import org.junit.Test;

import ch.ivyteam.ivy.maven.engine.ClasspathJar;
import ch.ivyteam.ivy.maven.engine.EngineClassLoaderFactory;

public class TestSetupIvyTestPropertiesMojo
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
            .get(SetupIvyTestPropertiesMojo.IVY_ENGINE_CLASSPATH_PROPERTY);
  }

  @Test
  public void engineClasspathIsConfiguredForSurefire() throws Exception
  {
    rule.getMojo().execute();
    
    MavenProject project = rule.getMojo().project;
    assertThat(project.getBuild().getTestOutputDirectory())
      .isEqualTo(new File(project.getBasedir(), "classes").getAbsolutePath());
    assertThat(project.getProperties().get(SetupIvyTestPropertiesMojo.MAVEN_TEST_ADDITIONAL_CLASSPATH_PROPERTY))
      .isEqualTo("${"+SetupIvyTestPropertiesMojo.IVY_ENGINE_CLASSPATH_PROPERTY+"}, ${"+SetupIvyTestPropertiesMojo.IVY_PROJECT_IAR_CLASSPATH_PROPERTY+"}");
  }

  @Rule
  public ProjectMojoRule<SetupIvyTestPropertiesMojo> rule = new ProjectMojoRule<SetupIvyTestPropertiesMojo>(
          new File("src/test/resources/base"), SetupIvyTestPropertiesMojo.GOAL)
  {
    @Override
    protected void before() throws Throwable
    {
      super.before();
      writeTestClasspathJar();
    }

    private void writeTestClasspathJar() throws IOException
    {
      File classPathJar = new File(
              getMojo().project.getBuild().getDirectory(), 
              EngineClassLoaderFactory.IVY_ENGINE_CLASSPATH_JAR_NAME);
      new ClasspathJar(classPathJar).createFileEntries(Arrays.asList(
              Files.createTempFile("dummy", ".jar").toFile(),
              Files.createTempFile("dummy2", ".jar").toFile()));
    }
  };

}
