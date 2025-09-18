/*
 * Copyright (C) 2024 Axon Ivy AG
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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.slf4j.impl.SimpleLogger;

/**
 * Sets the logging properties for the ivy engine.
 *
 * @author Reguel Wermelinger
 * @since 6.0.0
 */
public class Slf4jSimpleEngineProperties {
  private static final String DEFAULT_LOG_LEVEL = SimpleLogger.DEFAULT_LOG_LEVEL_KEY;
  private static final List<String> INTERESTING_LOGGERS = Arrays.asList(
      "ch.ivyteam.ivy.server.build.InMemoryEngineController",
      "ch.ivyteam.ivy.project.build.MavenProjectBuilder",
      "ch.ivyteam.ivy.java.JavaCompiler",
      "ch.ivyteam.ivy.webservice.process.restricted.WebServiceProcessClassBuilder",
      "ch.ivyteam.ivy.dialog.form.build.JsonFormResourceBuilder"
  );
  private static final String IVY_PREFIX = "ch.ivyteam.ivy";

  public static void install() {
    setDefaultProperty(SimpleLogger.SHOW_THREAD_NAME_KEY, Boolean.FALSE.toString());
    setDefaultProperty(SimpleLogger.LEVEL_IN_BRACKETS_KEY, Boolean.TRUE.toString());
    setDefaultProperty(SimpleLogger.SHOW_LOG_NAME_KEY, Boolean.FALSE.toString());
    setDefaultProperty(SimpleLogger.WARN_LEVEL_STRING_KEY, "WARNING");

    // apply Maven log level to well known white-listed ivy loggers
    String mavenClientLogLevel = getDefaultLogLevel();
    for (String loggerName : INTERESTING_LOGGERS) {
      System.setProperty(SimpleLogger.LOG_KEY_PREFIX + loggerName, mavenClientLogLevel);
    }

    // only log errors from unspecific engine loggers!
    System.setProperty(SimpleLogger.LOG_KEY_PREFIX + IVY_PREFIX, Level.ERROR);

    // Disable CXF warning at startup (missing META-INF/cxf/cxf.xml)
    System.setProperty(SimpleLogger.LOG_KEY_PREFIX + "org.apache.cxf.bus.spring", Level.ERROR);

    // only warnings from any logger used by ivy third parties (e.g.
    // org.apache.myfaces.xxx, org.apache.cxf, ...)
    System.setProperty(DEFAULT_LOG_LEVEL, Level.WARNING);

    // remain in same stream as the Maven CLI; don't use the default 'System.err'
    System.setProperty(SimpleLogger.LOG_FILE_KEY, "System.out");
  }

  public static void enforceSimpleConfigReload() {
    try {
      Method initMethod = SimpleLogger.class.getDeclaredMethod("init");
      initMethod.setAccessible(true);
      initMethod.invoke(null);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public static void reset() {
    for (String loggerName : INTERESTING_LOGGERS) {
      System.clearProperty(SimpleLogger.LOG_KEY_PREFIX + loggerName);
    }
    System.clearProperty(SimpleLogger.LOG_KEY_PREFIX + IVY_PREFIX);
  }

  private static String getDefaultLogLevel() {
    String mavenClientLogLevel = System.getProperty(DEFAULT_LOG_LEVEL);
    boolean isPreMaven31Client = mavenClientLogLevel == null;
    if (isPreMaven31Client) { // pre 31 clients we're not using sfl4j-simple for
                              // their own logs
      return Level.INFO;
    }
    return mavenClientLogLevel;
  }

  private static void setDefaultProperty(String property, String value) {
    if (System.getProperty(property) == null) {
      System.setProperty(property, value);
    }
  }

  /**
   * Valid levels as documented in {@link SimpleLogger}
   */
  interface Level {
    String TRACE = "trace";
    String DEBUG = "debug";
    String INFO = "info";
    String WARNING = "warn";
    String ERROR = "error";
  }
}
