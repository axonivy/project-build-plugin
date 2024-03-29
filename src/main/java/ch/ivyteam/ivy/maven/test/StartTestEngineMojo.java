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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

import org.apache.commons.exec.Executor;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import ch.ivyteam.ivy.maven.engine.EngineControl;
import ch.ivyteam.ivy.maven.engine.EngineMojoContext;
import ch.ivyteam.ivy.maven.engine.EngineVmOptions;

/**
 * Starts the Axon Ivy Engine for integration testing.
 * 
 * <p>
 * After starting the engine, this goal provides the url of the engine as
 * property <code>test.engine.url</code>. You can use this property to configure
 * your 'maven-failsafe-plugin' to work against this test engine. However, in an
 * <code>iar-integration-test</code> lifecycle this is already provided by the
 * 'ivy-integration-test-properties' goal.
 * 
 * <pre>
 * {@code
 *   <artifactId>maven-failsafe-plugin</artifactId>
 *   ...
 *   <configuration>
 *     <argLine>-Dtest.engine.url=$}{test.engine.url} {@code -Dtest.engine.app=Portal</argLine>
 *   </configuration>
 * }
 * </pre>
 * 
 * @since 6.2.0
 */
@Mojo(name = StartTestEngineMojo.GOAL)
public class StartTestEngineMojo extends AbstractIntegrationTestMojo {
  public static final String GOAL = "start-test-engine";
  public static final String IVY_ENGINE_START_TIMEOUT_SECONDS = "ivy.engine.start.timeout.seconds";

  @Parameter(property = "project", required = true, readonly = true)
  public MavenProject project;

  /**
   * The maximum heap (-Xmx) that is used for starting and running the Engine
   **/
  @Parameter(property = "ivy.engine.start.maxmem", required = false, defaultValue = "2048m")
  String maxmem;

  /** Additional classpath entries for the JVM that runs the Engine **/
  @Parameter(property = "ivy.engine.start.additional.classpath", required = false, defaultValue = "")
  String additionalClasspath;

  /**
   * Additional options for the JVM that runs the Engine. To modify the
   * classpath or the max heap use the provided properties.
   **/
  @Parameter(property = "ivy.engine.start.additional.vmoptions", required = false, defaultValue = "")
  String additionalVmOptions;

  /** The file where the engine start is logged **/
  @Parameter(property = "ivy.engine.start.log", required = false, defaultValue = "${project.build.directory}/testEngineOut.log")
  File engineLogFile;

  /** The maximum amount of seconds that we wait for a engine to start */
  @Parameter(property = IVY_ENGINE_START_TIMEOUT_SECONDS, defaultValue = "120")
  Integer startTimeoutInSeconds;

  /** Set to <code>true</code> to skip the engine start. */
  @Parameter(property = "maven.test.skip", defaultValue = "false")
  boolean skipTest;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (skipTest) {
      getLog().info("Skipping start of engine.");
      return;
    }

    try {
      startEngine();
    } catch (Exception ex) {
      throw new MojoExecutionException("Cannot start engine", ex);
    }
  }

  public Executor startEngine() throws Exception {
    File engineDir = identifyAndGetEngineDirectory();

    if (engineToTarget()) {
      engineDir = copyEngineToTarget(engineDir);
    }

    EngineVmOptions vmOptions = new EngineVmOptions(maxmem, additionalClasspath, additionalVmOptions);
    EngineControl engineControl = new EngineControl(new EngineMojoContext(
            engineDir, project, getLog(), engineLogFile, vmOptions, startTimeoutInSeconds));
    return engineControl.start();
  }

  private File copyEngineToTarget(File cachedEngineDir) {
    File targetEngine = getTargetDir(project);
    if (targetEngine.exists()) {
      getLog().warn("Skipping copy of engine to " + targetEngine
              + " it already exists. Use \"mvn clean\" to ensure a clean engine each cycle.");
      return targetEngine;
    }

    try {
      getLog().info("Parameter <testEngine> is set to " + testEngine +
              ", copying engine from: " + cachedEngineDir + " to " + targetEngine);

      copyEngine(cachedEngineDir.toPath(), targetEngine.toPath());
      return targetEngine;
    } catch (IOException ex) {
      getLog().warn("Could not copy engine from: " + cachedEngineDir + " to " + targetEngine, ex);
    }
    return cachedEngineDir;
  }

  public void copyEngine(Path src, Path dest) throws IOException {
    try (Stream<Path> walk = Files.walk(src)) {
      walk.forEach(source -> copyFile(source, dest.resolve(src.relativize(source))));
    }
  }

  private void copyFile(Path source, Path dest) {
    try {
      Files.copy(source, dest, StandardCopyOption.COPY_ATTRIBUTES);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }
}