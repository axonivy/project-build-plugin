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

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import ch.ivyteam.ivy.maven.engine.MavenProjectBuilderProxy;
import ch.ivyteam.ivy.maven.util.ClasspathJar;
import ch.ivyteam.ivy.maven.util.SharedFile;

/**
 * Compiles an ivy Project with an ivyEngine.
 * 
 * @author Reguel Wermelinger
 * @since 6.0.0
 */
@Mojo(name=CompileProjectMojo.GOAL, requiresDependencyResolution=ResolutionScope.COMPILE)
public class CompileProjectMojo extends AbstractProjectCompileMojo
{
  public static final String GOAL = "compileProject";
  
  /** 
   * Set to <code>true</code> to bypass the generation of <b>ivy data classes</b>+<b>webservice processes</b> and compilation of <b>java sources</b>.
   * @since 6.1.0
   */
  @Parameter(property="ivy.compiler.skip", defaultValue="false")
  boolean skipCompilation;

  /** 
   * Set to <code>true</code> to avoid the validation of ivyScript code within ivy processes.
   * @since 8.0.3
   */
  @Parameter(property="ivy.script.validation.skip", defaultValue="true")
  boolean skipScriptValidation;
  
  @Override
  protected void compile(MavenProjectBuilderProxy projectBuilder) throws Exception
  {
    if (skipCompilation)
    {
      return;
    }
    
    getLog().info("Compiling ivy Project...");
    List<File> iarDependencies = getDependencies("iar");
    List<File> iarJars = projectBuilder.createIarJars(iarDependencies);
    Map<String, Object> options = getOptions();
    projectBuilder.compile(project.getBasedir(), iarJars, options);
    
    if (skipScriptValidation)
    {
      getLog().info("Skipping ivy script validation");
    }
    else
    {
      projectBuilder.validate(project.getBasedir(), iarDependencies, options);
    }
    
    writeDependencyIarJar(iarJars);
  }
  
  private void writeDependencyIarJar(Collection<File> iarJarDepenencies) throws IOException
  {
    if (iarJarDepenencies == null)
    { // no dependencies
      return;
    }
    File jar = new SharedFile(project).getIarDependencyClasspathJar();
    new ClasspathJar(jar).createFileEntries(iarJarDepenencies);
  }

}
