package ch.ivyteam.ivy.maven.engine;

import java.util.List;

import org.apache.commons.exec.CommandLine;

/** hints adopted from engine <code>bin/launcher.sh</code> */
public class EngineModuleHints {

  private interface Add {
    String OPENS = "--add-opens";
    String EXPORTS = "--add-exports";
  }

  private static final List<String> ADD_OPENS_HINTS = List.of(
    // for javassist / ivy script and others
    "java.base/java.lang=ALL-UNNAMED",
    "java.base/java.lang.reflect=ALL-UNNAMED",
    // for com.thoughtworks.xstream.XStream (used by drools) / swagger
    "java.base/java.text=ALL-UNNAMED",
    "java.base/java.util=ALL-UNNAMED",
    "java.desktop/java.awt.font=ALL-UNNAMED",
    // on engine stop tomcat clears caches / ivy script serialization
    "java.base/java.io=ALL-UNNAMED",
    "java.rmi/sun.rmi.transport=ALL-UNNAMED",
    // allow ZipFileSystem readonly feature on engine with Java 11
    "jdk.zipfs/jdk.nio.zipfs=ALL-UNNAMED"
  );

  private static final List<String> ADD_EXPORTS_HINTS = List.of(
    // for XML scripting object / XPath
    "java.xml/com.sun.org.apache.xpath.internal=ALL-UNNAMED",
    "java.xml/com.sun.org.apache.xpath.internal.objects=ALL-UNNAMED",
    "java.xml/com.sun.org.apache.xml.internal.utils=ALL-UNNAMED"
  );

  public static String getCmdArgLine() {
    StringBuilder hints = new StringBuilder();
    addHints(hints, Add.OPENS, ADD_OPENS_HINTS);
    addHints(hints, Add.EXPORTS, ADD_EXPORTS_HINTS);
    return hints.toString();
  }

  private static void addHints(StringBuilder argLine, String hint, List<String> values) {
    for (String open : values) {
      argLine.append(" ").append(hint).append(" ").append(open);
    }
  }

  public static void addToCmdLine(CommandLine cli) {
    for (String hint : EngineModuleHints.ADD_OPENS_HINTS) {
      cli.addArgument(EngineModuleHints.Add.OPENS).addArgument(hint);
    }
    for (String hint : EngineModuleHints.ADD_EXPORTS_HINTS) {
      cli.addArgument(EngineModuleHints.Add.EXPORTS).addArgument(hint);
    }
  }
}