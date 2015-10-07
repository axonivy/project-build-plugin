/*
 * Copyright (C) 2015 AXON IVY AG
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

package ch.ivyteam.ivy.maven.log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.plugin.logging.Log;

/**
 * Simple Test logger built to make simple assertions on occurred logs.
 * 
 * @author Reguel Wermelinger
 * @since 05.11.2014
 */
public class LogCollector implements Log
{
  private List<LogEntry> debuggings = new ArrayList<>();
  private List<LogEntry> infos = new ArrayList<>();
  private List<LogEntry> warnings = new ArrayList<>();
  private List<LogEntry> errors = new ArrayList<>();

  public static class LogEntry
  {
    private String message;
    private Throwable error;

    private LogEntry(String message, Throwable error)
    {
      if (message == null)
      {
        message = error.getMessage();
      }
      this.message = message;
      this.error = error;
    }
    
    @Override
    public String toString()
    {
      StringBuilder builder = new StringBuilder(message);
      if (error != null)
      {
        builder.append(" /error:").append(error.toString());
      }
      return builder.toString();
    }
    
  }
  
  public List<LogEntry> getDebug()
  {
    return Collections.unmodifiableList(debuggings);
  }


  public List<LogEntry> getInfos()
  {
    return Collections.unmodifiableList(infos);
  }


  public List<LogEntry> getWarnings()
  {
    return Collections.unmodifiableList(warnings);
  }


  public List<LogEntry> getErrors()
  {
    return Collections.unmodifiableList(errors);
  }
  
  public List<LogEntry> getLogs()
  {
    List<LogEntry> logs = new ArrayList<>();
    logs.addAll(debuggings);
    logs.addAll(infos);
    logs.addAll(warnings);
    logs.addAll(errors);
    return Collections.unmodifiableList(logs);
  }

  private void log(List<LogEntry> appender, CharSequence message, Throwable error)
  {
    LogEntry entry = new LogEntry(message.toString(), error);
    appender.add(entry);
  }
  

  @Override
  public boolean isDebugEnabled()
  {
    return true;
  }

  @Override
  public void debug(CharSequence content)
  {
    log(debuggings, content, null);
  }

  @Override
  public void debug(CharSequence content, Throwable error)
  {
    log(debuggings, content, error);
  }

  @Override
  public void debug(Throwable error)
  {
    log(debuggings, null, error);    
  }

  @Override
  public boolean isInfoEnabled()
  {
    return true;
  }

  @Override
  public void info(CharSequence content)
  {
    log(infos, content, null); 
  }

  @Override
  public void info(CharSequence content, Throwable error)
  {
    log(infos, content, error);
  }

  @Override
  public void info(Throwable error)
  {
    log(infos, null, error);
  }

  @Override
  public boolean isWarnEnabled()
  {
    return true;
  }

  @Override
  public void warn(CharSequence content)
  {
    log(warnings, content, null);
  }

  @Override
  public void warn(CharSequence content, Throwable error)
  {
    log(warnings, content, error);
  }

  @Override
  public void warn(Throwable error)
  {
    log(warnings, null, error);
  }

  @Override
  public boolean isErrorEnabled()
  {
    return true;
  }

  @Override
  public void error(CharSequence content)
  {
    log(errors, content, null);
  }

  @Override
  public void error(CharSequence content, Throwable error)
  {
    log(errors, content, error);
  }

  @Override
  public void error(Throwable error)
  {
    log(errors, null, error);
  }

}
