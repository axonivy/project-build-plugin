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
import static org.assertj.core.api.Assertions.fail;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.apache.commons.exec.Executor;
import org.apache.commons.exec.ShutdownHookProcessDestroyer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.io.Files;

import ch.ivyteam.ivy.maven.AbstractIntegrationTestMojo.TestEngineLocation;
import ch.ivyteam.ivy.maven.engine.EngineControl;
import ch.ivyteam.ivy.maven.log.LogCollector;

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

  /**
   * MODIFY_EXISTING
   * 1. If engine {@link ch.ivyteam.ivy.maven.AbstractEngineMojo#engineDirectory} exists -> do not copy
   * 2. If engine {@link ch.ivyteam.ivy.maven.AbstractEngineMojo#engineCacheDirectory} exists -> do not copy
   */
  @Test
  public void startEngine_MODIFY_EXISTING_configuredEngine() throws MojoExecutionException
  {
    StartTestEngineMojo mojo = rule.getMojo();
    mojo.testEngine = TestEngineLocation.MODIFY_EXISTING;
    mojo.engineDirectory = Files.createTempDir();
    assertThat(mojo.engineToTarget()).as("MODIFY_EXISTING set and using configured engine do not copy").isFalse();
  }

  @Test
  public void startEngine_MODIFY_EXISTING_cacheEngine() throws MojoExecutionException
  {
    StartTestEngineMojo mojo = rule.getMojo();
    mojo.testEngine = TestEngineLocation.MODIFY_EXISTING;
    assertThat(mojo.engineToTarget()).as("MODIFY_EXISTING set and using cached engine do not copy").isFalse();
  }

  /**
   * COPY_FROM_TEMPLATE
   * 1. If engine {@link ch.ivyteam.ivy.maven.AbstractEngineMojo#engineDirectory} exists -> do copy
   * 2. If engine {@link ch.ivyteam.ivy.maven.AbstractEngineMojo#engineCacheDirectory} exists -> do copy
   */
  @Test
  public void startEngine_COPY_FROM_TEMPLATE_configuredEngine() throws MojoExecutionException
  {
    StartTestEngineMojo mojo = rule.getMojo();
    mojo.testEngine = TestEngineLocation.COPY_FROM_TEMPLATE;
    mojo.engineDirectory = Files.createTempDir();
    assertThat(mojo.engineToTarget()).as("COPY_FROM_TEMPLATE set and using configured engine do copy").isTrue();
  }

  @Test
  public void startEngine_COPY_FROM_TEMPLATE_cacheEngine() throws MojoExecutionException
  {
    StartTestEngineMojo mojo = rule.getMojo();
    mojo.testEngine = TestEngineLocation.COPY_FROM_TEMPLATE;
    assertThat(mojo.engineToTarget()).as("COPY_FROM_TEMPLATE set and using cached engine do copy").isTrue();
  }

  /**
   * COPY_FROM_CACHE
   * 1. If engine {@link ch.ivyteam.ivy.maven.AbstractEngineMojo#engineDirectory} exists -> do not copy
   * 2. If engine {@link ch.ivyteam.ivy.maven.AbstractEngineMojo#engineCacheDirectory} exists -> do copy
   */
  @Test
  public void startEngine_COPY_FROM_CACHE_configuredEngine() throws MojoExecutionException
  {
    StartTestEngineMojo mojo = rule.getMojo();
    mojo.testEngine = TestEngineLocation.COPY_FROM_CACHE;
    mojo.engineDirectory = Files.createTempDir();
    assertThat(mojo.engineToTarget()).as("COPY_FROM_CACHE set and using configured engine do not copy").isFalse();
  }

  @Test
  public void startEngine_COPY_FROM_CACHE_cacheEngine() throws MojoExecutionException
  {
    StartTestEngineMojo mojo = rule.getMojo();
    mojo.testEngine = TestEngineLocation.COPY_FROM_CACHE;
    assertThat(mojo.engineToTarget()).as("COPY_FROM_CACHE set and using cached engine do copy").isTrue();
  }

  @Test
  public void startEngine_copyEngineToTarget() throws Exception
  {
    StartTestEngineMojo mojo = rule.getMojo();
    Executor startedProcess = null;
    try
    {
      mojo.testEngine = TestEngineLocation.COPY_FROM_TEMPLATE;
      File engineDirTarget = mojo.getEngineDir(mojo.project);
      assertThat(engineDirTarget.toString()).contains("/target/ivyEngine");
      
      assertThat(engineDirTarget).doesNotExist();
      startedProcess = mojo.startEngine();
      assertThat(engineDirTarget).exists();
    }
    finally
    {
      kill(startedProcess);
    }
  }
  
  @Test
  public void startEngine_targetDirectoryNotClean() throws Exception
  {
    LogCollector log = new LogCollector();
    StartTestEngineMojo mojo = rule.getMojo();
    mojo.setLog(log);
    
    Executor startedProcess = null;
    try
    {
      File engineDirTarget = mojo.getEngineDir(mojo.project);
      assertThat(engineDirTarget.toString()).contains("/target/ivyEngine");
      
      assertThat(engineDirTarget).doesNotExist();
      assertThat(log.getWarnings().toString()).doesNotContain("Skipping copy");

      startedProcess = mojo.startEngine();
      assertThat(engineDirTarget).exists();
      assertThat(log.getWarnings().toString()).doesNotContain("Skipping copy");
      
      kill(startedProcess);
      startedProcess = mojo.startEngine();
      assertThat(engineDirTarget).exists();
      assertThat(log.getWarnings().toString()).contains("Skipping copy");
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
