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
          "ch.ivyteam.ivy.java.internal.JavaBuilder"
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