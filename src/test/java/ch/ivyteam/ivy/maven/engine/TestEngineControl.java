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

package ch.ivyteam.ivy.maven.engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.apache.maven.api.di.Provides;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import ch.ivyteam.ivy.maven.engine.EngineControl.EngineState;
import ch.ivyteam.ivy.maven.extension.ProjectExtension;
import ch.ivyteam.ivy.maven.log.LogCollector;
import ch.ivyteam.ivy.maven.test.StopTestEngineMojo;

@MojoTest
@ExtendWith(ProjectExtension.class)
class TestEngineControl {

  private StopTestEngineMojo mojo;

  @BeforeEach
  @InjectMojo(goal = StopTestEngineMojo.GOAL)
  void setUp(StopTestEngineMojo stop) throws Exception {
    this.mojo = stop;
    ch.ivyteam.ivy.maven.BaseEngineProjectMojoTest.provideEngine(mojo);
  }

  @Provides
  MavenProject provideMockedComponent() throws IOException {
    return ProjectExtension.project();
  }

  @Test
  void resolveEngineState() throws MojoExecutionException {
    var controller = mojo.createEngineController();
    assertThat(controller.state()).isNotNull();
  }

  @Test
  void stopNotRunningEngine() throws Exception {
    var controller = mojo.createEngineController();
    controller.stop();
    Assertions.assertThat(controller.state()).isEqualTo(EngineState.STOPPED);
  }

  @Test
  void startAndStop() throws Exception {
    var log = new LogCollector();
    mojo.setLog(log);
    var controller = mojo.createEngineController();
    assertThat(controller.state()).isEqualTo(EngineState.STOPPED);

    controller.start();
    assertThat(controller.state()).isEqualTo(EngineState.RUNNING);

    controller.stop();
    assertThat(controller.state()).isEqualTo(EngineState.STOPPED);
    assertThat(log.getErrors()).isEmpty();
  }

  @Test
  void evalutateIvyContext() {
    assertThat(EngineControl.evaluateIvyContextFromUrl("sys/info.xhtml")).isEmpty();
    assertThat(EngineControl.evaluateIvyContextFromUrl("/sys/info123.xhtml")).isEmpty();
    assertThat(EngineControl.evaluateIvyContextFromUrl("ivy/sys/info")).isEqualTo("ivy/");
    assertThat(EngineControl.evaluateIvyContextFromUrl("/ivy/sys/test")).isEqualTo("ivy/");
    assertThat(EngineControl.evaluateIvyContextFromUrl("")).isEmpty();
  }
}
