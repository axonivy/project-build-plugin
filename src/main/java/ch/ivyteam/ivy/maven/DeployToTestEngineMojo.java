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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

import ch.ivyteam.ivy.maven.engine.deploy.DeploymentOptionsFileFactory;

/**
 * <p>Deploys a set of test projects (iar) or a full application (set of projects as zip) to a running test engine.</p>
 *
 * @since 9.1.0
 */
@Mojo(name = DeployToTestEngineMojo.TEST_GOAL, requiresProject=true)
public class DeployToTestEngineMojo extends AbstractDeployMojo
{
  public static final String TEST_GOAL = "deploy-to-test-engine";
  
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
    
    deployTestApp();
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
