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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.Properties;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.Rule;
import org.junit.Test;

import ch.ivyteam.ivy.maven.engine.EngineControl;

public class TestShareIntegrationTestPropertiesMojo
{
  @Rule
  public ProjectMojoRule<SetupIntegrationTestPropertiesMojo> setupProps = new ProjectMojoRule<>(
          new File("src/test/resources/base"), SetupIntegrationTestPropertiesMojo.GOAL);
  
  @Test
  public void shareFailsafeProps() throws MojoExecutionException, MojoFailureException
  {
    Properties props = setupProps.project.getProperties();
    String argLineBefore = (String)props.get(SetupIntegrationTestPropertiesMojo.FAILSAFE_ARGLINE_PROPERTY);
    assertThat(argLineBefore).isNullOrEmpty();
    
    props.setProperty(EngineControl.Property.TEST_ENGINE_URL, "http://127.0.0.1:9999");
    props.setProperty(EngineControl.Property.TEST_ENGINE_LOG, "/var/logs/ivy.log");
    props.setProperty(DeployToTestEngineMojo.Property.TEST_ENGINE_APP, "myTstApp");
    
    SetupIntegrationTestPropertiesMojo mojo = setupProps.getMojo();
    mojo.execute();
    String argLine = (String)props.get(SetupIntegrationTestPropertiesMojo.FAILSAFE_ARGLINE_PROPERTY);
    assertThat(argLine).isEqualTo("-Dtest.engine.url=http://127.0.0.1:9999 -Dtest.engine.log=/var/logs/ivy.log -Dtest.engine.app=myTstApp");
  }
  
}
