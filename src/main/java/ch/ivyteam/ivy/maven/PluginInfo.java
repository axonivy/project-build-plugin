package ch.ivyteam.ivy.maven;

import java.util.Optional;
import java.util.regex.Pattern;

import org.apache.maven.plugin.descriptor.PluginDescriptor;

public class PluginInfo {

  private static final Pattern PLUGIN_VERSION = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)(.*)?$");

  public static Optional<Version> versionOf(PluginDescriptor descriptor) {
    if (descriptor == null || descriptor.getVersion() == null) {
      return Optional.empty();
    }
    var matcher = PLUGIN_VERSION.matcher(descriptor.getVersion());
    if (!matcher.matches()) {
      return Optional.empty();
    }
    try {
      int major = Integer.parseInt(matcher.group(1));
      int minor = Integer.parseInt(matcher.group(2));
      int patch = Integer.parseInt(matcher.group(3));
      String qualifier = matcher.group(4) == null ? "" : matcher.group(4);
      return Optional.of(new Version(major, minor, patch, qualifier));
    } catch (NumberFormatException ex) {
      return Optional.empty();
    }
  }

  public record Version(int major, int minor, int patch, String qualifier) {
  }
}
