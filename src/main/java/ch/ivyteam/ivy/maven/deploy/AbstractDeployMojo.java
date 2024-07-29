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
import java.nio.file.ReadOnlyFileSystemException;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenFileFilter;

import ch.ivyteam.ivy.maven.IarPackagingMojo;
import ch.ivyteam.ivy.maven.deploy.DeployToEngineMojo.DefaultDeployOptions;
import ch.ivyteam.ivy.maven.engine.deploy.DeploymentOptionsFileFactory;
import ch.ivyteam.ivy.maven.engine.deploy.YamlOptionsFactory;
import ch.ivyteam.ivy.maven.engine.deploy.dir.FileDeployer;
import ch.ivyteam.ivy.maven.engine.deploy.dir.IvyDeployer;
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

  /**
   * The target version controls on which process model version (PMV) a project
   * is re-deployed.
   *
   * <p>
   * Matching:
   * </p>
   * <ul>
   * <li>In all cases the library identifier (group id and project/artifact id)
   * of the PMV and the project has to be equal.</li>
   * <li>If multiple PMVs match the target version then the PMV with the highest
   * library version is chosen.</li>
   * <li>If no PMV matches the target version then a new PMV is created and the
   * project is deployed to the new PMV.</li>
   * </ul>
   *
   * <p>
   * Possible values:
   * </p>
   * <ul>
   * <li><code>AUTO</code>: a project is re-deployed if the version of the PMV
   * is equal to the project's version.</li>
   * <li><code>RELEASED</code>: a project is re-deployed to the released PMV.
   * The version of the PMV and the project does not matter</li>
   * <li>Maven version range: a project is re-deployed if the version of the PMV
   * matches the given range. Some samples:
   * <ul>
   * <li><code>,</code> - Matches all version.</li>
   * <li><code>,2.5]</code> - Matches every version up to 2.5 inclusive.</li>
   * <li><code>(2.5,</code> - Matches every version from 2.5 exclusive.</li>
   * <li><code>[2.0,3.0)</code> - Matches every version from 2.0 inclusive up to
   * 3.0 exclusive.</li>
   * <li><code>2.5</code> - Matches every version from 2.5 inclusive.</li>
   * </ul>
   * </li>
   * </ul>
   */
  @Parameter(property = "ivy.deploy.target.version", defaultValue = DefaultDeployOptions.VERSION_AUTO)
  public String deployTargetVersion;

  /**
   * The target state of all process model versions (PMVs) of the deployed
   * projects.
   *
   * <ul>
   * <li><code>ACTIVE_AND_RELEASED</code>: PMVs are activated and released after
   * the deployment</li>
   * <li><code>ACTIVE</code>: PMVs are activated but not released after the
   * deployment</li>
   * <li><code>INACTIVE</code>: PMVs are neither activated nor released after
   * the deployment</li>
   * </ul>
   */
  @Parameter(property = "ivy.deploy.target.state", defaultValue = DefaultDeployOptions.STATE_ACTIVE_AND_RELEASED)
  public String deployTargetState;

  /**
   * The target file format as which the project will be deployed into the
   * process model version (PMV).
   *
   * <ul>
   * <li><code>AUTO</code>: Keep the format of the origin project file if
   * possible. Deploys IAR or ZIP projects into a ZIP process model version.
   * <br>
   * But if the target PMV already exists as expanded directory, the new version
   * will be expanded as well.</li>
   * <li><code>PACKED</code>: Enforce the deployment of a project as zipped
   * file. Normal (expanded) project directories will be compressed into a ZIP
   * during deployment.</li>
   * <li><code>EXPANDED</code>: Enforce the deployment of a project as expanded
   * file directory.<br>
   * This is recommended for projects that change the project files at runtime.
   * E.g. projects that use the Content Management (CMS) write API.<br>
   * The expanded format behaves exactly like projects deployed with Axon Ivy
   * 7.0 or older. You might choose to deploy expanded projects in order to
   * avoid {@link ReadOnlyFileSystemException} at runtime.<br>
   * <strong>Warning</strong>: Expanded projects will perform slower at runtime
   * and are therefore not recommended.</li>
   * </ul>
   */
  @Parameter(property = "ivy.deploy.target.file.format", defaultValue = DefaultDeployOptions.FILE_FORMAT_AUTO)
  public String deployTargetFileFormat;

  /**
   * <p>
   * The file that contains deployment options.
   * </p>
   *
   * Example options file content:
   *
   * <pre>
   * <code>deployTestUsers: auto
   *target:
   *  version: RELEASED
   *  state: ACTIVE_AND_RELEASED</code>
   * </pre>
   *
   * <p>
   * Inside the options file you can use property placeholders. The options file
   * may look like this:
   * </p>
   *
   * <pre>
   * <code>deployTestUsers: ${ivy.deploy.test.users}
   *target:
   *  version: AUTO
   *  state: ${ivy.deploy.target.state}</code>
   * </pre>
   *
   * <p>
   * All options in this file are optional. You only need to specify options
   * that overwrite the default behavior.
   * </p>
   * <p>
   * If configured, all Maven properties are ignored and only values in this
   * file are used.
   * </p>
   *
   * @see <a href=
   *      "https://developer.axonivy.com/doc/7.1.latest/EngineGuideHtml/administration.html#administration.deployment.directory.options">Engine
   *      Guide</a>
   */
  @Parameter(property = "ivy.deploy.options.file", required = false)
  protected Path deployOptionsFile;

  @Component
  private MavenFileFilter fileFilter;
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

  /** The name of an ivy application to which the file is deployed. */
  @Parameter(property = "ivy.deploy.engine.app")
  String deployToEngineApplication;

  public AbstractDeployMojo() {
    super();
  }

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

  protected final Path createDeployOptionsFile(DeploymentOptionsFileFactory optionsFileFactory)
          throws MojoExecutionException {
    if (deployOptionsFile != null) {
      return optionsFileFactory.createFromTemplate(deployOptionsFile, project, session, fileFilter);
    }

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

  protected final void deployToDirectory(Path resolvedOptionsFile, Path deployDir)
          throws MojoExecutionException {
    var targetDeployableFile = createTargetDeployableFile(deployDir);
    String deployablePath = deployDir.relativize(targetDeployableFile).toString();
    IvyDeployer deployer = new FileDeployer(deployDir, resolvedOptionsFile, deployTimeoutInSeconds, deployFile, targetDeployableFile);
    deployer.deploy(deployablePath, getLog());
  }

  private final Path createTargetDeployableFile(Path deployDir) {
    return deployDir
            .resolve(deployToEngineApplication)
            .resolve(deployFile.getFileName().toString());
  }
}
