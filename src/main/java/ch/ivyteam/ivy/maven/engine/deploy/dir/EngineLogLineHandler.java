/*
 * Copyright (C) 2021 Axon Ivy AG
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

package ch.ivyteam.ivy.maven.engine.deploy.dir;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.plugin.logging.Log;

/**
 * Logs only log lines with a well known severity into maven log.
 * 
 * <p>{@link Level#INFO} is logged as debug, as it contains information to trace the detailed 
 * deployment steps which are too detailed for a normal deployment run.</p>
 */
class EngineLogLineHandler implements FileLogForwarder.LogLineHandler
{
  private static final Pattern LOG_PATTERN = Pattern.compile("([A-Za-z]+):(.*)");
  
  private final Log log;

  EngineLogLineHandler(Log log)
  {
    this.log = log;
  }
  
  @Override
  public void handleLine(String logLine)
  {
    Matcher matcher = LOG_PATTERN.matcher(logLine);
    if (matcher.matches())
    {
      String level = matcher.group(1);
      String message = matcher.group(2);
      message = " ENGINE:"+message;
      
      if (level.equalsIgnoreCase(Level.INFO))
      { // infos are too detailed, but users can trace in debug -X mode.
        log.debug(message);
      }
      if (level.equalsIgnoreCase(Level.WARNING))
      {
        log.warn(message);
      }
      if (level.equalsIgnoreCase(Level.ERROR))
      {
        log.error(message);
      }
    }
  }

  static interface Level
  {
    String INFO = "info";
    String WARNING = "warning";
    String ERROR = "error";
  }
}