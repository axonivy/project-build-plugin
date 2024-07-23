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

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ch.ivyteam.ivy.maven.log.LogCollector;
import ch.ivyteam.ivy.maven.log.LogCollector.LogEntry;

class TestFileLogForwarder {

  @TempDir
  Path tempDir;

  @Test
  void fileToMavenLog() throws Exception {
    var fakeEngineLog = tempDir.resolve("myProject.iar.deploymentLog");
    Files.createFile(fakeEngineLog);
    var mavenLog = new LogCollector();
    var logForwarder = new FileLogForwarder(fakeEngineLog, mavenLog, new EngineLogLineHandler(mavenLog));
    var log = new FakeLogger(fakeEngineLog);

    try {
      logForwarder.activate();

      log.write("WARNING: starting");
      await().untilAsserted(() -> assertThat(mavenLog.getWarnings()).hasSize(1));
      LogEntry firstEntry = mavenLog.getWarnings().get(mavenLog.getWarnings().size() - 1);
      assertThat(firstEntry.toString()).isEqualTo(" ENGINE: starting");

      log.write("WARNING: finished");
      await().untilAsserted(() -> assertThat(mavenLog.getWarnings()).hasSize(2));
      LogEntry lastEntry = mavenLog.getWarnings().get(mavenLog.getWarnings().size() - 1);
      assertThat(lastEntry.toString()).isEqualTo(" ENGINE: finished");

      log.write("INFO: hi");
      await().untilAsserted(() -> assertThat(mavenLog.getDebug()).hasSize(1));
      LogEntry debugEntry = mavenLog.getDebug().get(mavenLog.getDebug().size() - 1);
      assertThat(debugEntry.toString()).isEqualTo(" ENGINE: hi");

    } finally {
      logForwarder.deactivate();
    }

    log.write("WARNING: illegal");
    await().untilAsserted(() -> assertThat(mavenLog.getWarnings()).hasSize(2));
  }

  private static final class FakeLogger {

    private final Path file;

    private FakeLogger(Path file) {
      this.file = file;
    }

    private void write(String log) {
      try {
        Files.writeString(file, log, StandardOpenOption.APPEND);
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
    }
  }
}
