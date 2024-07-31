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

package ch.ivyteam.ivy.maven.engine;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;

import ch.ivyteam.ivy.maven.engine.EngineClassLoaderFactory.OsgiDir;
import ch.ivyteam.ivy.maven.test.StartTestEngineMojo;

/**
 * Sends commands like start, stop to the ivy Engine
 */
public class EngineControl {
  public static interface Property {
    String TEST_ENGINE_URL = "test.engine.url";
    String TEST_ENGINE_LOG = "test.engine.log";
  }

  /**
   * mvn-plugin implementation of: ch.ivyteam.server.ServerState
   */
  public enum EngineState {
    STOPPED, STARTING, RUNNING, STOPPING, UNREGISTERED, FAILED;
  }

  private EngineMojoContext context;
  private AtomicBoolean engineStarted = new AtomicBoolean(false);


  private enum Command {
    start, stop, status
  }

  public EngineControl(EngineMojoContext context) {
    this.context = context;
  }

  public Process start() throws Exception {
    var builder = toProcessBuilder(Command.start);
    context.log.info("Start Axon Ivy Engine in folder: " + context.engineDirectory);
    var redirectTo = redirectEngineLog(builder);

    var process = builder.start();

    var inputStream = redirectTo == null ? process.getInputStream() : Files.newInputStream(redirectTo);
    try {
      var streamHandler = new ProcessStreamHandler(inputStream);
      streamHandler.start();

      process.onExit()
        .thenAccept(p -> context.log.info("Engine process stopped."))
        .exceptionally(ex -> {
          throw new RuntimeException("Engine operation failed.", ex);
      });

      waitForEngineStarted();
      return process;
    } finally {
      if (redirectTo != null) {
        inputStream.close();
      }
    }
  }

  public void stop() throws Exception {
    var builder = toProcessBuilder(Command.stop);
    context.log.info("Stopping Axon Ivy Engine in folder: " + context.engineDirectory);
    executeSynch(builder);
    waitFor(() -> EngineState.STOPPED == state(), context.timeoutInSeconds, TimeUnit.SECONDS);
  }

  EngineState state() {
    var builder = toProcessBuilder(Command.status);
    try {
      var engineOutput = executeSynch(builder);
      return parseState(engineOutput);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  private ProcessBuilder toProcessBuilder(Command command) {
    var cmds = new ArrayList<String>();

    var classpath = context.engineClasspathJarPath.toString();
    if (StringUtils.isNotBlank(context.vmOptions.additionalClasspath())) {
      classpath += File.pathSeparator + context.vmOptions.additionalClasspath();
    }

    var osgiDir = context.engineDirectory.resolve(OsgiDir.INSTALL_AREA);

    cmds.add(getJavaExec());
    cmds.add("-classpath");
    cmds.add(classpath);
    cmds.add("-Divy.engine.testheadless=true");
    cmds.add("-Djava.awt.headless=true");
    cmds.add("-Dosgi.install.area=" + osgiDir.toAbsolutePath());

    if (StringUtils.isNotBlank(context.vmOptions.additionalVmOptions())) {
      var multipleCommands = context.vmOptions.additionalVmOptions().split(" ");
      Arrays.stream(multipleCommands).forEach(o -> cmds.add(o));
    }
    new EngineModuleHints(context.engineDirectory, context.log).asStream().forEach(option -> cmds.add(option));

    cmds.add("org.eclipse.equinox.launcher.Main");
    cmds.add("-application");
    cmds.add("ch.ivyteam.ivy.server.exec.engine");
    cmds.add(command.toString());

    var builder = new ProcessBuilder(cmds);
    builder.directory(context.engineDirectory.toFile());

    builder.redirectErrorStream(true);

    return builder;
  }

  private Path redirectEngineLog(ProcessBuilder builder) {
    if (context.engineLogFile == null) {
      context.log.info("Do not forward engine output to a persistent location");
      return null;
    }

    var logFile = context.engineLogFile;
    var logFilePath = logFile.toAbsolutePath().toString();
    context.properties.setMavenProperty(Property.TEST_ENGINE_LOG, logFilePath);
    context.log.info("Forwarding engine logs to: " + logFilePath);
    builder.redirectOutput(java.lang.ProcessBuilder.Redirect.to(logFile.toFile()));
    return logFile;
  }

  private String getJavaExec() {
    String javaExec = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
    context.log.debug("Using Java exec from path: " + javaExec);
    return javaExec;
  }

  static String evaluateIvyContextFromUrl(String location) {
    return StringUtils.substringBefore(StringUtils.removeStart(location, "/"), "sys");
  }

  private void waitForEngineStarted() throws Exception {
    int i = 0;
    while (!engineStarted.get()) {
      Thread.sleep(500);
      i++;
      if (i / 2 > context.timeoutInSeconds) {
        throw new TimeoutException("Timeout while starting engine " + context.timeoutInSeconds + " [s].\n"
                + "Check the engine log for details or increase the timeout property '"
                + StartTestEngineMojo.IVY_ENGINE_START_TIMEOUT_SECONDS + "'");
      }
    }
    context.log.info("Engine started after " + i / 2 + " [s]");
  }

  private String executeSynch(ProcessBuilder statusCmd) throws IOException {
    var process = statusCmd.start();
    var in = process.getInputStream();
    try {
      process.waitFor();
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(ex);
    }
    try {
      return new String(in.readAllBytes());
    } finally {
      in.close();
    }
  }

  private EngineState parseState(String engineOut) {
    var state = engineOut.lines()
            .map(line -> parse(line))
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    if (state == null) {
      context.log.error("Failed to evaluate engine state of engine in directory " + context.engineDirectory);
    }
    return state;
  }

  private EngineState parse(String line) {
    try {
      return EngineState.valueOf(line.strip());
    } catch (Exception ex) {
      // output can contain log4j configuration outputs -> ignore them!
      return null;
    }
  }

  private static long waitFor(Supplier<Boolean> condition, long duration, TimeUnit unit) throws Exception {
    StopWatch watch = new StopWatch();
    watch.start();
    long timeout = unit.toMillis(duration);

    while (!condition.get()) {
      Thread.sleep(500);
      if (watch.getTime() > timeout) {
        throw new TimeoutException("Condition not reached in " + duration + " " + unit);
      }
    }
    return watch.getTime();
  }


  private final class ProcessStreamHandler {

    private final InputStream out;
    private Thread thread;

    private ProcessStreamHandler(InputStream out) {
      this.out = out;
    }

    public void start() {
      thread = new OutputReaderThread(this, out);
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

    private final ProcessStreamHandler handler;
    private final BufferedReader reader;

    private OutputReaderThread(ProcessStreamHandler handler, InputStream inputStream) {
      this.handler = handler;
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
          context.log.debug("engine: " + line);
          findStartEngineUrl(line);
          if (engineStarted.get()) {
            return;
          }
        }
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      } finally {
        handler.stop();
      }
    }

    private void findStartEngineUrl(String newLine) {
      var lowercaseNewLine = StringUtils.lowerCase(newLine);
      if (lowercaseNewLine.contains("info page of axon ivy engine")) {
        if (!engineStarted.get()) {
          var url = "http://" + StringUtils.substringBetween(newLine, "http://", "/") + "/";
          url += evaluateDefaultContext(url);
          context.log.info("Axon Ivy Engine runs on : " + url);
          context.properties.setMavenProperty(Property.TEST_ENGINE_URL, url);
          engineStarted.set(true);
        }
      }
    }

    private String evaluateDefaultContext(String url) {
      context.log.debug("Call '" + url + "' to evaluate the default context");
      var client = HttpClient.newBuilder().followRedirects(Redirect.NEVER).build();
      var request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
      var result = "ivy/";
      try {
        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
        result = response.headers().firstValue("Location").map(location -> {
          context.log.debug("'" + url + "' returns location header: " + location);
          String defaultContext = evaluateIvyContextFromUrl(location);
          context.log.debug("Evalutate '" + defaultContext + "' as default context");
          return defaultContext;
        }).orElse(result);
      } catch (IOException | InterruptedException ex) {
        context.log.warn("Couldn't evaluate default context of engine > use 'ivy/'");
      }
      return result;
    }
  }
}
