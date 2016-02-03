/*
 * Copyright (C) 2016 AXON IVY AG
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

package ch.ivyteam.ivy.maven.engine.deploy;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

/**
 * Detects new log lines in file and forwards them to an {@link LogLineHandler}.
 * @since 6.1.0
 */
class FileLogForwarder
{
  private final File engineLog;
  private final Log mavenLog;
  
  private FileAlterationMonitor monitor;
  private LogLineHandler logLineHandler;

  /**
   * @param engineLog the log file to watch for new lines
   * @param mavenLog the target logger
   */
  FileLogForwarder(File engineLog, Log mavenLog, LogLineHandler handler)
  {
    this.engineLog = engineLog;
    this.mavenLog = mavenLog;
    this.logLineHandler = handler;
  }

  public synchronized void activate() throws MojoExecutionException
  {   
    IOFileFilter logFilter = FileFilterUtils.and(
            FileFilterUtils.fileFileFilter(),
            FileFilterUtils.nameFileFilter(engineLog.getName()));
    FileAlterationObserver fileObserver = new FileAlterationObserver(engineLog.getParent(), logFilter);
    fileObserver.addListener(new LogModificationListener());
    monitor = new FileAlterationMonitor(100);
    monitor.addObserver(fileObserver);
    try
    {
      monitor.start();
    }
    catch (Exception ex)
    {
      throw new MojoExecutionException("Failed to activate deploy log forwarder", ex);
    }
  }
  
  public synchronized void deactivate() throws MojoExecutionException
  {
    try
    {
      if (monitor != null)
      {
        monitor.stop(0);
      }
    }
    catch (Exception ex)
    {
      throw new MojoExecutionException("Failed to deactivate deploy log forwarder", ex);
    }
    finally
    {
      monitor = null;
    }
  }

  private class LogModificationListener extends FileAlterationListenerAdaptor
  {
    private long lastReadPosition = 0;
    
    @Override
    public void onFileChange(File log)
    {
      try(RandomAccessFile readableLog = new RandomAccessFile(log, "r");)
      {
        readableLog.seek(lastReadPosition);
        readNewLines(readableLog);
        lastReadPosition = readableLog.getFilePointer();
      }
      catch (Exception ex)
      {
        mavenLog.warn("Failed to get engine deploy log content", ex);
      }
    }

    private void readNewLines(RandomAccessFile readableLog) throws IOException
    {
      String line = null;
      while((line = readableLog.readLine()) != null)
      {
        logLineHandler.handleLine(line);
      }
    }
  }
  
  static interface LogLineHandler
  {
    void handleLine(String newLogLine);
  }
  
}