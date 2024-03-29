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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import ch.ivyteam.ivy.maven.util.ClasspathJar;

public class TestClasspathJar {

  @Test
  public void readWriteClasspath() throws IOException {
    File jarFile = Files.createTempFile("my", ".jar").toFile();
    ClasspathJar jar = new ClasspathJar(jarFile);
    File content = Files.createTempFile("content", ".jar").toFile();
    jar.createFileEntries(Arrays.asList(content));

    assertThat(jar.getClasspathFiles()).contains(content.getName());

    ZipInputStream jarStream = new ZipInputStream(new FileInputStream(jarFile));
    ZipEntry first = jarStream.getNextEntry();
    assertThat(first.getName()).isEqualTo("META-INF/MANIFEST.MF");
    String manifest = IOUtils.toString(jarStream, StandardCharsets.UTF_8);
    assertThat(manifest)
            .as("Manifest should not start with a whitespace or it will not be interpreted by the JVM")
            .startsWith("Manifest-Version:");
  }

}
