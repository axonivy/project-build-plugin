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

package ch.ivyteam.ivy.maven.engine.deploy.dir;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

/**
 * Deploys ivy deployables to an AXON.IVY engine. Deployables can be:
 * <ul>
 * <li>An *.iar project file</li>
 * <li>An *.zip full application file containing a set of projects</li>
 * </ul>
 * @since 6.1.0
 */
public interface IvyDeployer
{
  /**
   * @param deployablePath the path to the deployable (uploaded). Must be relative the engines deploy directory.
   * @param log mojo log
   * @throws MojoExecutionException if deployment fails
   */
  void deploy(String deployablePath, Log log) throws MojoExecutionException;
}
