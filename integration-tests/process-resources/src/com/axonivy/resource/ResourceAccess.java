package com.axonivy.resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ResourceAccess {

  public static String resourceContent() throws IOException {
    try (var in = ResourceAccess.class.getResourceAsStream("resource_in_src.txt")) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
