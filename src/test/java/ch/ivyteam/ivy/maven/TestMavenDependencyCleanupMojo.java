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

import java.io.File;
import java.nio.file.Files;

import org.junit.Rule;
import org.junit.Test;

public class TestMavenDependencyCleanupMojo extends BaseEngineProjectMojoTest
{
  private MavenDependencyCleanupMojo testMojo;

  @Rule
  public CompileMojoRule<MavenDependencyCleanupMojo> compile = new CompileMojoRule<MavenDependencyCleanupMojo>(MavenDependencyCleanupMojo.GOAL)
  {
    @Override
    protected void before() throws Throwable 
    {
      super.before();
      // use same project as first rule/mojo
      testMojo = (MavenDependencyCleanupMojo) lookupConfiguredMojo(project, MavenDependencyCleanupMojo.GOAL);
      configureMojo(testMojo);
    }
  };
  
  @Test
  public void noMavenDepsDir() throws Exception
  {
    var mojo = compile.getMojo();
    var mvnLibDir = mojo.project.getBasedir().toPath().resolve("lib").resolve("mvn-deps");
    assertThat(mvnLibDir).doesNotExist();
    mojo.execute();
    assertThat(mvnLibDir).doesNotExist();
  }
  
  @Test
  public void cleanupMavenDepsDir() throws Exception
  {
    var mojo = compile.getMojo();
    var mvnLibDir = Files.createDirectories(mojo.project.getBasedir().toPath().resolve("lib").resolve("mvn-deps"));
    var mvnDep = Files.copy(new File("src/test/resources/jjwt-0.9.1.jar").toPath(), mvnLibDir.resolve("jjwt-0.9.1.jar"));
    assertThat(mvnLibDir).exists();
    assertThat(mvnDep).exists();
    mojo.execute();
    assertThat(mvnLibDir).doesNotExist();
  }
  
  @Test
  public void dontCleanManualLibs() throws Exception
  {
    var mojo = compile.getMojo();
    var libDir = Files.createDirectories(mojo.project.getBasedir().toPath().resolve("lib"));
    var dep = Files.copy(new File("src/test/resources/jjwt-0.9.1.jar").toPath(), libDir.resolve("jjwt-0.9.1.jar"));
    assertThat(libDir).exists();
    assertThat(dep).exists();
    mojo.execute();
    assertThat(libDir).exists();
    assertThat(dep).exists();
  }
  
}
