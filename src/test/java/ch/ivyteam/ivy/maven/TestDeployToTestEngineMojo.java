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
package ch.ivyteam.ivy.maven;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.codehaus.plexus.archiver.ArchiverException;
import org.junit.Rule;
import org.junit.Test;

public class TestDeployToTestEngineMojo
{
  @Test
  public void autoAppZip() throws ArchiverException, IOException
  {
    DeployToTestEngineMojo mojo = deploy.getMojo();
    mojo.deployToEngineApplication = "myApp";
    
    File appZip = mojo.createFullAppZip(List.of(
            Files.createTempFile("demo", ".iar").toFile(), 
            Files.createTempFile("demoTest", ".iar").toFile()));
    assertThat(appZip.getName()).isEqualTo("myApp-app.zip");
  }

  @Rule
  public ProjectMojoRule<DeployToTestEngineMojo> deploy = new ProjectMojoRule<>(
          new File("src/test/resources/base"), DeployToTestEngineMojo.TEST_GOAL);

}
