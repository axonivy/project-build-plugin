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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.api.di.Provides;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoExtension;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.testing.ArtifactStubFactory;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import ch.ivyteam.ivy.maven.extension.LocalRepoTest;
import ch.ivyteam.ivy.maven.extension.ProjectExtension;

@MojoTest
@ExtendWith(ProjectExtension.class)
class TestMavenDependencyMojo {

  private final Set<Artifact> artifacts = new HashSet<>();
  private MavenDependencyMojo mojo;
  private Path projectDir;

  @BeforeEach
  @InjectMojo(goal = InstallEngineMojo.GOAL)
  void setUp(InstallEngineMojo install) throws Exception {
    BaseEngineProjectMojoTest.provideEngine(install);
  }

  @BeforeEach
  @InjectMojo(goal = MavenDependencyMojo.GOAL)
  void setUp(MavenDependencyMojo dependency) throws Exception {
    this.mojo = dependency;
    BaseEngineProjectMojoTest.provideEngine(mojo);
    this.mojo.localRepository = LocalRepoTest.repo();
    projectDir = mojo.project.getBasedir().toPath();
  }

  @AfterEach
  void tearDown() {
    artifacts.clear();
  }

  @Provides
  MavenProject provideMockedComponent() throws IOException {
    var project = ProjectExtension.project();
    Mockito.lenient().when(project.getArtifacts()).thenReturn(artifacts);
    Mockito.lenient().when(project.getBasedir())
        .thenReturn(Paths.get(MojoExtension.getBasedir()).toFile());
    return project;
  }

  @Test
  void noMavenDeps() throws Exception {
    var mvnLibDir = projectDir.resolve("lib").resolve("mvn-deps");
    assertThat(mvnLibDir).doesNotExist();
    mojo.execute();
    assertThat(mvnLibDir).doesNotExist();
  }

  @Test
  void exportMavenDepsToLibDir() throws Exception {
    var mvnLibDir = projectDir.resolve("lib").resolve("mvn-deps");
    assertThat(mvnLibDir).doesNotExist();
    Artifact artifact = new ArtifactStubFactory().createArtifact("io.jsonwebtoken", "jjwt", "0.9.1");

    artifact.setDependencyTrail(List.of(mojo.project.getArtifact().toString()));
    artifact.setFile(Path.of("src/test/resources/jjwt-0.9.1.jar").toFile());

    this.artifacts.add(artifact);
    assertThat(mojo.project.getArtifacts()).isNotEmpty();
    mojo.execute();
    assertThat(mvnLibDir).exists();
    List<String> libs = getMavenLibs(mvnLibDir);
    assertThat(libs).contains("jjwt-0.9.1.jar");
  }

  @Test
  void onlyLocalDeps() throws Exception {
    var mvnLibDir = projectDir.resolve("lib").resolve("mvn-deps");
    assertThat(mvnLibDir).doesNotExist();
    Artifact artifact = new ArtifactStubFactory().createArtifact("io.jsonwebtoken", "jjwt", "0.9.1");
    artifact.setFile(Path.of("src/test/resources/jjwt-0.9.1.jar").toFile());
    artifact.setDependencyTrail(
        List.of(mojo.project.getArtifact().toString(), "other.group:other.artifact:iar:1.0.0"));
    artifacts.add(artifact);
    mojo.execute();
    assertThat(getMavenLibs(mvnLibDir))
        .as("libs provided through a dependent 'iar' should not be packed.")
        .isEmpty();
  }

  private static List<String> getMavenLibs(Path mvnLibDir) throws IOException {
    if (!Files.isDirectory(mvnLibDir)) {
      return Collections.emptyList();
    }
    try (var walker = Files.walk(mvnLibDir, 1)) {
      return walker
          .filter(p -> !p.equals(mvnLibDir))
          .map(p -> p.getFileName().toString())
          .collect(Collectors.toList());
    }
  }

}
