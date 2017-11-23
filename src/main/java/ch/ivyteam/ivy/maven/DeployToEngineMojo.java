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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenFileFilter;

import ch.ivyteam.ivy.maven.engine.deploy.DeploymentOptionsFile;
import ch.ivyteam.ivy.maven.engine.deploy.IvyDeployer;
import ch.ivyteam.ivy.maven.engine.deploy.MarkerFileDeployer;

/**
 * Deploys a single project (iar) or a full application (set of projects as zip) to a running AXON.IVY Engine.
 * 
 * <p>Command line invocation is supported. E.g.</p>
 * <pre>mvn com.axonivy.ivy.ci:project-build-plugin:7.1.0:deploy-to-engine 
 * -Divy.deploy.file=myProject.iar 
 * -Divy.deploy.engine.dir=c:/axonviy/engine
 * -Divy.deploy.engine.app=Portal</pre>
 * 
 * @since 7.1.0
 */
@Mojo(name = DeployToEngineMojo.GOAL, requiresProject=false)
public class DeployToEngineMojo extends AbstractEngineMojo
{
  static final String PROPERTY_IVY_DEPLOY_FILE = "ivy.deploy.file";

  public static final String GOAL = "deploy-to-engine";
  
  /** The file to deploy. Can either be a *.iar project file or a *.zip file containing a full application (set of projects). By default the packed IAR from the {@link IarPackagingMojo#GOAL} is used. */
  @Parameter(property=PROPERTY_IVY_DEPLOY_FILE, defaultValue="${project.build.directory}/${project.artifactId}-${project.version}.iar")
  File deployFile;
  
  /** The path to the AXON.IVY Engine to which we deploy the file. <br/>
   * The path can reference a remote engine by using UNC paths e.g. <code>\\myRemoteHost\myEngineShare</code> */
  @Parameter(property="ivy.deploy.engine.dir", defaultValue="${"+ENGINE_DIRECTORY_PROPERTY+"}")
  File deployEngineDirectory;
  
  /** The name of an ivy application to which the file is deployed. */
  @Parameter(property="ivy.deploy.engine.app", defaultValue="SYSTEM")
  String deployToEngineApplication;
  
  /** The auto deployment directory of the engine. Must match the ivy engine system property 'deployment.directory' */
  @Parameter(property="ivy.deploy.dir", defaultValue="deploy")
  String deployDirectory;
  
  /** The file that contains deployment options.<br/>
   * Example options file content:
   * <pre><code>deployTestUsers: true
   *configuration:
   *  overwrite: true
   *  cleanup: REMOVE_UNUSED
   *target:
   *  version: RELEASED
   *  state: ACTIVE_AND_RELEASED</code></pre>
   *  
   *  <p>Inside the options file you can use property placeholders. The options file may look like this:</p>
   *  <pre><code>deployTestUsers: ${ivy.deploy.test.users}
   *configuration:
   *  overwrite: true
   *  cleanup: REMOVE_UNUSED
   *target:
   *  version: AUTO
   *  state: ${ivy.deploy.target.state}</code></pre>
   *  
   *  <p>All options in this file are optional. You only need to specify options that overwrite the default behavior.</p>
   *  
   * @see <a href="https://developer.axonivy.com/doc/7.1.latest/EngineGuideHtml/administration.html#administration.deployment.directory.options">Engine Guide</a>
   */  
  @Parameter(property="ivy.deploy.options.file", required=false)
  File deployOptionsFile;
  
  /** The maximum amount of seconds that we wait for a deployment result from the engine */
  @Parameter(property="ivy.deploy.timeout.seconds", defaultValue="30")
  Integer deployTimeoutInSeconds;
  
  /** Set to <code>true</code> to skip the deployment to the engine. */
  @Parameter(defaultValue="false", property="ivy.deploy.skip")
  boolean skipDeploy;
  
  @Component
  private MavenFileFilter fileFilter;
  
  @Parameter(property = "project", required = false, readonly = true)
  MavenProject project;
  
  @Parameter(property = "session", required = true, readonly = true)
  MavenSession session;
  
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException
  {
    if (skipDeploy)
    {
      getLog().info("Skipping deployment to engine.");
      return;
    }
    warnDeprectadIarProperty();
    if (!deployFile.exists())
    {
      getLog().warn("Skipping deployment of '"+deployFile+"' to engine. The file does not exist.");
      return;
    }
    File deployDir = getDeployDirectory();
    if (!deployDir.exists())
    {
      getLog().warn("Skipping deployment to engine '"+deployEngineDirectory+"'. The directory '"+deployDir+"' does not exist.");
      return;
    }

    File targetDeployableFile = createTargetDeployableFile(deployDir);
    String deployablePath = deployDir.toPath().relativize(targetDeployableFile.toPath()).toString();
    DeploymentOptionsFile deploymentOptions = new DeploymentOptionsFile(deployOptionsFile, project, session, fileFilter);
    IvyDeployer deployer = new MarkerFileDeployer(deployDir, deploymentOptions, deployTimeoutInSeconds, deployFile, targetDeployableFile);
    deployer.deploy(deployablePath, getLog());
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

  private File createTargetDeployableFile(File deployDir)
  {
    File deployApp = new File(deployDir, deployToEngineApplication);
    File targetDeployableFile = new File(deployApp, deployFile.getName());
    return targetDeployableFile;
  }

  @SuppressWarnings("deprecation")
  private void warnDeprectadIarProperty()
  {
    String legacyIarFileProperty = System.getProperty(IarDeployMojo.PROPERTY_IVY_DEPLOY_IAR_FILE);
    if (legacyIarFileProperty != null)
    {
      getLog().warn("Ignoring deprecated property '"+IarDeployMojo.PROPERTY_IVY_DEPLOY_IAR_FILE+"' with value '"+legacyIarFileProperty+"'.");
      getLog().warn("Please migrate to the new property '"+PROPERTY_IVY_DEPLOY_FILE+"'.");
    }
  }
  
}
