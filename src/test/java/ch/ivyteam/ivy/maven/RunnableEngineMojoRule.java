/*
 * Copyright (C) 2016 AXON IVY AG
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
import java.util.concurrent.TimeUnit;

import ch.ivyteam.ivy.maven.BaseEngineProjectMojoTest.EngineMojoRule;
import ch.ivyteam.ivy.maven.engine.EngineClassLoaderFactory;
import ch.ivyteam.ivy.maven.util.ClasspathJar;
import ch.ivyteam.ivy.maven.util.SharedFile;

public class RunnableEngineMojoRule<T extends AbstractEngineMojo> extends EngineMojoRule<T>
{

  public RunnableEngineMojoRule(String mojoName)
  {
    super(mojoName);
  }
  
  @Override
  protected void before() throws Throwable
  {
    super.before();
    provideClasspathJar();
  }
  
  private void provideClasspathJar() throws IOException
  {
    File cpJar = new SharedFile(project).getEngineClasspathJar();
    new ClasspathJar(cpJar).createFileEntries(EngineClassLoaderFactory
            .getIvyEngineClassPathFiles(getMojo().getEngineDirectory()));
  }
  
  @Override
  protected void after() 
  {  // give time to close output stream before we delete the project;
    sleep(1, TimeUnit.SECONDS);
    // will delete the maven project under test + logs
    super.after();
  }

  private void sleep(long duration, TimeUnit unit)
  {
    try
    {
      Thread.sleep(unit.toMillis(duration));
    }
    catch (InterruptedException ex)
    {
      throw new RuntimeException(ex);
    }
  }
}
