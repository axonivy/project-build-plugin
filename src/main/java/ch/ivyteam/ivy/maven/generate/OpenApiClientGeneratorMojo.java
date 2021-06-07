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

import java.util.HashMap;
import java.util.Map;

import org.apache.maven.plugins.annotations.Mojo;

import ch.ivyteam.ivy.maven.compile.AbstractEngineInstanceMojo;
import ch.ivyteam.ivy.maven.engine.MavenProjectBuilderProxy;

@Mojo(name = OpenApiClientGeneratorMojo.GOAL)
public class OpenApiClientGeneratorMojo extends AbstractEngineInstanceMojo
{
  public static final String GOAL = "generate-openapi-client";

  @Override
  protected void engineExec(MavenProjectBuilderProxy projectBuilder) throws Exception
  {
    Map<String, Object> opts = new HashMap<>();
    projectBuilder.generateClient(project.getBasedir(), opts);
  }
}
