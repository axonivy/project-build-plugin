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

package ch.ivyteam.ivy.maven.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.maven.project.MavenProject;

import ch.ivyteam.ivy.maven.engine.MavenProjectBuilderProxy.Result;

/**
 * @author Reguel Wermelinger
 * @since 6.0.3
 */
public class CompilerResult
{
  public static void store(Map<String, Object> result, MavenProject project) throws IOException
  {
    Properties properties = new Properties();
    for(Entry<String, Object> entry : result.entrySet())
    {
      properties.setProperty(entry.getKey(), entry.getValue().toString());
    }
    File propertyFile = new SharedFile(project).getCompileResultProperties();
    try(FileOutputStream fos = new FileOutputStream(propertyFile))
    {
      properties.store(fos, "ivy project build results");
    }
  }
  
  public static CompilerResult load(MavenProject project) throws IOException
  {
    File propertyFile = new SharedFile(project).getCompileResultProperties();
    Properties compileResults = new Properties();
    try(FileInputStream fis = new FileInputStream(propertyFile))
    {
      compileResults.load(fis);
    }
    return new CompilerResult(compileResults);
  }
  
  private final Properties result;
  
  public CompilerResult(Properties result)
  {
    this.result = result;
  }
  
  public String getTestOutputDirectory()
  {
    if (!result.stringPropertyNames().contains(Result.TEST_OUTPUT_DIR))
    {
      return null;
    }
    return result.getProperty(Result.TEST_OUTPUT_DIR);
  }
  
}