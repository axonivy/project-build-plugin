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
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.StopWatch;
import org.fest.assertions.api.Assertions;
import org.junit.Rule;
import org.junit.Test;

import ch.ivyteam.ivy.maven.engine.EngineClassLoaderFactory;
import ch.ivyteam.ivy.maven.engine.EngineControl;
import ch.ivyteam.ivy.maven.util.ClasspathJar;
import ch.ivyteam.ivy.maven.util.SharedFile;

/**
 * @since 6.1.1
 */
public class TestStartEngine extends BaseEngineProjectMojoTest
{
  
  @Test
  public void canStartEngine() throws Exception
  {
    StartTestEngineMojo mojo = rule.getMojo();
    assertThat(getProperty(EngineControl.Property.TEST_ENGINE_URL)).isNull();
    assertThat(getProperty(EngineControl.Property.TEST_ENGINE_LOG)).isNull();
    
    Process startedProcess = null;
    try
    {
      startedProcess = mojo.startEngine();
      assertThat(startedProcess.isAlive()).isTrue();
      
      assertThat(getProperty(EngineControl.Property.TEST_ENGINE_URL)).startsWith("http://");
      assertThat(new File(getProperty(EngineControl.Property.TEST_ENGINE_LOG))).exists();
    }
    finally
    {
      if (startedProcess != null)
      {
        startedProcess.destroy();
      }
    }
  }
  
  @Test
  public void engineStartCanFailFast() throws Exception
  {
    StartTestEngineMojo mojo = rule.getMojo();
    File engineDir = installUpToDateEngineRule.getMojo().getEngineDirectory();
    File configDir = new File(engineDir, "configuration");
    File tmpConfigDir = new File(engineDir, "config.bkp");
    configDir.renameTo(tmpConfigDir);
    
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();
    Process startedProcess = null;
    try
    {
      startedProcess = mojo.startEngine();
      Assertions.fail("Engine start should fail as no configuration directory exists.");
    }
    catch (RuntimeException ex)
    {
      stopWatch.stop();
      long seconds = TimeUnit.SECONDS.convert(stopWatch.getTime(), TimeUnit.MILLISECONDS);
      assertThat(seconds)
        .as("engine start should fail early if engine config is incomplete")
        .isLessThanOrEqualTo(20);
    }
    finally
    {
      tmpConfigDir.renameTo(configDir);
      if (startedProcess != null)
      {
        startedProcess.destroy();
      }
    }
  }

  private String getProperty(String name)
  {
    return (String)rule.project.getProperties().get(name);
  }

  @Rule
  public EngineMojoRule<StartTestEngineMojo> rule = 
    new EngineMojoRule<StartTestEngineMojo>(StartTestEngineMojo.GOAL){

    @Override
    protected void before() throws Throwable
    {
      super.before();
      provideClasspathJar();
    }
    
    private void provideClasspathJar() throws IOException
    {
      File cpJar = new SharedFile(rule.project).getEngineClasspathJar();
      new ClasspathJar(cpJar).createFileEntries(EngineClassLoaderFactory
              .getIvyEngineClassPathFiles(installUpToDateEngineRule.getMojo().getEngineDirectory()));
    }
  };

}
