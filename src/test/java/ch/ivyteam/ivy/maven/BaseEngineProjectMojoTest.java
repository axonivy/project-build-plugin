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

package ch.ivyteam.ivy.maven;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Calendar;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.plugin.MojoExecutionException;

import ch.ivyteam.ivy.maven.extension.LocalRepoTest;
import ch.ivyteam.ivy.maven.util.PathUtils;

public class BaseEngineProjectMojoTest {
  protected static final String ENGINE_VERSION_TO_TEST = getTestEngineVersion();
  public static final String LOCAL_REPOSITORY = LocalRepoTest.path().toString();
  protected static final String CACHE_DIR = LOCAL_REPOSITORY + "/.cache/ivy-dev";

  private static String getTestEngineVersion() {
    return System.getProperty("ivy.engine.version", toRange(AbstractEngineMojo.DEFAULT_VERSION));
  }

  private static String toRange(String version) {
    String nextMajor = getNextMajor(version);
    return "[" + version + "," + nextMajor + ")";
  }

  private static String getNextMajor(String version) {
    DefaultArtifactVersion parsed = new DefaultArtifactVersion(version);
    int majorVersion = parsed.getMajorVersion();
    return new StringBuilder()
        .append(majorVersion + 1).append('.')
        .append(parsed.getMinorVersion()).append('.')
        .append(parsed.getIncrementalVersion())
        .toString();
  }

  private static final Path evalEngineDir(AbstractEngineMojo mojo) {
    return mojo.engineCacheDirectory.resolve(System.getProperty("ivy.engine.version", AbstractEngineMojo.DEFAULT_VERSION));
  }

  private static final String TIMESTAMP_FILE_NAME = "downloadtimestamp";

  public static void provideEngine(AbstractEngineMojo mojo) throws MalformedURLException, MojoExecutionException, IOException {
    String alternateEngineListPageUrl = System.getProperty(InstallEngineMojo.ENGINE_LIST_URL_PROPERTY);
    if (alternateEngineListPageUrl != null && mojo instanceof InstallEngineMojo install) {
      install.engineListPageUrl = URI.create(alternateEngineListPageUrl).toURL();
    }
    mojo.engineCacheDirectory = Path.of(CACHE_DIR);
    mojo.ivyVersion = ENGINE_VERSION_TO_TEST;
    mojo.engineDirectory = evalEngineDir(mojo);
    if (mojo instanceof InstallEngineMojo install) {
      mojo.useLatestMinor = true;
      deleteOutdatedEngine(mojo);
      install.execute();
      addTimestampToDownloadedEngine(mojo);
    }
  }

  private static void deleteOutdatedEngine(AbstractEngineMojo mojo) {
    var engineDir = mojo.getRawEngineDirectory();
    if (engineDir == null || !Files.exists(engineDir)) {
      return;
    }

    var timestampFile = engineDir.resolve(TIMESTAMP_FILE_NAME);
    if (isOlderThan24h(timestampFile.toFile())) {
      System.out.println("Deleting cached outdated engine.");
      PathUtils.delete(engineDir);
    }
  }

  private static boolean isOlderThan24h(File timestampFile) {
    if (!timestampFile.exists()) {
      return true;
    }

    try {
      BasicFileAttributes attr = Files.readAttributes(timestampFile.toPath(), BasicFileAttributes.class);
      long createTimeMillis = attr.creationTime().toMillis();
      Calendar cal = Calendar.getInstance();
      cal.add(Calendar.DAY_OF_YEAR, -1);
      long yesterday = cal.getTimeInMillis();
      return yesterday > createTimeMillis;
    } catch (IOException ex) { // corrupt state - trigger re-download
      return true;
    }
  }

  private static void addTimestampToDownloadedEngine(AbstractEngineMojo mojo) throws IOException {
    var engineDir = mojo.getRawEngineDirectory();
    if (engineDir == null || !Files.exists(engineDir)) {
      return;
    }
    var timestampFile = engineDir.resolve(TIMESTAMP_FILE_NAME);
    timestampFile.toFile().createNewFile();
  }

  public static void configureMojo(AbstractEngineMojo newMojo) {
    newMojo.engineCacheDirectory = Path.of(CACHE_DIR);
    newMojo.engineDirectory = evalEngineDir(newMojo);
    newMojo.ivyVersion = ENGINE_VERSION_TO_TEST;
  }

}
