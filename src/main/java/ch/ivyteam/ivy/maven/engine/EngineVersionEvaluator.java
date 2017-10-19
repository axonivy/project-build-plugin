package ch.ivyteam.ivy.maven.engine;

import java.io.File;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.plugin.logging.Log;

import ch.ivyteam.ivy.maven.engine.EngineClassLoaderFactory.OsgiDir;

public class EngineVersionEvaluator
{
  public static final String LIBRARY_ID = "ch.ivyteam.util";
 
  private Log log;
  private File engineDir;

  public EngineVersionEvaluator(Log log, File engineDir)
  {
    this.log = log;
    this.engineDir = engineDir;
  }

  public ArtifactVersion evaluateVersion()
  {
    if (!isOSGiEngine(engineDir))
    {
      String absolutePath = engineDir == null ? "" : engineDir.getAbsolutePath();
      log.info("Can not evaluate version of a non-OSGi engine in directory '" + absolutePath + "'");
      return null;
    }

    String libraryFileName = getLibraryFileName(LIBRARY_ID);
    if (libraryFileName == null)
    {
      return null;
    }
    
    String version = StringUtils.substringAfter(libraryFileName, "_");
    return new DefaultArtifactVersion(toReleaseVersion(version));
  }

  public static boolean isOSGiEngine(File engineDir)
  {
    File folder = new File(engineDir, OsgiDir.INSTALL_AREA);
    return folder.exists();
  }

  public static String toReleaseVersion(String version)
  { // 6.1.0.51869 -> 6.1.0
    String[] versionParts = StringUtils.split(version, ".");
    if (ArrayUtils.isEmpty(versionParts))
    {
      return null;
    }
    return StringUtils.join(versionParts, ".", 0, Math.min(versionParts.length, 3));
  }
  
  private String getLibraryFileName(String libraryId)
  {
    File ivyLibs = new File(engineDir, OsgiDir.PLUGINS);
    if (!ivyLibs.exists())
    {
      return null;
    }

    String[] libraryNames = ivyLibs.list();
    if (libraryNames == null)
    {
      return null;
    }
    
    for (String libraryName : libraryNames)
    {
      if (libraryName.toLowerCase().startsWith(libraryId))
      {
        return libraryName;
      }
    }
    return null;
  }

}
