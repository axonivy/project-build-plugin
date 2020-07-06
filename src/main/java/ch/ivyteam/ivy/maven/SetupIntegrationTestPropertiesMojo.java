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

import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import ch.ivyteam.ivy.maven.engine.EngineControl;
import ch.ivyteam.ivy.maven.util.MavenProperties;

/**
 * <p>Shares crucial test engine internals with the forked JVM that runs tests.</p>
 * <p>The property being set is called <code>argLine</code> and classically used by the 'maven-failsafe-plugin'.</p>
 * 
 * @since 9.1
 */
@Mojo(name = SetupIntegrationTestPropertiesMojo.GOAL)
public class SetupIntegrationTestPropertiesMojo extends AbstractEngineMojo
{
  public static final String GOAL = "ivy-integration-test-properties";

  @Parameter(property = "project", required = true, readonly = true)
  MavenProject project;

  static final String FAILSAFE_ARGLINE_PROPERTY="argLine";
  
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException
  {
    var props = new MavenProperties(project, getLog());
    String jmvArgProps = asJvmArgs(props, 
      EngineControl.Property.TEST_ENGINE_URL, 
      EngineControl.Property.TEST_ENGINE_LOG, 
      DeployToTestEngineMojo.Property.TEST_ENGINE_APP
    );
    props.set(FAILSAFE_ARGLINE_PROPERTY, jmvArgProps);
  }

  private static String asJvmArgs(MavenProperties store, String... keys)
  {
    return Arrays.stream(keys)
      .map(key -> "-D"+key+"="+store.get(key))
      .collect(Collectors.joining(" "));
  }

}
