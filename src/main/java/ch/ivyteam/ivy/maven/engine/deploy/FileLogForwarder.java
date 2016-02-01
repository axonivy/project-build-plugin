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

class FileLogForwarder
{
  private final File engineLog;
  private final Log mavenLog;
  
  private FileAlterationMonitor monitor;

  FileLogForwarder(File engineLog, Log mavenLog)
  {
    this.engineLog = engineLog;
    this.mavenLog = mavenLog;
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
      monitor.stop(0);
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
    long lastReadPosition = 0;
    
    @Override
    public void onFileChange(File log)
    {
      try(RandomAccessFile readableLog = new RandomAccessFile(log, "r");)
      {
        readableLog.seek(lastReadPosition);
        logNewLines(readableLog);
        lastReadPosition = readableLog.getFilePointer();
      }
      catch (Exception ex)
      {
        mavenLog.warn("Failed to get engine deploy log content", ex);
      }
    }

    private void logNewLines(RandomAccessFile readableLog) throws IOException
    {
      String line = null;
      while((line = readableLog.readLine()) != null)
      {
        mavenLog.info(line);
      }
    }
  }
  
}