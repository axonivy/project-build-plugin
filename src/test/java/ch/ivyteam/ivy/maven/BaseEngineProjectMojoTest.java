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
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.junit.Rule;

import ch.ivyteam.ivy.maven.engine.EngineClassLoaderFactory;
import ch.ivyteam.ivy.maven.util.ClasspathJar;
import ch.ivyteam.ivy.maven.util.SharedFile;

public class BaseEngineProjectMojoTest {
  protected static final String ENGINE_VERSION_TO_TEST = getTestEngineVersion();
  public static final String LOCAL_REPOSITORY = getLocalRepoPath();
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
    String nextMajor = new StringBuilder()
            .append(majorVersion + 1).append('.')
            .append(parsed.getMinorVersion()).append('.')
            .append(parsed.getIncrementalVersion())
            .toString();
    return nextMajor;
  }

  private static String getLocalRepoPath() {
    String locaRepoGlobalProperty = System.getProperty("maven.repo.local");
    if (locaRepoGlobalProperty != null) {
      return locaRepoGlobalProperty;
    }

    StringBuilder defaultHomePath = new StringBuilder(SystemUtils.USER_HOME)
            .append(File.separatorChar).append(".m2")
            .append(File.separatorChar).append("repository");
    return defaultHomePath.toString();
  }

  protected final static Collection<File> findFiles(File dir, String fileExtension) {
    if (!dir.exists()) {
      return Collections.emptyList();
    }
    return FileUtils.listFiles(dir, new String[] {fileExtension}, true);
  }

  private static final File evalEngineDir(AbstractEngineMojo mojo) {
    return new File(mojo.engineCacheDirectory,
            System.getProperty("ivy.engine.version", AbstractEngineMojo.DEFAULT_VERSION));
  }

  @Rule
  public ProjectMojoRule<InstallEngineMojo> installUpToDateEngineRule = new ProjectMojoRule<InstallEngineMojo>(
          new File("src/test/resources/base"), InstallEngineMojo.GOAL) {

    private static final String TIMESTAMP_FILE_NAME = "downloadtimestamp";

    @Override
    protected void before() throws Throwable {
      super.before();

      String alternateEngineListPageUrl = System.getProperty(InstallEngineMojo.ENGINE_LIST_URL_PROPERTY);
      if (alternateEngineListPageUrl != null) {
        getMojo().engineListPageUrl = new URL(alternateEngineListPageUrl);
      }
      getMojo().engineCacheDirectory = new File(CACHE_DIR);
      getMojo().ivyVersion = ENGINE_VERSION_TO_TEST;
      getMojo().engineDirectory = evalEngineDir(getMojo());
      getMojo().useLatestMinor = true;
      deleteOutdatedEngine();
      getMojo().execute();
      addTimestampToDownloadedEngine();
    }

    private void deleteOutdatedEngine() throws IOException {
      File engineDir = getMojo().getRawEngineDirectory();
      if (engineDir == null || !engineDir.exists()) {
        return;
      }

      File timestampFile = new File(engineDir, TIMESTAMP_FILE_NAME);
      if (isOlderThan24h(timestampFile)) {
        System.out.println("Deleting cached outdated engine.");
        FileUtils.deleteDirectory(engineDir);
      }
    }

    private boolean isOlderThan24h(File timestampFile) {
      if (!timestampFile.exists()) {
        return true;
      }

      try {
        BasicFileAttributes attr = Files.readAttributes(timestampFile.toPath(), BasicFileAttributes.class);
        long createTimeMillis = attr.creationTime().toMillis();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -1);
        long yesterday = cal.getTimeInMillis();
        boolean cachedEngineIsOlderThan24h = yesterday > createTimeMillis;
        return cachedEngineIsOlderThan24h;
      } catch (IOException ex) { // corrupt state - trigger re-download
        return true;
      }
    }

    private void addTimestampToDownloadedEngine() throws IOException {
      File engineDir = getMojo().getRawEngineDirectory();
      if (engineDir == null || !engineDir.exists()) {
        return;
      }
      File timestampFile = new File(engineDir, TIMESTAMP_FILE_NAME);
      timestampFile.createNewFile();
    }
  };

  public static class EngineMojoRule<T extends AbstractEngineMojo> extends ProjectMojoRule<T> {
    protected EngineMojoRule(String mojoName) {
      super(new File("src/test/resources/base"), mojoName);
    }

    @Override
    protected void before() throws Throwable {
      super.before();
      configureMojo(getMojo());
    }

    protected void configureMojo(AbstractEngineMojo newMojo) {
      newMojo.engineCacheDirectory = new File(CACHE_DIR);
      newMojo.engineDirectory = evalEngineDir(getMojo());
      newMojo.ivyVersion = ENGINE_VERSION_TO_TEST;
    }
  }

  protected class RunnableEngineMojoRule<T extends AbstractEngineMojo> extends EngineMojoRule<T> {

    public RunnableEngineMojoRule(String mojoName) {
      super(mojoName);
    }

    @Override
    protected void before() throws Throwable {
      super.before();
      provideClasspathJar();
    }

    private void provideClasspathJar() throws IOException {
      File cpJar = new SharedFile(project).getEngineOSGiBootClasspathJar();
      new ClasspathJar(cpJar).createFileEntries(EngineClassLoaderFactory
              .getOsgiBootstrapClasspath(installUpToDateEngineRule.getMojo().getRawEngineDirectory()));
    }

    @Override
    protected void after() { // give time to close output stream before we
                             // delete the project;
      sleep(1, TimeUnit.SECONDS);
      // will delete the maven project under test + logs
      super.after();
    }

    private void sleep(long duration, TimeUnit unit) {
      try {
        Thread.sleep(unit.toMillis(duration));
      } catch (InterruptedException ex) {
        throw new RuntimeException(ex);
      }
    }
  }

}