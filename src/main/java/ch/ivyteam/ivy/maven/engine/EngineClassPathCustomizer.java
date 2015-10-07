/*
 * Copyright (C) 2015 AXON IVY AG
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

package ch.ivyteam.ivy.maven.engine;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Changes the default class path of an ivyEngine.
 * 
 * @author Reguel Wermelinger
 * @since 06.11.2014
 */
class EngineClassPathCustomizer
{
  private Map<String, File> replacements = new HashMap<>();
  private List<String> filters = new ArrayList<>();
  
  void registerReplacement(String originalJarNamePrefix, File replacement)
  {
    replacements.put(originalJarNamePrefix, replacement);
  }
  
  void registerFilter(String originalJarNamePrefix)
  {
    filters.add(originalJarNamePrefix);
  }

  List<File> customizeClassPath(List<File> ivyEngineClassPathFiles)
  {
    List<File> customClassPathFiles = new ArrayList<>();
    for(File file : ivyEngineClassPathFiles)
    {
      String fileName = file.getName();
      if (matchesFilter(fileName))
      {
        continue;
      }
      
      for(String replacePrefix : replacements.keySet())
      {
        if (fileName.startsWith(replacePrefix))
        {
          file = replacements.get(replacePrefix);
        }
      }
  
      customClassPathFiles.add(file);
    }
    return customClassPathFiles;
  }
  
  private boolean matchesFilter(String fileName)
  {
    for (String filterPrefix : filters)
    {
      if (fileName.startsWith(filterPrefix))
      {
        return true;
      }
    }
    return false;
  }

}