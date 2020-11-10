package ch.ivyteam.ivy.maven.engine;

import java.util.List;

import org.apache.commons.exec.CommandLine;

public class EngineModuleHints
{
  private static final String ADD_OPENS = "--add-opens";
  
  /** hints adopted from engine <code>bin/launcher.sh</code> */
  private static final List<String> ADD_OPENS_HINTS=List.of(
     // ignore illegal reflective access warning at startup because guice
    "java.base/java.lang=ALL-UNNAMED", 
     // ignore illegal reflective access warning at rest deployment because ProjectJaxRsClassesScanner#hackReflectionHelperToNonOsgiMode()
    "java.base/java.lang.reflect=ALL-UNNAMED", 
     // on engine stop tomcat clears caches
    "java.base/java.io=ALL-UNNAMED",
    "java.rmi/sun.rmi.transport=ALL-UNNAMED",
     // allow ZipFileSystem readonly feature on engine with Java 11
    "jdk.zipfs/jdk.nio.zipfs=ALL-UNNAMED"
  );
  
  public static void addToCmdLine(CommandLine cli)
  {
    for(String hint : EngineModuleHints.ADD_OPENS_HINTS)
    {
      cli.addArgument(EngineModuleHints.ADD_OPENS).addArgument(hint);
    }
  }
}