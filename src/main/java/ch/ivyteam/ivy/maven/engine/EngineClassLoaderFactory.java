package ch.ivyteam.ivy.maven.engine;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.repository.RepositorySystem;

/**
 * Factory that provides an {@link URLClassLoader} for ivy Engine class access.
 * This makes invocation of engine parts possible without starting a new java process.
 * 
 * @author Reguel Wermelinger
 * @since 25.09.2014
 */
public class EngineClassLoaderFactory
{
  /** must match version in pom.xml */
  private static final String SLF4J_VERSION = "1.7.7";

  private static List<String> ENGINE_LIB_DIRECTORIES = Arrays.asList(
          "lib"+File.separator+"ivy"+File.separator,
          "lib"+File.separator+"patch"+File.separator,
          "lib"+File.separator+"shared"+File.separator,
          "webapps"+File.separator+"ivy"+File.separator+"WEB-INF"+File.separator+"lib"+File.separator
        );
  
  
  private MavenContext maven;
  
  public EngineClassLoaderFactory(MavenContext mavenContext)
  {
    this.maven = mavenContext;
  }
  
  public URLClassLoader createEngineClassLoader(File engineDirectory) throws MalformedURLException
  {
    return new URLClassLoader(getIvyEngineClassPathUrls(engineDirectory));
  }
  
  private URL[] getIvyEngineClassPathUrls(File engineDirectory) throws MalformedURLException
  {
    List<URL> classPathUrls = new ArrayList<>();
    List<File> ivyEngineClassPathFiles = getIvyEngineClassPathFiles(engineDirectory);
    for(File file : customize(ivyEngineClassPathFiles))
    {
      classPathUrls.add(new URL(file.toURI().toASCIIString()));
    }
    return classPathUrls.toArray(new URL[classPathUrls.size()]);
  }

  private static List<File> getIvyEngineClassPathFiles(File engineDirectory)
  {
    List<File> classPathFiles = new ArrayList<>();
    for(String engineLibDirectory : ENGINE_LIB_DIRECTORIES)
    {
      
      File jarDir = new File(engineDirectory, engineLibDirectory);
      for(File jar : FileUtils.listFiles(jarDir, new String[]{"jar"}, false))
      {
        classPathFiles.add(jar);
      }
    }
    return classPathFiles;
  }
  
  private List<File> customize(List<File> engineClassPath)
  {
    EngineClassPathCustomizer classPathCustomizer = new EngineClassPathCustomizer();
    
    // bridge log4j logs to SFL4J
    classPathCustomizer.registerReplacement("log4j-", 
            maven.getJar("org.slf4j", "log4j-over-slf4j", SLF4J_VERSION)); 
    
    // do not bind log4j as implementation of SLF4J but use slf4j-simple
    classPathCustomizer.registerReplacement("slf4j-log4j12", 
            maven.getJar("org.slf4j", "slf4j-simple", SLF4J_VERSION));
    Slf4jSimpleEngineProperties.install();
    
    return classPathCustomizer.customizeClassPath(engineClassPath);
  }

  public static class MavenContext
  {
    private RepositorySystem repoSystem;
    private ArtifactRepository localRepository;
    private Log log;
    
    public MavenContext(RepositorySystem repoSystem, ArtifactRepository localRepository, Log log)
    {
      this.repoSystem = repoSystem;
      this.localRepository = localRepository;
      this.log = log;
    }
    
    public File getJar(String groupId, String artifactId, String version)
    {
      Artifact artifact = repoSystem.createArtifact(groupId, artifactId, version, "jar");
      File jar = new File(localRepository.getBasedir(), localRepository.pathOf(artifact));
      if (!jar.exists())
      {
        log.warn("Failed to resolve '" + artifactId + "' from local repository in '"+jar+"'.");
      }
      return jar;
    }
  }
  
}