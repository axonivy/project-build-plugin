/*
 * Copyright (C) 2021 Axon Ivy AG
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

package ch.ivyteam.ivy.maven.engine.deploy.dir;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

/**
 * Detects new log lines in file and forwards them to an {@link LogLineHandler}.
 * @since 6.1.0
 */
class FileLogForwarder {

  private final Path engineLog;
  private final Log mavenLog;

  private LogFileReader handler;
  private LogLineHandler logLineHandler;

  /**
   * @param engineLog the log file to watch for new lines
   * @param mavenLog the target logger
   */
  FileLogForwarder(Path engineLog, Log mavenLog, LogLineHandler handler) {
    this.engineLog = engineLog;
    this.mavenLog = mavenLog;
    this.logLineHandler = handler;
  }

  public synchronized void activate() {
    handler = new LogFileReader(engineLog);
    handler.start();
  }

  private final class LogFileReader {

    private final Path path;
    private InputStream out;
    private Thread thread;

    private LogFileReader(Path path) {
      this.path = path;
    }

    public void start() {
      try {
        out = Files.newInputStream(path);
      } catch (IOException ex) {
        mavenLog.warn("Failed to get engine deploy log content", ex);
        return;
      }
      thread = new OutputReaderThread(out);
      thread.start();
    }

    public void stop() {
      if (thread != null) {
        thread.interrupt();
      }
      if (out != null) {
        try {
          out.close();
        } catch (IOException ex) {
          // silent
        }
      }
    }
  }

  private final class OutputReaderThread extends Thread {

    private final BufferedReader reader;

    private OutputReaderThread(InputStream inputStream) {
      this.reader = new BufferedReader(new InputStreamReader(inputStream));
    }

    @Override
    public void run() {
      try {
        while (!isInterrupted()) {
          var line = reader.readLine();
          if (line == null) {
            continue;
          }
          logLineHandler.handleLine(line);
        }
      } catch (IOException ex) {
        mavenLog.warn("Failed to get engine deploy log content", ex);
        throw new RuntimeException(ex);
      } finally {
        handler.stop();
      }
    }
  }

  public synchronized void deactivate() throws MojoExecutionException {
    try {
      if (handler != null) {
        handler.stop();
      }
    } catch (Exception ex) {
      throw new MojoExecutionException("Failed to deactivate deploy log forwarder", ex);
    } finally {
      handler = null;
    }
  }

  static interface LogLineHandler {
    void handleLine(String newLogLine);
  }
}
