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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteResultHandler;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.exec.ShutdownHookProcessDestroyer;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;

import ch.ivyteam.ivy.maven.engine.EngineClassLoaderFactory.OsgiDir;
import ch.ivyteam.ivy.maven.test.StartTestEngineMojo;
import ch.ivyteam.ivy.maven.util.stream.LineOrientedOutputStreamRedirector;

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

  public Executor start() throws Exception {
    CommandLine startCmd = toEngineCommand(Command.start);
    context.log.info("Start Axon Ivy Engine in folder: " + context.engineDirectory);

    Executor executor = createEngineExecutor();
    executor.setStreamHandler(createEngineLogStreamForwarder(logLine -> findStartEngineUrl(logLine)));
    executor.setWatchdog(new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT));
    executor.setProcessDestroyer(new ShutdownHookProcessDestroyer());
    executor.execute(startCmd, asynchExecutionHandler());
    waitForEngineStart(executor);
    return executor;
  }

  public void stop() throws Exception {
    CommandLine stopCmd = toEngineCommand(Command.stop);
    context.log.info("Stopping Axon Ivy Engine in folder: " + context.engineDirectory);

    executeSynch(stopCmd);
    waitFor(() -> EngineState.STOPPED == state(), context.timeoutInSeconds, TimeUnit.SECONDS);
  }

  EngineState state() {
    CommandLine statusCmd = toEngineCommand(Command.status);
    String engineOutput = executeSynch(statusCmd);
    return parseState(engineOutput);
  }

  private CommandLine toEngineCommand(Command command) {
    String classpath = context.engineClasspathJarPath;
    if (StringUtils.isNotBlank(context.vmOptions.additionalClasspath)) {
      classpath += File.pathSeparator + context.vmOptions.additionalClasspath;
    }

    File osgiDir = new File(context.engineDirectory, OsgiDir.INSTALL_AREA);

    CommandLine cli = new CommandLine(new File(getJavaExec()))
            .addArgument("-classpath").addArgument(classpath)
            .addArgument("-Divy.engine.testheadless=true")
            .addArgument("-Djava.awt.headless=true")
            .addArgument("-Dosgi.install.area=" + osgiDir.getAbsolutePath());

    if (StringUtils.isNotBlank(context.vmOptions.additionalVmOptions)) {
      cli.addArguments(context.vmOptions.additionalVmOptions, false);
    }
    EngineModuleHints.loadFromJvmOptionsFile(context, cli);

    cli.addArgument("org.eclipse.equinox.launcher.Main")
            .addArgument("-application").addArgument("ch.ivyteam.ivy.server.exec.engine")
            .addArgument(command.toString());
    return cli;
  }

  private Executor createEngineExecutor() {
    DefaultExecutor executor = new DefaultExecutor();
    executor.setWorkingDirectory(context.engineDirectory);
    return executor;
  }

  private PumpStreamHandler createEngineLogStreamForwarder(Consumer<String> logLineHandler)
          throws FileNotFoundException {
    OutputStream output = getEngineLogTarget();
    OutputStream engineLogStream = new LineOrientedOutputStreamRedirector(output) {
      @Override
      protected void processLine(byte[] b) throws IOException {
        super.processLine(b); // write file log
        String line = new String(b);
        context.log.debug("engine: " + line);
        if (logLineHandler != null) {
          logLineHandler.accept(line);
        }
      }
    };
    PumpStreamHandler streamHandler = new PumpStreamHandler(engineLogStream, System.err) {
      @Override
      public void stop() throws IOException {
        super.stop();
        engineLogStream.close(); // we opened the stream - we're responsible to
                                 // close it!
      }
    };
    return streamHandler;
  }

  private OutputStream getEngineLogTarget() throws FileNotFoundException {
    if (context.engineLogFile == null) {
      context.log.info("Do not forward engine output to a persistent location");
      return new ByteArrayOutputStream();
    }

    context.properties.setMavenProperty(Property.TEST_ENGINE_LOG, context.engineLogFile.getAbsolutePath());
    context.log.info("Forwarding engine logs to: " + context.engineLogFile.getAbsolutePath());
    return new FileOutputStream(context.engineLogFile.getAbsolutePath());
  }

  private String getJavaExec() {
    String javaExec = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
    context.log.debug("Using Java exec from path: " + javaExec);
    return javaExec;
  }

  private void findStartEngineUrl(String newLine) {
    var lowercaseNewLine = StringUtils.lowerCase(newLine);
    if (lowercaseNewLine.contains("info page of axon.ivy engine") || // 9.1.1
                                                                     // and
                                                                     // earlier
            lowercaseNewLine.contains("info page of axon ivy engine")) // 9.2.0
                                                                       // and
                                                                       // newer
    {
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

  static String evaluateIvyContextFromUrl(String location) {
    return StringUtils.substringBefore(StringUtils.removeStart(location, "/"), "sys");
  }

  private void waitForEngineStart(Executor executor) throws Exception {
    int i = 0;
    while (!engineStarted.get()) {
      Thread.sleep(1_000);
      i++;
      if (!executor.getWatchdog().isWatching()) {
        throw new RuntimeException("Engine start failed unexpected.");
      }
      if (i > context.timeoutInSeconds) {
        throw new TimeoutException("Timeout while starting engine " + context.timeoutInSeconds + " [s].\n"
                + "Check the engine log for details or increase the timeout property '"
                + StartTestEngineMojo.IVY_ENGINE_START_TIMEOUT_SECONDS + "'");
      }
    }
    context.log.info("Engine started after " + i + " [s]");
  }

  private ExecuteResultHandler asynchExecutionHandler() {
    return new ExecuteResultHandler() {
      @Override
      public void onProcessFailed(ExecuteException ex) {
        throw new RuntimeException("Engine operation failed.", ex);
      }

      @Override
      public void onProcessComplete(int exitValue) {
        context.log.info("Engine process stopped.");
      }
    };
  }

  /**
   * Run a short living engine command where we expect a process failure as the
   * engine invokes <code>System.exit(-1)</code>.
   * @param statusCmd
   * @return the output of the engine command.
   */
  private String executeSynch(CommandLine statusCmd) {
    String engineOutput = null;
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream, System.err);
    Executor executor = createEngineExecutor();
    executor.setStreamHandler(streamHandler);
    executor.setExitValue(-1);
    try {
      executor.execute(statusCmd);
    } catch (IOException ex) { // expected!
    } finally {
      engineOutput = outputStream.toString();
      IOUtils.closeQuietly(outputStream);
    }
    return engineOutput;
  }

  private EngineState parseState(String engineOut) {
    for (String line : StringUtils.split(engineOut, '\n')) {
      try {
        line = StringUtils.strip(line, "\r");
        return EngineState.valueOf(line);
      } catch (Exception ex) { // output can contain log4j configuration outputs
                               // -> ignore them!
      }
    }
    context.log.error("Failed to evaluate engine state of engine in directory " + context.engineDirectory);
    return null;
  }

  private static long waitFor(Supplier<Boolean> condition, long duration, TimeUnit unit) throws Exception {
    StopWatch watch = new StopWatch();
    watch.start();
    long timeout = unit.toMillis(duration);

    while (!condition.get()) {
      Thread.sleep(1_000);
      if (watch.getTime() > timeout) {
        throw new TimeoutException("Condition not reached in " + duration + " " + unit);
      }
    }

    return watch.getTime();
  }

}
