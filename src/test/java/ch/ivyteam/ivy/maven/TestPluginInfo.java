package ch.ivyteam.ivy.maven;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.junit.jupiter.api.Test;

class TestPluginInfo {

  @Test
  void snapshotQualifier() {
    var descriptor = new PluginDescriptor();
    descriptor.setVersion("14.0.2-SNAPSHOT");

    assertThat(PluginInfo.versionOf(descriptor))
        .contains(new PluginInfo.Version(14, 0, 2, "-SNAPSHOT"));
  }

  @Test
  void milestoneSnapshotQualifier() {
    var descriptor = new PluginDescriptor();
    descriptor.setVersion("14.0.2.m31-SNAPSHOT");

    assertThat(PluginInfo.versionOf(descriptor))
        .contains(new PluginInfo.Version(14, 0, 2, ".m31-SNAPSHOT"));
  }

  @Test
  void rcQualifier() {
    var descriptor = new PluginDescriptor();
    descriptor.setVersion("14.0.2.RC1");

    assertThat(PluginInfo.versionOf(descriptor))
        .contains(new PluginInfo.Version(14, 0, 2, ".RC1"));
  }

  @Test
  void defaultsMissingQualifierToEmpty() {
    var descriptor = new PluginDescriptor();
    descriptor.setVersion("14.0.2");

    assertThat(PluginInfo.versionOf(descriptor))
        .contains(new PluginInfo.Version(14, 0, 2, ""));
  }
}
