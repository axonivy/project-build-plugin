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

import static org.fest.assertions.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import ch.ivyteam.ivy.maven.log.LogCollector;
import ch.ivyteam.ivy.maven.log.LogCollector.LogEntry;

public class TestFileLogForwarder
{

  @Test
  public void fileToMavenLog() throws Exception
  {
    File fakeEngineLog = Files.createTempFile("myProject.iar", ".deploymentLog").toFile();
    LogCollector mavenLog = new LogCollector();
    FileLogForwarder logForwarder = new FileLogForwarder(fakeEngineLog, mavenLog);
    
    try
    {
      logForwarder.activate();
      
      logAndWait(fakeEngineLog, "[INFO] starting");
      assertThat(mavenLog.getInfos()).hasSize(1);
      LogEntry firstEntry = mavenLog.getInfos().get(mavenLog.getInfos().size()-1);
      assertThat(firstEntry.toString()).isEqualTo("[INFO] starting");
      
      logAndWait(fakeEngineLog, "[INFO] finished");
      assertThat(mavenLog.getInfos()).hasSize(2);
      LogEntry lastEntry = mavenLog.getInfos().get(mavenLog.getInfos().size()-1);
      assertThat(lastEntry.toString()).isEqualTo("[INFO] finished");
    }
    finally
    {
      logForwarder.deactivate();
    }
    
    logAndWait(fakeEngineLog, "[INFO] illegal");
    assertThat(mavenLog.getInfos()).hasSize(2);
  }

  private static void logAndWait(File fakeEngineLog, String log) throws IOException, InterruptedException
  {
    boolean append = true;
    FileUtils.write(fakeEngineLog, log, append);
    Thread.sleep(1000);
  }
  
}
