/*
 * Copyright (C) 2021 Axon Ivy AG
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

package ch.ivyteam.ivy.maven.generate;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import ch.ivyteam.ivy.maven.compile.AbstractEngineInstanceMojo;
import ch.ivyteam.ivy.maven.engine.MavenProjectBuilderProxy;

@Mojo(name = OpenApiClientGeneratorMojo.GOAL, requiresDependencyResolution=ResolutionScope.COMPILE)
public class OpenApiClientGeneratorMojo extends AbstractEngineInstanceMojo
{
  public static final String GOAL = "generate-openapi-client";

  @Override
  protected void engineExec(MavenProjectBuilderProxy projectBuilder) throws Exception
  {
    Map<String, Object> opts = new HashMap<>();
    List<File> clients = projectBuilder.generateClient(project.getBasedir(), opts);
    List<String> simpleNames = clients.stream().map(File::getName).collect(Collectors.toList());
    getLog().info("Created JAX-RS clients: "+simpleNames);
  }
}
