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
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    
    Path workspace = Files.createTempDirectory("myWs");
    
    Path reactorProject = workspace.resolve("myReactorProject");
    Files.createDirectories(reactorProject);
    Path superPom = reactorProject.resolve("pom.xml");
    Files.copy(new File("src/test/resources/base/pom.xml").toPath(), superPom);
    
    Path builtTarget = workspace.resolve("myPackedReactorProject").resolve("target");
    Files.createDirectories(builtTarget);
    Files.createFile(builtTarget.resolve("alreadyPacked.iar"));
    
    File appZip = mojo.createFullAppZip(List.of(
            Files.createFile(workspace.resolve("demo.iar")).toFile(), 
            Files.createFile(workspace.resolve("demoTest.iar")).toFile(),
            reactorProject.toFile(),
            builtTarget.getParent().toFile()
    ));
    assertThat(appZip.getName()).isEqualTo("myApp-app.zip");
    assertThat(DeployToTestEngineMojo.findPackedIar(builtTarget.getParent().toFile())).isPresent();
    assertThat(getRootFiles(appZip))
      .containsOnly("demo.iar", "demoTest.iar", "myReactorProject", "alreadyPacked.iar");
  }

  private static List<String> getRootFiles(File zip) throws IOException
  {
    java.net.URI uri = java.net.URI.create("jar:" + zip.toPath().toUri());
    try (FileSystem zipFsCr = FileSystems.newFileSystem(uri, Collections.emptyMap()))
    {
      Path root = zipFsCr.getPath("/");
      try(Stream<Path> paths = Files.list(root))
      {
        return paths
          .map(child -> child.getFileName().toString())
          .collect(Collectors.toList());
      }
    }
  }

  @Rule
  public ProjectMojoRule<DeployToTestEngineMojo> deploy = new ProjectMojoRule<>(
          new File("src/test/resources/base"), DeployToTestEngineMojo.TEST_GOAL);

}
