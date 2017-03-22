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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.apache.commons.exec.Executor;
import org.apache.commons.exec.ShutdownHookProcessDestroyer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import ch.ivyteam.ivy.maven.engine.EngineControl;

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
    
    Executor startedProcess = null;
    try
    {
      startedProcess = mojo.startEngine();
      assertThat(getProperty(EngineControl.Property.TEST_ENGINE_URL)).startsWith("http://");
      assertThat(new File(getProperty(EngineControl.Property.TEST_ENGINE_LOG))).exists();
    }
    finally
    {
      kill(startedProcess);
    }
  }
  
  @Test @Ignore
  public void engineStartCanFailFast() throws Exception
  {
    StartTestEngineMojo mojo = rule.getMojo();
    File engineDir = installUpToDateEngineRule.getMojo().getRawEngineDirectory();
    File configDir = new File(engineDir, "configuration");
    File tmpConfigDir = new File(engineDir, "config.bkp");
    configDir.renameTo(tmpConfigDir);
    
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();
    Executor startedProcess = null;
    try
    {
      startedProcess = mojo.startEngine();
      fail("Engine start should fail as no configuration directory exists.");
    }
    catch (RuntimeException ex)
    {
      stopWatch.stop();
      long seconds = TimeUnit.SECONDS.convert(stopWatch.getTime(), TimeUnit.MILLISECONDS);
      assertThat(seconds)
        .describedAs("engine start should fail early if engine config is incomplete")
        .isLessThanOrEqualTo(20);
    }
    finally
    {
      kill(startedProcess);
      FileUtils.deleteDirectory(configDir);
      tmpConfigDir.renameTo(configDir);
    }
  }

  @Test
  public void testKillEngineOnVmExit() throws Exception
  {
    StartTestEngineMojo mojo = rule.getMojo();
    Executor startedProcess = null;
    try
    {
      startedProcess = mojo.startEngine();
      assertThat(startedProcess.getProcessDestroyer()).isInstanceOf(ShutdownHookProcessDestroyer.class);
      ShutdownHookProcessDestroyer jvmShutdownHoock = (ShutdownHookProcessDestroyer) startedProcess.getProcessDestroyer();
      assertThat(jvmShutdownHoock.size())
        .as("One started engine process must be killed on VM end.")
        .isEqualTo(1);
    }
    finally
    {
      kill(startedProcess);
    }
  }

  private static void kill(Executor startedProcess)
  {
    if (startedProcess != null)
    {
      startedProcess.getWatchdog().destroyProcess();
    }
  }
  
  private String getProperty(String name)
  {
    return (String)rule.project.getProperties().get(name);
  }

  @Rule
  public RunnableEngineMojoRule<StartTestEngineMojo> rule = 
    new RunnableEngineMojoRule<StartTestEngineMojo>(StartTestEngineMojo.GOAL){
    @Override
    protected void before() throws Throwable {
      super.before();
      getMojo().maxmem = "2048m";
    }
  };

}
