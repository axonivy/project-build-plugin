/*
 * Copyright (C) 2021 Axon Ivy AG
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package ch.ivyteam.ivy.maven.engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ch.ivyteam.ivy.maven.util.ClasspathJar;

class TestClasspathJar {

  @TempDir
  Path tempDir;

  @Test
  void readWriteClasspath() throws IOException {
    var jarFile = tempDir.resolve("my.jar");
    Files.createFile(jarFile);
    var jar = new ClasspathJar(jarFile.toFile());
    var content = tempDir.resolve("content.jar");
    Files.createFile(content);
    jar.createFileEntries(List.of(content.toFile()));

    assertThat(jar.getClasspathFiles()).contains(content.getFileName().toString());

    try (var in = new ZipInputStream(Files.newInputStream(jarFile))) {
      ZipEntry first = in.getNextEntry();
      assertThat(first.getName()).isEqualTo("META-INF/MANIFEST.MF");

      String manifest = new String(in.readAllBytes(), StandardCharsets.UTF_8);
      assertThat(manifest)
              .as("Manifest should not start with a whitespace or it will not be interpreted by the JVM")
              .startsWith("Manifest-Version:");
    }
  }
}
