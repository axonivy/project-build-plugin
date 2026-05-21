/*
 * Copyright (C) 2021 Axon Ivy AG
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package ch.ivyteam.ivy.maven.deploy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import ch.ivyteam.ivy.maven.IarPackagingMojo;
import ch.ivyteam.ivy.maven.deploy.DeployToEngineMojo.DefaultDeployOptions;
import ch.ivyteam.ivy.maven.engine.deploy.DeploymentOptionsFileFactory;
import ch.ivyteam.ivy.maven.engine.deploy.YamlOptionsFactory;
import ch.ivyteam.ivy.maven.engine.deploy.dir.FileDeployer;
import ch.ivyteam.ivy.maven.test.AbstractIntegrationTestMojo;

public abstract class AbstractDeployMojo extends AbstractIntegrationTestMojo {
  static final String PROPERTY_IVY_DEPLOY_FILE = "ivy.deploy.file";

  /**
   * The file to deploy. Can either be a *.iar project file or a *.zip file
   * containing a full application (set of projects). By default the packed IAR
   * from the {@link IarPackagingMojo#GOAL} is used.
   */
  @Parameter(property = PROPERTY_IVY_DEPLOY_FILE, defaultValue = "${project.build.directory}/${project.artifactId}-${project.version}.iar")
  protected Path deployFile;

  /**
   * If set to <code>true</code> then test users defined in the projects are
   * deployed to the engine. If set to <code>auto</code> then test users will
   * only deployed when engine runs in demo mode. Should only be used for
   * testing.
   * <p>
   * This option is only in charge if security system is set to <i>Ivy Security
   * System</i>. This means if the security system is Active Directory or Novell
   * eDirectory test users will never deployed.
   * </p>
   */
  @Parameter(property = "ivy.deploy.test.users", defaultValue = DefaultDeployOptions.DEPLOY_TEST_USERS)
  public String deployTestUsers;

  @Parameter(property = "project", required = false, readonly = true)
  protected MavenProject project;
  @Parameter(property = "session", required = true, readonly = true)
  MavenSession session;

  /**
   * The maximum amount of seconds that we wait for a deployment result from the
   * engine
   */
  @Parameter(property = "ivy.deploy.timeout.seconds", defaultValue = "30")
  protected Integer deployTimeoutInSeconds;

  /** Set to <code>true</code> to skip the deployment to the engine. */
  @Parameter(defaultValue = "false", property = "ivy.deploy.skip")
  protected boolean skipDeploy;

  /** The name of security context to which the file is deployed. */
  @Parameter(property = "ivy.deploy.engine.context", required = false, defaultValue = "default")
  String deployToEngineSecurityContext;

  /** The name of an ivy application to which the file is deployed. */
  @Parameter(property = "ivy.deploy.engine.app")
  String deployToEngineApplication;

  /** The application version to which the file is deployed. */
  @Parameter(property = "ivy.deploy.engine.app", required = false, defaultValue = "new")
  String deployToEngineApplicationVersion;

  public AbstractDeployMojo() {}

  protected final boolean checkSkip() {
    if (skipDeploy) {
      getLog().info("Skipping deployment to engine.");
      return true;
    }
    if (!Files.exists(deployFile)) {
      getLog().warn("Skipping deployment of '" + deployFile + "' to engine. The file does not exist.");
      return true;
    }
    return false;
  }

  protected final Path createDeployOptionsFile(DeploymentOptionsFileFactory optionsFileFactory) throws MojoExecutionException {
    try {
      String yamlOptions = YamlOptionsFactory.toYaml(this);
      if (StringUtils.isNotBlank(yamlOptions)) {
        return optionsFileFactory.createFromConfiguration(yamlOptions);
      }
    } catch (IOException ex) {
      throw new MojoExecutionException("Failed to generate YAML option", ex);
    }
    return null;
  }

  protected static void deleteFile(Path file) {
    if (file != null && Files.exists(file)) {
      try {
        Files.delete(file);
      } catch (IOException ex) {
        throw new UncheckedIOException("Could not delete " + file, ex);
      }
    }
  }

  protected final void deployToDirectory(Path resolvedOptionsFile, Path deployDir) throws MojoExecutionException {
    var targetDeployableFile = createTargetDeployableFile(deployDir);
    var deployablePath = deployDir.relativize(targetDeployableFile).toString();
    var deployer = new FileDeployer(deployDir, resolvedOptionsFile, deployTimeoutInSeconds, deployFile, targetDeployableFile);
    deployer.deploy(deployablePath, getLog());
  }

  private final Path createTargetDeployableFile(Path deployDir) {
    return deployDir
        .resolve(deployToEngineSecurityContext)
        .resolve(deployToEngineApplication)
        .resolve(deployFile.getFileName().toString());
  }
}
