/*
 * Copyright (C) 2020 AXON Ivy AG
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
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.zip.ZipArchiver;

import ch.ivyteam.ivy.maven.engine.deploy.DeploymentOptionsFileFactory;
import ch.ivyteam.ivy.maven.util.MavenProperties;
import ch.ivyteam.ivy.maven.util.MavenRuntime;

/**
 * <p>Deploys a set of test projects (iar) or a full application (set of projects as zip) to a running test engine.</p>
 * <p>By default the IAR of the current project plus all declared IAR dependencies will be deployed to the test engine.</p>
 *
 * @since 9.1.0
 */
@Mojo(name = DeployToTestEngineMojo.TEST_GOAL, requiresDependencyResolution=ResolutionScope.TEST)
public class DeployToTestEngineMojo extends AbstractDeployMojo
{
  public static final String TEST_GOAL = "deploy-to-test-engine";
  
  public static interface Property
  {
    String TEST_ENGINE_APP = "test.engine.app";
  }
  
  /** If set to <code>true</code>, the 'deployFile' will automatically be replaced with an ZIP file 
   * that contains all IAR dependencies of the project. <br/>
   * This change will only be applied if 'deployFile' has it's default value and at least one IAR dependency has been declared. */
  @Parameter(property="ivy.deploy.deps.as.app", defaultValue="true")
  boolean deployDepsAsApp;
  
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException
  {
    if (checkSkip())
    {
      return;
    }
    if (deployToEngineApplication == null)
    {
      deployToEngineApplication = project.getArtifactId();
      getLog().info("Using '"+deployToEngineApplication+"' as target app.");
    }
    var props = new MavenProperties(project, getLog());
    props.set(Property.TEST_ENGINE_APP, deployToEngineApplication);
    
    boolean isDefaultFile = deployFile.getName().endsWith(project.getArtifactId()+"-"+project.getVersion()+".iar");
    if (isDefaultFile && deployDepsAsApp)
    {
      provideDepsAsAppZip();
    }
    
    deployTestApp();
  }

  private void provideDepsAsAppZip()
  {
    List<File> deps = MavenRuntime.getDependencies(project, session, "iar");
    if (deps.isEmpty())
    {
      return;
    }
    
    deps.add(deployFile);
    try
    {
      File appZip = createFullAppZip(deps);
      getLog().info("Using "+appZip.getName()+" with all IAR dependencies of this test project for deployments.");
      deployFile = appZip;
    }
    catch (ArchiverException | IOException ex)
    {
      getLog().error("Failed to write deployable application ", ex);
    }
  }

  File createFullAppZip(List<File> deps) throws ArchiverException, IOException
  {
    File appZip = new File(project.getBuild().getDirectory(), deployToEngineApplication+"-app.zip");
    ZipArchiver appZipper = new ZipArchiver();
    appZipper.setDestFile(appZip);
    deps.stream().forEach(iar -> appZipper.addFile(iar, iar.getName()));
    appZipper.createArchive();
    return appZip;
  }

  private void deployTestApp() throws MojoExecutionException
  {
    File resolvedOptionsFile = createDeployOptionsFile(new DeploymentOptionsFileFactory(deployFile));
    try
    {
      File deployDir = new File(getEngineDir(project), DeployToEngineMojo.DEPLOY_DEFAULT);
      deployToDirectory(resolvedOptionsFile, deployDir);
    }
    finally
    {
      removeTemporaryDeploymentOptionsFile(resolvedOptionsFile);
    }
  }

}
