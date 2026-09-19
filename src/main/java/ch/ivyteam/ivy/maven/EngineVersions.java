package ch.ivyteam.ivy.maven;

import java.io.IOException;
import java.util.Properties;

class EngineVersions {
  private static Properties PROPERTIES;
  
  static String readVersion(String key) {
    if (PROPERTIES == null) {
      PROPERTIES = readVersions();;
    }
    String version = EngineVersions.PROPERTIES.getProperty(key);
    if (version == null) {
      throw new IllegalStateException("Missing property '" + key + "' in resource: versions.properties");
    }
    return version;
  }

  private static Properties readVersions() {
    String versions = "versions.properties";
    try (var in = EngineVersions.class.getResourceAsStream(
        "/ch/ivyteam/ivy/maven/engine/" + versions)) {
      if (in == null) {
        throw new IllegalStateException("Missing resource: " + versions);
      }
      Properties props = new Properties();
      props.load(in);
      return props;
    } catch (IOException ex) {
      throw new RuntimeException("Failed to read versions from file: " + versions, ex);
    }
  }

}
