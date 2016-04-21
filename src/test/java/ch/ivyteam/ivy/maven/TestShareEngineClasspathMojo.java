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

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;

public class TestShareEngineClasspathMojo
{
  @Test
  public void engineClasspathIsSharedAsProperty() throws Exception
  {
    ShareEngineCoreClasspathMojo mojo = rule.getMojo();
    assertThat(getEngineClasspathProperty())
            .as("used classpath has not been evaluated.")
            .isNullOrEmpty();

    mojo.execute();

    assertThat(getEngineClasspathProperty())
            .as("used classpath must be shared as property so that other mojos can access it")
            .contains("dummy-ivy.jar");
  }

  private String getEngineClasspathProperty()
  {
    return (String) rule.getMojo().project.getProperties()
            .get(ShareEngineCoreClasspathMojo.IVY_ENGINE_CORE_CLASSPATH_PROPERTY);
  }

  @Rule
  public ProjectMojoRule<ShareEngineCoreClasspathMojo> rule = new ProjectMojoRule<ShareEngineCoreClasspathMojo>(
          new File("src/test/resources/base"), ShareEngineCoreClasspathMojo.GOAL)
    {
      @Override
      protected void before() throws Throwable
      {
        super.before();
        writeEngineLibDir();
      }

      private void writeEngineLibDir()
      {
        File engineDirectory = rule.getMojo().getEngineDirectory();
        try
        {
          FileUtils.touch(new File(engineDirectory, "lib/shared/dummy-shared.jar"));
          FileUtils.touch(new File(engineDirectory, "/lib/patch/dummy-patch.jar"));
          FileUtils.touch(new File(engineDirectory, "/lib/ivy/dummy-ivy.jar"));
        }
        catch (IOException ex)
        {
          throw new RuntimeException("Cannot create server jars", ex);
        }
        
      }
    };
}
