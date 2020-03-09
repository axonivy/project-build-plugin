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

import java.io.File;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

import ch.ivyteam.ivy.maven.engine.EngineClassLoaderFactory;
import ch.ivyteam.ivy.maven.engine.MavenProperties;

/**
 * Shares the Engine core classpath with the property: <code>ivy.engine.core.classpath</code>.
 * 
 * @since 6.2.0
 */
@Mojo(name = ShareEngineCoreClasspathMojo.GOAL)
public class ShareEngineCoreClasspathMojo extends AbstractEngineMojo
{
  public static final String GOAL = "share-engine-core-classpath";

  public static final String IVY_ENGINE_CORE_CLASSPATH_PROPERTY = "ivy.engine.core.classpath"; //Duplicated for the comment, JavaDoc value didn't work.
  
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException
  {
    List<File> ivyEngineClassPathFiles = EngineClassLoaderFactory.getIvyEngineClassPathFiles(identifyAndGetEngineDirectory());
    String propertyValue = StringUtils.join(ivyEngineClassPathFiles, ",");
    
    MavenProperties properties = new MavenProperties(project, getLog());
    properties.setMavenProperty(IVY_ENGINE_CORE_CLASSPATH_PROPERTY, propertyValue);
  }

}
