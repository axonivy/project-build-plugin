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

package ch.ivyteam.ivy.maven;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.testing.ArtifactStubFactory;
import org.junit.Rule;
import org.junit.Test;

import ch.ivyteam.ivy.maven.compile.CompileMojoRule;

public class TestMavenDependencyMojo extends BaseEngineProjectMojoTest
{
  private MavenDependencyMojo testMojo;

  @Rule
  public CompileMojoRule<MavenDependencyMojo> deps = new CompileMojoRule<MavenDependencyMojo>(MavenDependencyMojo.GOAL)
  {
    @Override
    protected void before() throws Throwable 
    {
      super.before();
      // use same project as first rule/mojo
      testMojo = (MavenDependencyMojo) lookupConfiguredMojo(project, MavenDependencyMojo.GOAL);
      configureMojo(testMojo);
    }
  };
  
  @Test
  public void noMavenDeps() throws Exception
  {
    MavenDependencyMojo mojo = deps.getMojo();
    var mvnLibDir = mojo.project.getBasedir().toPath().resolve("lib").resolve("mvn-deps");
    assertThat(mvnLibDir).doesNotExist();
    mojo.execute();
    assertThat(mvnLibDir).doesNotExist();
  }
  
  @Test
  public void exportMavenDepsToLibDir() throws Exception
  {
    MavenDependencyMojo mojo = deps.getMojo();
    var mvnLibDir = mojo.project.getBasedir().toPath().resolve("lib").resolve("mvn-deps");
    assertThat(mvnLibDir).doesNotExist();
    Artifact artifact = new ArtifactStubFactory().createArtifact("io.jsonwebtoken", "jjwt", "0.9.1");
    artifact.setDependencyTrail(List.of(mojo.project.getArtifact().toString()));
    artifact.setFile(new File("src/test/resources/jjwt-0.9.1.jar"));
    mojo.project.setArtifacts(Set.of(artifact));
    mojo.execute();
    assertThat(mvnLibDir).exists();
    List<String> libs = getMavenLibs(mvnLibDir);
    assertThat(libs).contains("jjwt-0.9.1.jar");
  }
  
  @Test
  public void onlyLocalDeps() throws Exception
  {
    MavenDependencyMojo mojo = deps.getMojo();
    var mvnLibDir = mojo.project.getBasedir().toPath().resolve("lib").resolve("mvn-deps");
    assertThat(mvnLibDir).doesNotExist();
    Artifact artifact = new ArtifactStubFactory().createArtifact("io.jsonwebtoken", "jjwt", "0.9.1");
    artifact.setFile(new File("src/test/resources/jjwt-0.9.1.jar"));
    artifact.setDependencyTrail(List.of(mojo.project.getArtifact().toString(), "other.group:other.artifact:iar:1.0.0"));
    mojo.project.setArtifacts(Set.of(artifact));
    mojo.execute();
    assertThat(getMavenLibs(mvnLibDir))
      .as("libs provided through a dependent 'iar' should not be packed.")
      .isEmpty();
  }
  
  private static List<String> getMavenLibs(Path mvnLibDir) throws IOException
  {
    if (!Files.isDirectory(mvnLibDir))
    {
      return Collections.emptyList();
    }
    try (var walker = Files.walk(mvnLibDir, 1))
    {
      return walker
              .filter(p -> !p.equals(mvnLibDir))
              .map(p -> p.getFileName().toString())
              .collect(Collectors.toList());
    }
  }

}
