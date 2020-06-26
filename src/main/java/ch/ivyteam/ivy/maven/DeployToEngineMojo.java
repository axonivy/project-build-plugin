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
import java.nio.file.Paths;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Server;

import ch.ivyteam.ivy.maven.engine.deploy.DeploymentOptionsFileFactory;
import ch.ivyteam.ivy.maven.engine.deploy.http.HttpDeployer;

/**
 * <p>Deploys a single project (iar) or a full application (set of projects as zip) to a running AXON.IVY Engine.</p>
 *
 * <p>Command line invocation is supported.</p>
 * <p>Local engine (using DIRECTORY deploy method):</p>
 * <pre>mvn com.axonivy.ivy.ci:project-build-plugin:8.0.0:deploy-to-engine
 * -Divy.deploy.file=myProject.iar
 * -Divy.deploy.engine.dir=c:/axonivy/engine
 * -Divy.deploy.engine.app=Portal</pre>
 * 
 * <p>Remote Engine (using HTTP deploy method):</p>
 * <pre>mvn com.axonivy.ivy.ci:project-build-plugin:8.0.0:deploy-to-engine 
 * -Divy.deploy.file=myProject.iar 
 * -Divy.deploy.method=HTTP 
 * -Divy.deploy.server.id=AxonIvyEngine
 * -Divy.deploy.engine.url=http://ivyhost:8080/ivy 
 * -Divy.deploy.engine.app=portal</pre>
 *
 * @since 7.1.0
 */
@Mojo(name = DeployToEngineMojo.GOAL, requiresProject=false)
public class DeployToEngineMojo extends AbstractDeployMojo
{
  private static final String DEPLOY_ENGINE_DIR_DEFAULT = "${"+ENGINE_DIRECTORY_PROPERTY+"}";
  private static final String HTTP_ENGINE_URL_DEFAULT = "http://localhost:8080/ivy";

  static final String DEPLOY_DEFAULT = "deploy";

  public static final String GOAL = "deploy-to-engine";

  /** The path to the AXON.IVY Engine to which we deploy the file. <br/>
   * The path can reference a remote engine by using UNC paths e.g. <code>\\myRemoteHost\myEngineShare</code> */
  @Parameter(property="ivy.deploy.engine.dir", defaultValue=DEPLOY_ENGINE_DIR_DEFAULT)
  File deployEngineDirectory;
  
  /** The auto deployment directory of the engine. Must match the ivy engine system property 'deployment.directory' */
  @Parameter(property="ivy.deploy.dir", defaultValue=DEPLOY_DEFAULT)
  String deployDirectory;
  
  /** 
   * The deploy method
   * 
   * <p>Possible values:</p>
   * <ul>
   *    <li><code>DIRECTORY</code>: use filesystem to deploy to local engine</li>
   *    <li><code>HTTP</code>: use HTTP or HTTPS to deploy to a remote engine</li>
   * </ul>
   * @since 7.4 */
  @Parameter(property="ivy.deploy.method", defaultValue=DeployMethod.DIRECTORY)
  String deployMethod;
  
  /**
   * Id of server configured in settings.xml that specifies the administrator user name and password 
   * used to authenticate in case of HTTP deployment.
   * @since 7.4 
   */
  @Parameter(property="ivy.deploy.server.id")
  String deployServerId;
  
  /** 
   * Engine url for deployment over HTTP or HTTPS
   * @since 7.4 
   */
  @Parameter(property="ivy.deploy.engine.url", defaultValue=HTTP_ENGINE_URL_DEFAULT)
  String deployEngineUrl;
  
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException
  {
    warnDeprectadIarProperty();
    if (checkSkip())
    {
      return;
    }
    if (StringUtils.isEmpty(deployToEngineApplication))
    {
      throw new MojoExecutionException("The parameter '"+deployToEngineApplication+"' for goal "+GOAL+" is missing.");
    }
    
    File resolvedOptionsFile = createDeployOptionsFile(new DeploymentOptionsFileFactory(deployFile));
    try
    {
      deployWithOptions(resolvedOptionsFile);
    }
    finally
    {
      removeTemporaryDeploymentOptionsFile(resolvedOptionsFile);
    }
  }
  
  private void deployWithOptions(File resolvedOptionsFile) throws MojoExecutionException
  {
    getLog().info("Deploying project "+deployFile.getName());
    if (DeployMethod.DIRECTORY.equals(deployMethod))
    {
      deployToDirectory(resolvedOptionsFile);
    }
    else if (DeployMethod.HTTP.equals(deployMethod))
    {
      deployToRestService(resolvedOptionsFile);
    }
    else
    {
      getLog().warn("Invalid deploy method  "+deployMethod+" configured in parameter deployMethod (Supported values are "+DeployMethod.DIRECTORY+", "+DeployMethod.HTTP+")");
    }
  }

  private void deployToDirectory(File resolvedOptionsFile) throws MojoExecutionException
  {
    File deployDir = getDeployDirectory();
    if (deployEngineDirectory == null)
    {
      getLog().warn("Skipping deployment, target engine could not be evaluated." +
              "Please configure an existing engine to deploy to with argument <deployEngineDirectory>.");
      return;
    }

    if (!deployDir.exists())
    {
      getLog().warn("Skipping deployment to engine '"+deployEngineDirectory+"'. The directory '"+deployDir+"' does not exist.");
      return;
    }
    
    checkDirParams();
  
    deployToDirectory(resolvedOptionsFile, deployDir);
  }

  private void deployToRestService(File resolvedOptionsFile) throws MojoExecutionException
  {    
    checkHttpParams();
    
    Server server = session.getSettings().getServer(deployServerId);
    if (server == null)
    {
      getLog().warn("Can not load credentials from settings.xml because server '" + deployServerId + "' is not definied. Try to deploy with default username, password");
    }
    HttpDeployer httpDeployer = new HttpDeployer(server, 
            deployEngineUrl, deployToEngineApplication, deployFile, resolvedOptionsFile);
    httpDeployer.deploy(getLog());
  }

  private void checkHttpParams()
  {
    if (!DEPLOY_DEFAULT.equals(deployDirectory))
    {
      logParameterIgnoredByMethod("deployDirectory", deployDirectory, DeployMethod.HTTP);
    }
    Object defaultDeployEngineDirectory = project.getProperties().get(ENGINE_DIRECTORY_PROPERTY);
    if (deployEngineDirectory != null && !deployEngineDirectory.getPath().equals(defaultDeployEngineDirectory))
    {
      logParameterIgnoredByMethod("deployEngineDirectory", deployEngineDirectory.getPath(), DeployMethod.HTTP);
    }
  }
  
  private void checkDirParams()
  {
    if (!HTTP_ENGINE_URL_DEFAULT.equals(deployEngineUrl))
    {
      logParameterIgnoredByMethod("deployEngineUrl", deployEngineUrl, DeployMethod.DIRECTORY);
    }
    if (StringUtils.isNotBlank(deployServerId))
    {
      logParameterIgnoredByMethod("deployServerId", deployServerId, DeployMethod.DIRECTORY);
    }
  }

  private void logParameterIgnoredByMethod(String parameter, String value, String method)
  {
    getLog().warn("Parameter "+parameter+" is set to "+value+" but will be ignored by "+method+" deployment.");
  }

  private File getDeployDirectory() throws MojoExecutionException
  {
    if (deployEngineDirectory == null || engineToTarget())
    { // re-use engine used to build
      deployEngineDirectory = getEngineDir(project);
    }
    if (Paths.get(deployDirectory).isAbsolute())
    {
      return new File(deployDirectory);
    }
    return new File(deployEngineDirectory, deployDirectory);
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

  public static interface DefaultDeployOptions
  {
    String CLEANUP_DISABLED = "DISABLED";
    String VERSION_AUTO = "AUTO";
    String STATE_ACTIVE_AND_RELEASED = "ACTIVE_AND_RELEASED";
    String FILE_FORMAT_AUTO = "AUTO";
    String DEPLOY_TEST_USERS = "AUTO";
  }
  
  public static interface DeployMethod
  {
    String DIRECTORY = "DIRECTORY";
    String HTTP = "HTTP";
  }

}
