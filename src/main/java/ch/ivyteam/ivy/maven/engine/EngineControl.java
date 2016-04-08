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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteResultHandler;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.exec.ShutdownHookProcessDestroyer;
import org.apache.commons.lang3.StringUtils;

import ch.ivyteam.ivy.maven.StartTestEngineMojo;
import ch.ivyteam.ivy.maven.util.stream.LineOrientedOutputStreamRedirector;

/**
 * Sends commands like start, stop to the ivy Engine
 */
public class EngineControl
{
  public static interface Property
  {
    public static final String TEST_ENGINE_URL = "test.engine.url";
    public static final String TEST_ENGINE_LOG = "test.engine.log";
  }
  
  private static final String SERVER_MAIN_CLASS = "ch.ivyteam.ivy.server.ServerLauncher";
  private EngineMojoContext context;
  private AtomicBoolean engineStarted = new AtomicBoolean(false);

  public enum Command
  {
    start, stop
  }

  public EngineControl(EngineMojoContext context)
  {
    this.context = context;

  }

  public Executor start() throws Exception
  {
    Executor executor = executeCommand(Command.start);
    waitForEngineStart(executor);
    return executor;
  }

  public void stop() throws Exception
  {
    executeCommand(Command.stop);
  }
  
  Executor executeCommand(Command command) throws IOException
  {
    CommandLine cli = toEngineCommand(command);
    context.log.info("Executing command " + command + " against Axon.ivy Engine in folder: " + context.engineDirectory);
  
    DefaultExecutor executor = new DefaultExecutor();
    executor.setWorkingDirectory(context.engineDirectory);
    executor.setWatchdog(new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT)); 
    executor.setProcessDestroyer(new ShutdownHookProcessDestroyer());
    executor.setStreamHandler(getEngineLogStreamForwarder());
    executor.execute(cli, asynchExecutionHandler());
    return executor;
  }
  
  private CommandLine toEngineCommand(Command command)
  {
    String classpath = context.engineClasspathJar;
    if (StringUtils.isNotBlank(context.vmOptions.additionalClasspath))
    {
      classpath += File.pathSeparator + context.vmOptions.additionalClasspath;
    }
    
    CommandLine cli = new CommandLine(new File(getJavaExec()))
            .addArgument("-classpath").addArgument(classpath)
            .addArgument(SERVER_MAIN_CLASS)
            .addArgument(command.toString());
    if (StringUtils.isNotBlank(context.vmOptions.additionalVmOptions))
    {
      cli.addArgument(context.vmOptions.additionalVmOptions);
    }
    
    return cli;
  }

  private PumpStreamHandler getEngineLogStreamForwarder() throws FileNotFoundException
  {
    context.properties.setMavenProperty(Property.TEST_ENGINE_LOG, context.engineLogFile.getAbsolutePath());
    context.log.info("Forwarding engine logs to: "+context.engineLogFile.getAbsolutePath());
    
    OutputStream fileLogStream = new FileOutputStream(context.engineLogFile.getAbsolutePath());
    OutputStream engineLogStream = new LineOrientedOutputStreamRedirector(fileLogStream)
    {
      @Override
      protected void processLine(byte[] b) throws IOException
      {
          super.processLine(b); // write file log
          handleEngineLogLine(new String(b));
      }
    };
    PumpStreamHandler streamHandler = new PumpStreamHandler(engineLogStream, System.err)
    {
      @Override
      public void stop() throws IOException
      {
        super.stop();
        engineLogStream.close(); // we opened the stream - we're responsible to close it!
      }
    };
    return streamHandler;
  }
  
  private void handleEngineLogLine(String newLine)
  {
    context.log.debug("engine: "+newLine);
    findStartEngineUrl(newLine);
  }

  private void findStartEngineUrl(String newLine)
  {
    if (newLine.contains("info page of Axon.ivy Engine") && !engineStarted.get())
    {
      String url = StringUtils.substringBetween(newLine, "http://", "/");
      url = "http://" + url + "/ivy/";
      context.log.info("Axon.ivy Engine runs on : " + url);
      context.properties.setMavenProperty(Property.TEST_ENGINE_URL, url);
      engineStarted.set(true);
    }
  }

  private void waitForEngineStart(Executor executor) throws Exception
  {
    int i = 0;
    while (!engineStarted.get() && executor.getWatchdog().isWatching())
    {
      Thread.sleep(1_000);
      i++;
      if (!executor.getWatchdog().isWatching())
      {
        throw new RuntimeException("Engine start failed unexpected.");
      }
      if (i > context.timeoutInSeconds)
      {
        throw new TimeoutException("Timeout while starting engine " + context.timeoutInSeconds + " [s].\n"
                + "Check the engine log for details or increase the timeout property '"+StartTestEngineMojo.IVY_ENGINE_START_TIMEOUT_SECONDS+"'");
      }
    }
    context.log.info("Engine started after " + i + " [s]");
  }

  private String getJavaExec()
  {
    String javaExec = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
    context.log.debug("Using Java exec from path: " + javaExec);
    return javaExec;
  }
  
  private static ExecuteResultHandler asynchExecutionHandler()
  {
    return new ExecuteResultHandler()
      {
        @Override
        public void onProcessFailed(ExecuteException ex)
        {
          throw new RuntimeException("Engine start failed.", ex);
        }
        
        @Override
        public void onProcessComplete(int exitValue)
        {
        }
      };
  }
}
