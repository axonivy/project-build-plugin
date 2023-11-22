package ch.ivyteam.ivy.maven.engine.download;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.versioning.VersionRange;

public class LatestMinorVersionRange {

  private final String version;

  public LatestMinorVersionRange(String version) {
    this.version = version;
  }

  public VersionRange get() {
    try {
      var majorVersionNr = StringUtils.substringBefore(version, ".");
      var minorVersionNr = StringUtils.substringBetween(version, ".");
      var bugfixVersionNr = StringUtils.substringAfterLast(version, ".");
      var nextMinorVersionNr = Integer.parseInt(minorVersionNr) + 1;
      var minVersion = majorVersionNr + "." + minorVersionNr + "." + bugfixVersionNr;
      var maxVersion = majorVersionNr + "." + nextMinorVersionNr + ".0";
      return VersionRange.createFromVersionSpec("[" + minVersion + "," + maxVersion + ")");
    } catch (Exception ex) {
      throw new RuntimeException("Could not calculate version spec from " + version, ex);
    }
  }
}
