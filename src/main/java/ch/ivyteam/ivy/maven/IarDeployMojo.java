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
import java.nio.file.Paths;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import ch.ivyteam.ivy.maven.engine.deploy.IvyDeployer;
import ch.ivyteam.ivy.maven.engine.deploy.FileDeployer;

/**
 * Deploys an ivy-archive (IAR) to a running AXON.IVY Engine.
 * 
 * <p>Command line invocation is supported. E.g.</p>
 * <pre>mvn com.axonivy.ivy.ci:project-build-plugin:6.2.0:deploy-iar 
 * -Divy.deploy.iarFile=myProject.iar 
 * -Divy.deploy.engine.dir=c:/axonviy/engine
 * -Divy.deploy.engine.app=Portal</pre>
 * 
 * @since 6.1.0
 * @deprecated since 7.1.0. Use the {@link DeployToEngineMojo#GOAL} instead.
 */
@Deprecated
@Mojo(name = IarDeployMojo.GOAL, requiresProject=false)
public class IarDeployMojo extends AbstractEngineMojo
{
  static final String PROPERTY_IVY_DEPLOY_IAR_FILE = "ivy.deploy.iarFile";

  public static final String GOAL = "deploy-iar";
  
  /** The IAR to deploy. By default the packed IAR from the {@link IarPackagingMojo#GOAL} is used. */
  @Parameter(property=PROPERTY_IVY_DEPLOY_IAR_FILE, defaultValue="${project.build.directory}/${project.artifactId}-${project.version}.iar")
  File deployIarFile;
  
  /** The path to the AXON.IVY Engine to which we deploy the IAR. <br/>
   * The path can reference a remote engine by using UNC paths e.g. <code>\\myRemoteHost\myEngineShare</code> */
  @Parameter(property="ivy.deploy.engine.dir", defaultValue="${"+ENGINE_DIRECTORY_PROPERTY+"}")
  File deployEngineDirectory;
  
  /** The name of an ivy application to which the IAR is deployed. */
  @Parameter(property="ivy.deploy.engine.app", defaultValue="SYSTEM")
  String deployToEngineApplication;
  
  /** The auto deployment directory of the engine. Must match the ivy engine system property 'deployment.directory' */
  @Parameter(property="ivy.deploy.dir", defaultValue="deploy")
  String deployDirectory;
  
  /** The maximum amount of seconds that we wait for a deployment result from the engine */
  @Parameter(property="ivy.deploy.timeout.seconds", defaultValue="30")
  Integer deployTimeoutInSeconds;
  
  /** Set to <code>true</code> to skip the deployment to the engine.
   * @since 6.1.1
   */
  @Parameter(defaultValue="false", property="ivy.deploy.skip")
  boolean skipDeploy;
  
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException
  {
    if (skipDeploy)
    {
      getLog().info("Skipping deployment of project.");
      return;
    }
    getLog().warn("The goal '"+GOAL+"' is deprecated! Please migrate to the new goal '"+DeployToEngineMojo.GOAL+"'.");
    
    if (!deployIarFile.exists())
    {
      getLog().warn("Skipping IAR deployment of '"+deployIarFile+"'. The file does not exist.");
      return;
    }
    File deployDir = getDeployDirectory();
    if (!deployDir.exists())
    {
      getLog().warn("Skipping IAR deployment to engine '"+deployEngineDirectory+"'. The directory '"+deployDir+"' does not exist.");
      return;
    }
    
    File deployApp = new File(deployDir, deployToEngineApplication);
    File targetIarFile = new File(deployApp, deployIarFile.getName());

    String iarPath = deployDir.toPath().relativize(targetIarFile.toPath()).toString();
    IvyDeployer deployer = new FileDeployer(deployDir, null, deployTimeoutInSeconds, deployIarFile, targetIarFile);
    deployer.deploy(iarPath, getLog());
  }

  private File getDeployDirectory() throws MojoExecutionException
  {
    if (deployEngineDirectory == null)
    { // re-use engine used to build
      deployEngineDirectory = identifyAndGetEngineDirectory();
    }
    if (Paths.get(deployDirectory).isAbsolute())
    {
      return new File(deployDirectory);
    }
    return new File(deployEngineDirectory, deployDirectory);
  }
  
}
