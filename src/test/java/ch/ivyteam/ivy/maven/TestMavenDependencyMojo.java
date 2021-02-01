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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.testing.ArtifactStubFactory;
import org.junit.Rule;
import org.junit.Test;

public class TestMavenDependencyMojo extends BaseEngineProjectMojoTest
{
  private MavenDependencyMojo testMojo;

  @Rule
  public CompileMojoRule<MavenDependencyMojo> compile = new CompileMojoRule<MavenDependencyMojo>(MavenDependencyMojo.GOAL)
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
    MavenDependencyMojo mojo = compile.getMojo();
    var mvnLibDir = mojo.project.getBasedir().toPath().resolve("lib").resolve("mvn-deps");
    assertThat(mvnLibDir).doesNotExist();
    mojo.execute();
    assertThat(mvnLibDir).doesNotExist();
  }
  
  @Test
  public void exportMavenDepsToLibDir() throws Exception
  {
    MavenDependencyMojo mojo = compile.getMojo();
    var mvnLibDir = mojo.project.getBasedir().toPath().resolve("lib").resolve("mvn-deps");
    assertThat(mvnLibDir).doesNotExist();
    Artifact artifact = new ArtifactStubFactory().createArtifact("io.jsonwebtoken", "jjwt", "0.9.1");
    mojo.project.setArtifacts(Set.of(artifact));
    var artefacts = mojo.project.getArtifacts();
    for (var art : artefacts)
    {
      mojo.localRepository.find(art);
    }
    mojo.execute();
    assertThat(mvnLibDir).exists();
    assertThat(getMavenLibs(mvnLibDir)).contains("jjwt-0.9.1.jar");
  }
  
  private List<String> getMavenLibs(Path mvnLibDir) throws IOException
  {
    try (var walker = Files.walk(mvnLibDir, 1))
    {
      return walker
              .filter(p -> !p.equals(mvnLibDir))
              .map(p -> p.getFileName().toString())
              .collect(Collectors.toList());
    }
  }

}
