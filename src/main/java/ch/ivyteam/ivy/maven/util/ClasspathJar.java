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

package ch.ivyteam.ivy.maven.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang3.StringUtils;

/**
 * Jar with only a Manifest.MF that defines the classpath.
 *
 * @author Reguel Wermelinger
 * @since 6.0.2
 */
public class ClasspathJar {

  private static final String MANIFEST_MF = "META-INF/MANIFEST.MF";
  private final Path jar;

  public ClasspathJar(Path jar) {
    this.jar = jar;
  }

  private void create(List<String> classpathEntries) throws IOException {
    Files.createDirectories(jar.getParent());
    try (var out = new ZipOutputStream(Files.newOutputStream(jar))) {
      String name = StringUtils.substringBeforeLast(jar.getFileName().toString(), ".");
      writeManifest(name, out, classpathEntries);
    }
  }

  public void createFileEntries(Collection<Path> classpathEntries) throws IOException {
    create(getClassPathUris(classpathEntries));
  }

  private void writeManifest(String name, ZipOutputStream jarStream, List<String> classpathEntries)
      throws IOException {
    Manifest manifest = new Manifest();
    manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
    manifest.getMainAttributes().putValue("Name", name);
    if (!classpathEntries.isEmpty()) {
      manifest.getMainAttributes().put(Attributes.Name.CLASS_PATH, StringUtils.join(classpathEntries, " "));
    }
    jarStream.putNextEntry(new ZipEntry(MANIFEST_MF));
    manifest.write(jarStream);
  }

  private static List<String> getClassPathUris(Collection<Path> classpathEntries) {
    return classpathEntries.stream()
        .map(p -> p.toUri().toASCIIString())
        .collect(Collectors.toList());
  }
}
