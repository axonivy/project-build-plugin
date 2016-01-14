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

package ch.ivyteam.ivy.maven.engine;

import java.util.Arrays;
import java.util.List;

/**
 * Sets the logging properties for the ivy engine.
 * 
 * @author Reguel Wermelinger
 * @since 11.11.2014
 */
class Slf4jSimpleEngineProperties
{
  private static final String PROPERTY_PREFIX = "org.slf4j.simpleLogger.";
  private static final List<String> INTERESTING_LOGGERS = Arrays.asList(
          "ch.ivyteam.ivy.scripting.dataclass.internal.InMemoryEngineController",
          "ch.ivyteam.ivy.java.internal.JavaBuilder",
          "ch.ivyteam.ivy.maven" // my mojo logs
  );
  
  static void install()
  {
    setDefaultProperty("showThreadName", Boolean.FALSE.toString());
    setDefaultProperty("levelInBrackets", Boolean.TRUE.toString());
    
    String mavenClientLogLevel = getProperty("defaultLogLevel");
    boolean isPreMaven31Client = mavenClientLogLevel == null;
    if (isPreMaven31Client)
    { // pre 31 clients we're not using sfl4j-simple for their own logs
      mavenClientLogLevel = "info";
    }
    setProperty("log.ch.ivyteam.ivy", "error"); // only log errors from unspecific engine loggers! 
    for(String loggerName : INTERESTING_LOGGERS)
    {
      setProperty("log."+loggerName, mavenClientLogLevel);
    }
    setProperty("defaultLogLevel", "warn"); 
  }
  
  private static void setDefaultProperty(String property, String value)
  {
    if (getProperty(property) == null)
    {
      setProperty(property, value);
    }
  }

  private static String getProperty(String property)
  {
    return System.getProperty(PROPERTY_PREFIX+property);
  }

  private static void setProperty(String property, String value)
  {
    System.setProperty(PROPERTY_PREFIX+property, value);
  }
}