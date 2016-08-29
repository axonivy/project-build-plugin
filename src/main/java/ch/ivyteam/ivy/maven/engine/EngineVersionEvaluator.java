package ch.ivyteam.ivy.maven.engine;

import java.io.File;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

public class EngineVersionEvaluator
{
  public static final String LIBRARY_ID = "ch.ivyteam.util";
  private File engineDir;

  public EngineVersionEvaluator(File engineDir)
  {
    this.engineDir = engineDir;
  }

  public ArtifactVersion evaluateVersion()
  {
    String libraryFileName = getLibraryFileName(LIBRARY_ID);
    if (libraryFileName == null)
    {
      return null;
    }
    
    String version = StringUtils.substringAfter(libraryFileName, "_");
    return new DefaultArtifactVersion(toReleaseVersion(version));
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
    File ivyLibs = new File(engineDir, "plugins");
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
