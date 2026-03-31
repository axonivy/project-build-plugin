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

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import ch.ivyteam.ivy.maven.extension.ProjectExtension;

@MojoTest
@ExtendWith(ProjectExtension.class)
class TestMavenDependencyCleanupMojo extends BaseEngineProjectMojoTest {

  private MavenDependencyCleanupMojo mojo;

  @BeforeEach
  @InjectMojo(goal = MavenDependencyCleanupMojo.GOAL)
  void setUp(MavenDependencyCleanupMojo clean) {
    this.mojo = clean;
  }

  @Test
  void noMavenDepsDir() throws Exception {
    var mvnLibDir = mojo.project.getBasedir().toPath().resolve("lib").resolve("mvn-deps");
    assertThat(mvnLibDir).doesNotExist();
    mojo.execute();
    assertThat(mvnLibDir).doesNotExist();
  }

  @Test
  void cleanupMavenDepsDir() throws Exception {
    var mvnLibDir = Files
        .createDirectories(mojo.project.getBasedir().toPath().resolve("lib").resolve("mvn-deps"));
    var mvnDep = Files.copy(Path.of("src/test/resources/jjwt-0.9.1.jar"), mvnLibDir.resolve("jjwt-0.9.1.jar"));
    assertThat(mvnLibDir).exists();
    assertThat(mvnDep).exists();
    mojo.execute();
    assertThat(mvnLibDir).doesNotExist();
  }

  @Test
  void dontCleanManualLibs() throws Exception {
    var libDir = Files.createDirectories(mojo.project.getBasedir().toPath().resolve("lib"));
    var dep = Files.copy(Path.of("src/test/resources/jjwt-0.9.1.jar"), libDir.resolve("jjwt-0.9.1.jar"));
    assertThat(libDir).exists();
    assertThat(dep).exists();
    mojo.execute();
    assertThat(libDir).exists();
    assertThat(dep).exists();
  }
}
