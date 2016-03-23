/*
 * Copyright (C) 2016 AXON IVY AG
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

package ch.ivyteam.ivy.maven.engine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

/**
 * Sends commands like start, stop to the ivy Engine
 */
public class EngineControl
{
  private static final String SERVER_MAIN_CLASS = "ch.ivyteam.ivy.server.ServerLauncher";
  private EngineMojoContext context;

  public enum Command
  {
    start, stop
  }

  public EngineControl(EngineMojoContext context)
  {
    this.context = context;

  }

  public void start()
  {
    executeCommand(Command.start);
  }

  public void stop()
  {
    executeCommand(Command.stop);
  }

  public void executeCommand(Command command)
  {
    try
    {
      
      String classpath = context.engineClasspathJar;
      if (StringUtils.isNotBlank(context.vmOptions.additionalClasspath))
      {
        classpath += File.pathSeparator + context.vmOptions.additionalClasspath;
      }
      
      ProcessBuilder builder = new ProcessBuilder("java", "-classpath", classpath, "-Xmx" + context.vmOptions.maxmem, SERVER_MAIN_CLASS, command.toString());
      
      if (StringUtils.isNotBlank(context.vmOptions.additionalVmOptions))
      {
        builder.command().add(context.vmOptions.additionalVmOptions);
      }
      
      builder.directory(context.engineDirectory);
      builder.redirectErrorStream(true);
      builder.redirectOutput(context.engineLogFile);
      context.properties.setMavenProperty("test.engine.log", context.engineLogFile.getAbsolutePath());
      context.log.info("Executing command " + command + " against Axon.ivy Engine in folder: " + context.engineDirectory);
      builder.start();

      if (command == Command.start)
      {
        waitForEngineStart();
      }

    }
    catch (Exception ex)
    {
      throw new RuntimeException("Cannot start engine", ex);
    }

  }

  /*
   * Preliminary implementation
   */
  private void waitForEngineStart() throws InterruptedException
  {
    String url;
    int i = 0;
    while ((url = checkForText()) == null)
    {
      Thread.sleep(1000);
      i++;
      if (i > 30)
      {
        context.log.error("Timeout while starting engine");
        break;
      }
    }
    url = "http://" + url + "/ivy/";
    context.log.info("Axon.ivy Engine runs on : " + url);

    context.properties.setMavenProperty("test.engine.url", url);
  }
  
  private String checkForText()
  {
    try (BufferedReader br = new BufferedReader(new FileReader(context.engineLogFile)))
    {
      String line;
      while ((line = br.readLine()) != null)
      {
        if (line.contains("info page of Axon.ivy Engine"))
        {
          String url = StringUtils.substringBetween(line, "http://", "/");
          return url;
        }
      }
    }
    catch (IOException ex)
    {
      throw new RuntimeException("Cannot read log file: " + context.engineLogFile, ex);
    }

    return null;
  }
}
