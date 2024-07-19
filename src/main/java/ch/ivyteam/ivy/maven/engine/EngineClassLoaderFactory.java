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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
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
public class EngineClassLoaderFactory {
  public interface OsgiDir {
    String INSTALL_AREA = "system";
    String PLUGINS = INSTALL_AREA + "/plugins";
    String LIB_BOOT = "lib/boot";
  }

  /** must match version in pom.xml */
  private static final String SLF4J_VERSION = "2.0.13";

  private static final List<String> ENGINE_LIB_DIRECTORIES = Arrays.asList(
          OsgiDir.INSTALL_AREA + "/" + OsgiDir.LIB_BOOT,
          OsgiDir.PLUGINS,
          OsgiDir.INSTALL_AREA + "/configuration/org.eclipse.osgi", // unpacked
                                                                    // jars from
                                                                    // OSGI
                                                                    // bundles
          "webapps" + File.separator + "ivy" + File.separator + "WEB-INF" + File.separator + "lib");

  private MavenContext maven;

  public EngineClassLoaderFactory(MavenContext mavenContext) {
    this.maven = mavenContext;
  }

  public URLClassLoader createEngineClassLoader(File engineDirectory) throws IOException {
    List<File> osgiClasspath = getOsgiBootstrapClasspath(engineDirectory);
    var filter = WildcardFileFilter.builder()
            .setWildcards("org.eclipse.osgi_*.jar")
            .get();
    addToClassPath(osgiClasspath, new File(engineDirectory, OsgiDir.PLUGINS), filter);
    osgiClasspath.addAll(0, getSlf4jJars());
    if (maven.log.isDebugEnabled()) {
      maven.log.debug("Configuring OSGi engine classpath:");
      osgiClasspath.stream().forEach(file -> maven.log.debug(" + " + file.getAbsolutePath()));
    }
    return new URLClassLoader(toUrls(osgiClasspath));
  }

  public List<File> getSlf4jJars() {
    return List.of(
            maven.getJar("org.slf4j", "slf4j-api", SLF4J_VERSION),
            maven.getJar("org.slf4j", "slf4j-simple", SLF4J_VERSION),
            maven.getJar("org.slf4j", "log4j-over-slf4j", SLF4J_VERSION));
  }

  public static List<File> getOsgiBootstrapClasspath(File engineDirectory) {
    if (engineDirectory == null || !engineDirectory.isDirectory()) {
      throw new RuntimeException("The engineDirectory is missing: " + engineDirectory);
    }
    List<File> classPathFiles = new ArrayList<>();
    addToClassPath(classPathFiles, new File(engineDirectory, OsgiDir.INSTALL_AREA + "/" + OsgiDir.LIB_BOOT),
            new SuffixFileFilter(".jar"));
    return classPathFiles;
  }

  private static void addToClassPath(List<File> classPathFiles, File dir, IOFileFilter fileFilter) {
    if (dir.isDirectory()) {
      classPathFiles.addAll(FileUtils.listFiles(dir, fileFilter, null));
    }
  }

  public static List<File> getIvyEngineClassPathFiles(File engineDirectory) {
    List<File> classPathFiles = new ArrayList<>();
    for (String libDirPath : ENGINE_LIB_DIRECTORIES) {
      File jarDir = new File(engineDirectory, libDirPath + File.separator);
      if (!jarDir.isDirectory()) {
        continue;
      }
      classPathFiles.addAll(FileUtils.listFiles(jarDir, new String[] {"jar"}, true));
    }
    return classPathFiles;
  }

  public void writeEngineClasspathJar(File engineDirectory) throws IOException {
    writeEngineClasspathJar(getIvyEngineClassPathFiles(engineDirectory));
  }

  private void writeEngineClasspathJar(List<File> ivyEngineClassPathFiles) throws IOException {
    File classPathJar = new SharedFile(maven.project).getEngineClasspathJar();
    ClasspathJar jar = new ClasspathJar(classPathJar);
    jar.setMainClass("ch.ivyteam.ivy.server.ServerLauncher");
    jar.createFileEntries(ivyEngineClassPathFiles);
  }

  private static URL[] toUrls(List<File> ivyEngineClassPathFiles) throws MalformedURLException {
    var classPathUrls = new ArrayList<URL>();
    for (File file : ivyEngineClassPathFiles) {
      classPathUrls.add(file.toURI().toURL());
    }
    return classPathUrls.toArray(URL[]::new);
  }

  public static class MavenContext {
    private final RepositorySystem repoSystem;
    private final MavenProject project;
    private final Log log;

    public MavenContext(RepositorySystem repoSystem, MavenProject project,
            Log log) {
      this.repoSystem = repoSystem;
      this.project = project;
      this.log = log;
    }

    public File getJar(String groupId, String artifactId, String version) {
      Artifact artifact = repoSystem.createArtifact(groupId, artifactId, version, "jar");
      var request = new ArtifactResolutionRequest();
      request.setArtifact(artifact);
      var response = repoSystem.resolve(request);
      if (response.isSuccess()) {
        return response.getArtifacts().iterator().next().getFile();
      } else {
        log.warn("Failed to resolve '" + artifactId + "' from local repository.");
      }
      return null;
    }
  }

}