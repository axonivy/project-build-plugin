package com.axonivy.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.axonivy.resource.ResourceAccess;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
class TestProcessResources {

  @Test
  void resources() throws IOException {
    assertThat(ResourceAccess.resourceContent()).isEqualTo("hello from src resource");
  }

  @Test
  void testResources() throws IOException {
    try (var in = getClass().getResourceAsStream("resource_in_src_test.txt")) {
      var content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
      assertThat(content).isEqualTo("hello from src_test resource");
    }
  }
}
