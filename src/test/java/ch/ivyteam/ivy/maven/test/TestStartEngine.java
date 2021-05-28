/*
 * Copyright (C) 2021 Axon Ivy AG
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

package ch.ivyteam.ivy.maven.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.apache.commons.exec.Executor;
import org.apache.commons.exec.ShutdownHookProcessDestroyer;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.io.Files;

import ch.ivyteam.ivy.maven.BaseEngineProjectMojoTest;
import ch.ivyteam.ivy.maven.engine.EngineControl;
import ch.ivyteam.ivy.maven.log.LogCollector;
import ch.ivyteam.ivy.maven.test.AbstractIntegrationTestMojo.TestEngineLocation;

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
      assertThat(getProperty(EngineControl.Property.TEST_ENGINE_URL)).startsWith("http://")
              .endsWith("/");
      assertThat(new File(getProperty(EngineControl.Property.TEST_ENGINE_LOG))).exists();
    }
    finally
    {
      kill(startedProcess);
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

  @Test
  public void startEngine_copiedEngine_executable() throws Exception
  {
    StartTestEngineMojo mojo = rule.getMojo();
    mojo.testEngine = TestEngineLocation.COPY_FROM_TEMPLATE;
    Executor startedProcess = null;
    try
    {
      File cacheEngine = mojo.engineDirectory;
      assertFileExecutable(new File(cacheEngine, "elasticsearch/bin/elasticsearch"));
      assertFileExecutable(new File(cacheEngine, "elasticsearch/bin/elasticsearch.bat"));

      startedProcess = mojo.startEngine();
      
      File engineTarget = mojo.getEngineDir(mojo.project);
      assertFileExecutable(new File(engineTarget, "elasticsearch/bin/elasticsearch"));
      assertFileExecutable(new File(engineTarget, "elasticsearch/bin/elasticsearch.bat"));
    }
    finally
    {
      kill(startedProcess);
    }
  }

  private void assertFileExecutable(File file)
  {
    assertThat(file).exists();
    assertThat(file.canExecute()).isTrue();
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
