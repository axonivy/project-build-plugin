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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;

import ch.ivyteam.ivy.maven.util.ClasspathJar;
import ch.ivyteam.ivy.maven.util.SharedFile;

/**
 * Factory that provides an {@link URLClassLoader} for ivy Engine class access.
 * This makes invocation of engine parts possible without starting a new java
 * process.
 *
 * @author Reguel Wermelinger
 * @since 25.09.2014
 */
@SuppressWarnings("deprecation")
public class EngineClassLoaderFactory {

  public interface OsgiDir {
    Path INSTALL_AREA = Path.of("system");
    Path PLUGINS = INSTALL_AREA.resolve("plugins");
    Path LIB_BOOT = Path.of("lib/boot");
  }

  /** must match version in pom.xml */
  private static final String SLF4J_VERSION = "2.0.13";

  private static final List<Path> ENGINE_LIB_DIRECTORIES = List.of(
          OsgiDir.INSTALL_AREA.resolve(OsgiDir.LIB_BOOT),
          OsgiDir.PLUGINS,
          // unpacked jars from OSGI bundles
          OsgiDir.INSTALL_AREA.resolve("configuration").resolve("org.eclipse.osgi"),
          Path.of("webapps").resolve("ivy").resolve("WEB-INF").resolve("lib"));

  private MavenContext maven;

  public EngineClassLoaderFactory(MavenContext mavenContext) {
    this.maven = mavenContext;
  }

  public URLClassLoader createEngineClassLoader(Path engineDirectory) throws IOException {
    var osgiClasspath = getOsgiBootstrapClasspath(engineDirectory);
    var filter = WildcardFileFilter.builder()
            .setWildcards("org.eclipse.osgi_*.jar")
            .get();
    var pluginsDir = engineDirectory.resolve(OsgiDir.PLUGINS);
    addToClassPath(osgiClasspath, pluginsDir, filter);
    osgiClasspath.addAll(0, getSlf4jJars());
    if (maven.log.isDebugEnabled()) {
      maven.log.debug("Configuring OSGi engine classpath:");
      osgiClasspath.stream().forEach(file -> maven.log.debug(" + " + file.toAbsolutePath()));
    }
    return new URLClassLoader(toUrls(osgiClasspath));
  }

  public List<Path> getSlf4jJars() {
    return List.of(
            maven.getJar("org.slf4j", "slf4j-api", SLF4J_VERSION),
            maven.getJar("org.slf4j", "slf4j-simple", SLF4J_VERSION),
            maven.getJar("org.slf4j", "log4j-over-slf4j", SLF4J_VERSION));
  }

  public static List<Path> getOsgiBootstrapClasspath(Path engineDirectory) {
    if (engineDirectory == null || !Files.isDirectory(engineDirectory)) {
      throw new RuntimeException("The engineDirectory is missing: " + engineDirectory);
    }
    var classPathFiles = new ArrayList<Path>();
    var libBootDir = engineDirectory.resolve(OsgiDir.INSTALL_AREA).resolve(OsgiDir.LIB_BOOT);
    var jarFilter = new SuffixFileFilter(".jar");
    addToClassPath(classPathFiles, libBootDir, jarFilter);
    return classPathFiles;
  }

  private static void addToClassPath(List<Path> classPathFiles, Path dir, IOFileFilter fileFilter) {
    if (Files.isDirectory(dir)) {
      classPathFiles.addAll(FileUtils.listFiles(dir.toFile(), fileFilter, null).stream().map(f -> f.toPath()).toList());
    }
  }

  public static List<Path> getIvyEngineClassPathFiles(Path engineDirectory) {
    if (engineDirectory == null) {
      return List.of();
    }

    var classPathFiles = new ArrayList<Path>();
    for (var libDirPath : ENGINE_LIB_DIRECTORIES) {
      var jarDir = engineDirectory.resolve(libDirPath);
      if (!Files.isDirectory(jarDir)) {
        continue;
      }
      classPathFiles.addAll(FileUtils.listFiles(jarDir.toFile(), new String[] {"jar"}, true).stream().map(f -> f.toPath()).toList());
    }
    return classPathFiles;
  }

  public void writeEngineClasspathJar(Path engineDirectory) throws IOException {
    writeEngineClasspathJar(getIvyEngineClassPathFiles(engineDirectory));
  }

  private void writeEngineClasspathJar(List<Path> ivyEngineClassPathFiles) throws IOException {
    var classPathJar = new SharedFile(maven.project).getEngineClasspathJar();
    var jar = new ClasspathJar(classPathJar);
    jar.setMainClass("ch.ivyteam.ivy.server.ServerLauncher");
    jar.createFileEntries(ivyEngineClassPathFiles);
  }

  private static URL[] toUrls(List<Path> ivyEngineClassPathFiles) throws MalformedURLException {
    var classPathUrls = new ArrayList<URL>();
    for (var file : ivyEngineClassPathFiles) {
      classPathUrls.add(file.toUri().toURL());
    }
    return classPathUrls.toArray(URL[]::new);
  }

  public static class MavenContext {
    private final RepositorySystem repoSystem;
    private final ArtifactRepository localRepository;
    private final MavenProject project;
    private final Log log;

    public MavenContext(RepositorySystem repoSystem, ArtifactRepository localRepository, MavenProject project,
            Log log) {
      this.repoSystem = repoSystem;
      this.localRepository = localRepository;
      this.project = project;
      this.log = log;
    }

    public Path getJar(String groupId, String artifactId, String version) {
      Artifact artifact = repoSystem.createArtifact(groupId, artifactId, version, "jar");
      File jar = new File(localRepository.getBasedir(), localRepository.pathOf(artifact));
      if (!jar.exists()) {
        log.warn("Failed to resolve '" + artifactId + "' from local repository in '" + jar + "'.");
      }
      return jar.toPath();
    }
  }
}
