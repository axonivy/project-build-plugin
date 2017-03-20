/*
 * Copyright (C) 2015 AXON IVY AG
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

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;

public class TestCompileProjectMojo extends BaseEngineProjectMojoTest
{
  private CompileTestProjectMojo testMojo;

  @Rule
  public CompileMojoRule<CompileProjectMojo> compile = new CompileMojoRule<CompileProjectMojo>(CompileProjectMojo.GOAL)
  {
    @Override
    protected void before() throws Throwable 
    {
      super.before();
      // use same project as first rule/mojo
      testMojo = (CompileTestProjectMojo) lookupConfiguredMojo(project, CompileTestProjectMojo.GOAL);
      configureMojo(testMojo);
    }
  };
  
  @Test
  public void buildWithExistingProject() throws Exception
  {
    CompileProjectMojo mojo = compile.getMojo();
    
    File dataClassDir = new File(mojo.project.getBasedir(), "src_dataClasses");
    File wsProcDir = new File(mojo.project.getBasedir(), "src_wsproc");
    File classDir = new File(mojo.project.getBasedir(), "classes");
    FileUtils.cleanDirectory(wsProcDir);
    FileUtils.cleanDirectory(dataClassDir);
    
    mojo.buildApplicationDirectory = Files.createTempDirectory("MyBuildApplication").toFile();
    mojo.execute();
    
    assertThat(findFiles(dataClassDir, "java")).hasSize(2);
    assertThat(findFiles(wsProcDir, "java")).hasSize(1);
    
    assertThat(findFiles(classDir, "txt"))
      .as("classes directory must be cleand by the builder before compilation")
      .isEmpty();
    assertThat(findFiles(classDir, "class"))
      .as("compiled classes must exist. but not contain any test class.")
      .hasSize(4);
    assertThat(findFiles(classDir, "xml"))
      .as("resources from the src folder must be copied to the classes folder")
      .hasSize(1);
    
    testMojo.execute();
    assertThat(findFiles(classDir, "class"))
      .as("compiled classes must contain test resources as well")
      .hasSize(5);
  }

}
