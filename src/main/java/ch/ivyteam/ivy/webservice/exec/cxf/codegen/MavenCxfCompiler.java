package ch.ivyteam.ivy.webservice.exec.cxf.codegen;

import java.io.File;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.compiler.Compiler;
import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.compiler.CompilerException;
import org.codehaus.plexus.compiler.CompilerMessage;
import org.codehaus.plexus.compiler.CompilerResult;
import org.codehaus.plexus.compiler.manager.CompilerManager;
import org.codehaus.plexus.compiler.manager.NoSuchCompilerException;


public class MavenCxfCompiler extends org.apache.cxf.common.util.Compiler {

  private static final String JAVA_VERSION = "11";

  private final CompilerManager compilerManager;
  private final Log log;
  private final CompilerConfiguration config;

  public MavenCxfCompiler(CompilerManager compilerManager, Log log) {
    this.compilerManager = compilerManager;
    this.log = log;

    this.config = defaultConfig();
    if (log.isDebugEnabled()) {
      config.setVerbose(true);
    }
  }

  private static CompilerConfiguration defaultConfig() {
    var config = new CompilerConfiguration();
    config.setReleaseVersion(JAVA_VERSION);
    config.setTargetVersion(JAVA_VERSION);
    config.setCompilerVersion(JAVA_VERSION);
    return config;
  }

  @Override
  public void setOutputDir(File out) {
    this.setOutputDir(out.getAbsolutePath());
  }

  @Override
  public void setOutputDir(String out) {
    config.setOutputLocation(out);
    config.addClasspathEntry(out);
  }

  @Override
  public void setEncoding(String encoding) {
    config.setSourceEncoding(encoding);
  }

  @Override
  public void setVerbose(boolean verbose) {
    config.setVerbose(verbose);
  }

  public void addClasspathEntry(String entry) {
    log.debug("Adding CXF client codegen classpath entry: "+entry);
    config.addClasspathEntry(entry);
  }

  @Override
  public boolean compileFiles(String[] files) {
    Set<File> sources = Arrays.stream(files).map(File::new).collect(Collectors.toSet());
    config.setSourceFiles(sources);
    try {
      Compiler compiler = compilerManager.getCompiler("javac");
      CompilerResult result = compiler.performCompile(config);
      result.getCompilerMessages().stream().forEach(this::logMessage);
      return result.isSuccess();
    } catch (NoSuchCompilerException | CompilerException ex) {
      log.error("Failed to compile CXF client", ex);
      return false;
    }
  }

  private void logMessage(CompilerMessage msg) {
    switch (msg.getKind()) {
      case ERROR:
        log.error(msg.toString());
        break;
      case WARNING:
      case MANDATORY_WARNING:
        log.warn(msg.toString());
        break;
      default:
        log.info(msg.toString());
        break;
    }
  }
}