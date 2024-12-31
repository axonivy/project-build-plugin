package ch.ivyteam.ivy.maven.engine.download;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class TestLatestMinorVersionRange {

  @Test
  void get() {
    assertThat(new LatestMinorVersionRange("8.0.0").get().toString()).isEqualTo("[8.0.0,8.1.0)");
    assertThat(new LatestMinorVersionRange("8.0.1").get().toString()).isEqualTo("[8.0.1,8.1.0)");
    assertThat(new LatestMinorVersionRange("8.1.0").get().toString()).isEqualTo("[8.1.0,8.2.0)");

    assertThatThrownBy(() -> new LatestMinorVersionRange("8").get())
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Could not calculate version spec from 8");
  }
}
