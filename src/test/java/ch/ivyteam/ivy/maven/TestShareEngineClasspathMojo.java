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

package ch.ivyteam.ivy.maven;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Rule;
import org.junit.Test;

import ch.ivyteam.ivy.maven.engine.EngineClassLoaderFactory.OsgiDir;

public class TestShareEngineClasspathMojo {
  @Test
  public void engineClasspathIsSharedAsProperty() throws Exception {
    ShareEngineCoreClasspathMojo mojo = rule.getMojo();
    assertThat(getEngineClasspathProperty())
            .as("used classpath has not been evaluated.")
            .isNullOrEmpty();

    mojo.execute();

    assertThat(getEngineClasspathProperty())
            .as("used classpath must be shared as property so that other mojos can access it")
            .contains("dummy-boot.jar");
  }

  private String getEngineClasspathProperty() {
    return (String) rule.getMojo().project.getProperties()
            .get(ShareEngineCoreClasspathMojo.IVY_ENGINE_CORE_CLASSPATH_PROPERTY);
  }

  @Rule
  public ProjectMojoRule<ShareEngineCoreClasspathMojo> rule = new ProjectMojoRule<ShareEngineCoreClasspathMojo>(
          new File("src/test/resources/base"), ShareEngineCoreClasspathMojo.GOAL) {
    @Override
    protected void before() throws Throwable {
      super.before();
      configureMojo(getMojo());
      writeEngineLibDir();
    }

    protected void configureMojo(AbstractEngineMojo newMojo) throws IOException {
      var engineDir = Files.createTempDirectory("tmpEngineDir");
      newMojo.engineDirectory = engineDir;
    }

    private void writeEngineLibDir() {
      try {
        var engineDirectory = rule.getMojo().identifyAndGetEngineDirectory();
        FileUtils.touch(
                new File(engineDirectory.toFile(), OsgiDir.INSTALL_AREA + "/" + OsgiDir.LIB_BOOT + "/dummy-boot.jar"));
      } catch (IOException | MojoExecutionException ex) {
        throw new RuntimeException("Cannot create server jars", ex);
      }

    }
  };
}
