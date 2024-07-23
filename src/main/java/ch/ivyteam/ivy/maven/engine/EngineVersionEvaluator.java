package ch.ivyteam.ivy.maven.engine;

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.plugin.logging.Log;

import ch.ivyteam.ivy.maven.engine.EngineClassLoaderFactory.OsgiDir;

public class EngineVersionEvaluator {

  public static final String LIBRARY_ID = "ch.ivyteam.util";

  private Log log;
  private Path engineDir;

  public EngineVersionEvaluator(Log log, Path engineDir) {
    this.log = log;
    this.engineDir = engineDir;
  }

  public ArtifactVersion evaluateVersion() {
    if (!isOSGiEngine(engineDir)) {
      String absolutePath = engineDir == null ? "" : engineDir.toAbsolutePath().toString();
      log.info("Can not evaluate version of a non-OSGi engine in directory '" + absolutePath + "'");
      return null;
    }

    String libraryFileName = getLibraryFileName(LIBRARY_ID);
    if (libraryFileName == null) {
      return null;
    }

    String version = StringUtils.substringAfter(libraryFileName, "_");
    return new DefaultArtifactVersion(toReleaseVersion(version));
  }

  public static boolean isOSGiEngine(Path engineDir) {
    var folder = engineDir.resolve(OsgiDir.INSTALL_AREA);
    return Files.exists(folder);
  }

  public static String toReleaseVersion(String version) { // 6.1.0.51869 ->
                                                          // 6.1.0
    String[] versionParts = StringUtils.split(version, ".");
    if (ArrayUtils.isEmpty(versionParts)) {
      return null;
    }
    return StringUtils.join(versionParts, ".", 0, Math.min(versionParts.length, 3));
  }

  private String getLibraryFileName(String libraryId) {
    var ivyLibs = engineDir.resolve(OsgiDir.PLUGINS);
    if (!Files.exists(ivyLibs)) {
      return null;
    }

    String[] libraryNames = ivyLibs.toFile().list();
    if (libraryNames == null) {
      return null;
    }

    for (String libraryName : libraryNames) {
      if (libraryName.toLowerCase().startsWith(libraryId)) {
        return libraryName;
      }
    }
    return null;
  }
}
