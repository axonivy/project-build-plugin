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

package ch.ivyteam.ivy.maven.compile;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.commons.lang3.Strings;
import org.apache.maven.api.di.Provides;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import ch.ivyteam.ivy.maven.BaseEngineProjectMojoTest;
import ch.ivyteam.ivy.maven.InstallEngineMojo;
import ch.ivyteam.ivy.maven.engine.Slf4jSimpleEngineProperties;
import ch.ivyteam.ivy.maven.extension.LocalRepoTest;
import ch.ivyteam.ivy.maven.extension.ProjectExtension;
import ch.ivyteam.ivy.maven.extension.SysoutExtension;
import ch.ivyteam.ivy.maven.extension.SysoutExtension.Sysout;
import ch.ivyteam.ivy.maven.log.LogCollector;
import ch.ivyteam.ivy.maven.util.PathUtils;

@MojoTest
@ExtendWith(ProjectExtension.class)
@ExtendWith(SysoutExtension.class)
@TestMethodOrder(MethodOrderer.MethodName.class)
class TestCompileProjectMojo {

  private Sysout sysout;

  @BeforeAll
  static void log() {
    Slf4jSimpleEngineProperties.install();
  }

  @BeforeEach
  void setup(Sysout sysout) {
    Slf4jSimpleEngineProperties.enforceSimpleConfigReload();
    this.sysout = sysout;
  }

  @BeforeEach
  @InjectMojo(goal = InstallEngineMojo.GOAL)
  void provideEngine2(InstallEngineMojo install) throws Exception {
    BaseEngineProjectMojoTest.provideEngine(install);
  }

  private CompileTestProjectMojo test;

  @Provides
  MavenProject mockProject() throws IOException {
    return ProjectExtension.project();
  }

  @Test
  @InjectMojo(goal = CompileProjectMojo.GOAL)
  void buildWithExistingProject(CompileProjectMojo compile) throws Exception {
    CompileProjectMojo mojo = compile;

    BaseEngineProjectMojoTest.configureMojo(mojo);
    mojo.localRepository = ch.ivyteam.ivy.maven.extension.LocalRepoTest.repo();

    Build build = mojo.project.getBuild();
    build.setTestSourceDirectory("src_test");
    build.setTestOutputDirectory("target/notMyTestClasses");

    var dataClassDir = mojo.project.getBasedir().toPath().resolve("src_dataClasses");
    var wsProcDir = mojo.project.getBasedir().toPath().resolve("src_wsproc");
    var classDir = mojo.project.getBasedir().toPath().resolve("classes");
    PathUtils.clean(wsProcDir);
    PathUtils.clean(dataClassDir);

    mojo.buildApplicationDirectory = Files.createTempDirectory("MyBuildApplication");
    mojo.execute();

    assertThat(findFiles(dataClassDir, "java")).hasSize(2);
    assertThat(findFiles(wsProcDir, "java")).hasSize(1);

    assertThat(findFiles(classDir, "txt"))
        .as("classes directory must only be cleand by the builder before compilation if class files are found")
        .hasSize(1);
    assertThat(findFiles(classDir, "class"))
        .as("compiled classes must exist. but not contain any test class or old class files.")
        .hasSize(4);

    test.execute();
    assertThat(findFiles(classDir, "class"))
        .as("compiled classes must contain test resources as well")
        .hasSize(5);
  }

  private static List<Path> findFiles(Path dir, String fileExtension) throws IOException {
    if (!Files.exists(dir)) {
      return List.of();
    }
    try (var stream = Files.walk(dir)) {
      return stream
          .filter(p -> p.getFileName().toString().endsWith("." + fileExtension))
          .toList();
    }
  }

  @Test
  @InjectMojo(goal = CompileProjectMojo.GOAL)
  void compilerSettingsFile_notFoundWarnings(CompileProjectMojo compile) throws Exception {
    LogCollector log = new LogCollector();

    CompileProjectMojo mojo = compile;
    BaseEngineProjectMojoTest.configureMojo(mojo);
    mojo.localRepository = ch.ivyteam.ivy.maven.extension.LocalRepoTest.repo();
    mojo.setLog(log);

    mojo.compilerWarnings = false;
    mojo.compilerSettings = Path.of("path/to/oblivion");

    mojo.execute();
    Assertions.assertThat(log.getWarnings().toString())
        .doesNotContain("Could not locate compiler settings file");

    mojo.compilerWarnings = true;
    mojo.execute();
    Assertions.assertThat(log.getWarnings().toString())
        .contains("Could not locate compiler settings file");
  }

  @Test
  @InjectMojo(goal = CompileProjectMojo.GOAL)
  void A_validateProcess(CompileProjectMojo compile) throws Exception {
    CompileProjectMojo mojo = compile;
    BaseEngineProjectMojoTest.configureMojo(mojo);
    mojo.localRepository = LocalRepoTest.repo();

    Path project = mojo.project.getBasedir().toPath();
    var dataClassDir = project.resolve("src_dataClasses");
    var wsProcDir = project.resolve("src_wsproc");
    PathUtils.clean(wsProcDir);
    PathUtils.clean(dataClassDir);

    var ws = project.resolve("processes").resolve("myWebService.p.json");
    String wsJson = Files.readString(ws);
    var patched = Strings.CS.replace(wsJson, "//TEMPLATE!!", "ivy.task.createNote(ivy.session, null).getWritterName();");
    Files.writeString(ws, patched);

    assertThat(wsProcDir.toFile().list()).isEmpty();
    mojo.buildApplicationDirectory = Files.createTempDirectory("MyBuildApplicationVald");
    mojo.execute();

    assertThat(sysout.toString())
        .contains("processes/myWebService.p.json /element=148CA74B16C580BF-ws0 : "
            + "Start code: Method getWritterName of class ch.ivyteam.ivy.workflow.INote "
            + "is deprecated");

    var warning = sysout.toString().lines()
        .filter(l -> l.contains("/element=148CA74B16C580BF-ws0"))
        .findFirst().get();
    assertThat(warning)
        .as("WARNING prefix is streamlined with Maven CLI")
        .startsWith("[WARNING]");
  }

  @BeforeEach
  @InjectMojo(goal = CompileTestProjectMojo.GOAL)
  void setupTestMojo(CompileTestProjectMojo mojo) {
    this.test = mojo;
  }

}
