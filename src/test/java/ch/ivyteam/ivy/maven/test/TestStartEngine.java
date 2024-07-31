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
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Rule;
import org.junit.Test;

import ch.ivyteam.ivy.maven.BaseEngineProjectMojoTest;
import ch.ivyteam.ivy.maven.engine.EngineControl;
import ch.ivyteam.ivy.maven.log.LogCollector;
import ch.ivyteam.ivy.maven.test.AbstractIntegrationTestMojo.TestEngineLocation;

/**
 * @since 6.1.1
 */
public class TestStartEngine extends BaseEngineProjectMojoTest {

  @Test
  public void canStartEngine() throws Exception {
    StartTestEngineMojo mojo = rule.getMojo();
    assertThat(getProperty(EngineControl.Property.TEST_ENGINE_URL)).isNull();
    assertThat(getProperty(EngineControl.Property.TEST_ENGINE_LOG)).isNull();

    Process startedProcess = null;
    try {
      startedProcess = mojo.startEngine();
      assertThat(getProperty(EngineControl.Property.TEST_ENGINE_URL))
              .startsWith("http://")
              .endsWith("/");
      assertThat(Path.of(getProperty(EngineControl.Property.TEST_ENGINE_LOG))).exists();
    } finally {
      kill(startedProcess);
    }
  }

  /**
   * MODIFY_EXISTING 1. If engine
   * {@link ch.ivyteam.ivy.maven.AbstractEngineMojo#engineDirectory} exists ->
   * do not copy 2. If engine
   * {@link ch.ivyteam.ivy.maven.AbstractEngineMojo#engineCacheDirectory} exists
   * -> do not copy
   * @throws IOException
   */
  @Test
  public void startEngine_MODIFY_EXISTING_configuredEngine() throws MojoExecutionException, IOException {
    StartTestEngineMojo mojo = rule.getMojo();
    mojo.testEngine = TestEngineLocation.MODIFY_EXISTING;
    mojo.engineDirectory = Files.createTempDirectory("test");
    assertThat(mojo.engineToTarget()).as("MODIFY_EXISTING set and using configured engine do not copy")
            .isFalse();
  }

  @Test
  public void startEngine_MODIFY_EXISTING_cacheEngine() throws MojoExecutionException {
    StartTestEngineMojo mojo = rule.getMojo();
    mojo.testEngine = TestEngineLocation.MODIFY_EXISTING;
    assertThat(mojo.engineToTarget()).as("MODIFY_EXISTING set and using cached engine do not copy").isFalse();
  }

  /**
   * COPY_FROM_TEMPLATE 1. If engine
   * {@link ch.ivyteam.ivy.maven.AbstractEngineMojo#engineDirectory} exists ->
   * do copy 2. If engine
   * {@link ch.ivyteam.ivy.maven.AbstractEngineMojo#engineCacheDirectory} exists
   * -> do copy
   * @throws IOException
   */
  @Test
  public void startEngine_COPY_FROM_TEMPLATE_configuredEngine() throws MojoExecutionException, IOException {
    StartTestEngineMojo mojo = rule.getMojo();
    mojo.testEngine = TestEngineLocation.COPY_FROM_TEMPLATE;
    mojo.engineDirectory = Files.createTempDirectory("test");
    assertThat(mojo.engineToTarget()).as("COPY_FROM_TEMPLATE set and using configured engine do copy")
            .isTrue();
  }

  @Test
  public void startEngine_COPY_FROM_TEMPLATE_cacheEngine() throws MojoExecutionException {
    StartTestEngineMojo mojo = rule.getMojo();
    mojo.testEngine = TestEngineLocation.COPY_FROM_TEMPLATE;
    assertThat(mojo.engineToTarget()).as("COPY_FROM_TEMPLATE set and using cached engine do copy").isTrue();
  }

  /**
   * COPY_FROM_CACHE 1. If engine
   * {@link ch.ivyteam.ivy.maven.AbstractEngineMojo#engineDirectory} exists ->
   * do not copy 2. If engine
   * {@link ch.ivyteam.ivy.maven.AbstractEngineMojo#engineCacheDirectory} exists
   * -> do copy
   * @throws IOException
   */
  @Test
  public void startEngine_COPY_FROM_CACHE_configuredEngine() throws MojoExecutionException, IOException {
    StartTestEngineMojo mojo = rule.getMojo();
    mojo.testEngine = TestEngineLocation.COPY_FROM_CACHE;
    mojo.engineDirectory = Files.createTempDirectory("test");
    assertThat(mojo.engineToTarget()).as("COPY_FROM_CACHE set and using configured engine do not copy")
            .isFalse();
  }

  @Test
  public void startEngine_COPY_FROM_CACHE_cacheEngine() throws MojoExecutionException {
    StartTestEngineMojo mojo = rule.getMojo();
    mojo.testEngine = TestEngineLocation.COPY_FROM_CACHE;
    assertThat(mojo.engineToTarget()).as("COPY_FROM_CACHE set and using cached engine do copy").isTrue();
  }

  @Test
  public void startEngine_copyEngineToTarget() throws Exception {
    StartTestEngineMojo mojo = rule.getMojo();
    Process startedProcess = null;
    try {
      mojo.testEngine = TestEngineLocation.COPY_FROM_TEMPLATE;
      var engineDirTarget = mojo.getEngineDir(mojo.project);
      assertThat(engineDirTarget)
        .endsWithRaw(Path.of("target").resolve("ivyEngine"))
        .doesNotExist();

      startedProcess = mojo.startEngine();
      assertThat(engineDirTarget).exists();
    } finally {
      kill(startedProcess);
    }
  }

  @Test
  public void startEngine_targetDirectoryNotClean() throws Exception {
    LogCollector log = new LogCollector();
    StartTestEngineMojo mojo = rule.getMojo();
    mojo.setLog(log);

    Process startedProcess = null;
    try {
      var engineDirTarget = mojo.getEngineDir(mojo.project);
      assertThat(engineDirTarget)
        .endsWithRaw(Path.of("target").resolve("ivyEngine"))
        .doesNotExist();
      assertThat(log.getWarnings().toString()).doesNotContain("Skipping copy");

      startedProcess = mojo.startEngine();
      assertThat(engineDirTarget).exists();
      assertThat(log.getWarnings().toString()).doesNotContain("Skipping copy");
      kill(startedProcess);

      startedProcess = mojo.startEngine();
      assertThat(engineDirTarget).exists();
      assertThat(log.getWarnings().toString()).contains("Skipping copy");
    } finally {
      kill(startedProcess);
    }
  }

  @Test
  public void startEngine_copiedEngine_executable() throws Exception {
    StartTestEngineMojo mojo = rule.getMojo();
    mojo.testEngine = TestEngineLocation.COPY_FROM_TEMPLATE;
    Process startedProcess = null;
    try {
      var cacheEngine = mojo.engineDirectory;
      assertFileExecutable(cacheEngine.resolve("elasticsearch/bin/elasticsearch"));
      assertFileExecutable(cacheEngine.resolve("elasticsearch/bin/elasticsearch.bat"));

      startedProcess = mojo.startEngine();
      var engineTarget = mojo.getEngineDir(mojo.project);
      assertFileExecutable(engineTarget.resolve("elasticsearch/bin/elasticsearch"));
      assertFileExecutable(engineTarget.resolve("elasticsearch/bin/elasticsearch.bat"));
    } finally {
      kill(startedProcess);
    }
  }

  private void assertFileExecutable(Path file) {
    assertThat(file)
            .exists()
            .isExecutable();
  }

  private static void kill(Process startedProcess) {
    if (startedProcess != null) {
      startedProcess.destroy();
      await().untilAsserted(() ->
        assertThat(startedProcess.isAlive()).as("gracefully wait on stop").isFalse()
      );
    }
  }

  private String getProperty(String name) {
    return (String) rule.project.getProperties().get(name);
  }

  @Rule
  public RunnableEngineMojoRule<StartTestEngineMojo> rule = new RunnableEngineMojoRule<StartTestEngineMojo>(
          StartTestEngineMojo.GOAL) {
    @Override
    protected void before() throws Throwable {
      super.before();
      getMojo().maxmem = "2048m";
    }
  };
}
