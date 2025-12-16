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

import static ch.ivyteam.ivy.maven.AbstractEngineMojo.DEFAULT_VERSION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.IntStream;

import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.MediaType;

import ch.ivyteam.ivy.maven.engine.EngineClassLoaderFactory.OsgiDir;
import ch.ivyteam.ivy.maven.engine.EngineVersionEvaluator;
import ch.ivyteam.ivy.maven.engine.download.URLEngineDownloader;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;

@MojoTest
class TestInstallEngineMojo {

  private InstallEngineMojo mojo;
  private String mockBaseUrl;
  private ClientAndServer mock;

  @BeforeEach
  @InjectMojo(goal = InstallEngineMojo.GOAL)
  void startHttp(InstallEngineMojo install) {
    this.mojo = install;
    Integer[] ports = IntStream.rangeClosed(3333, 3333 + 20).boxed().toArray(Integer[]::new);
    this.mock = new ClientAndServer(ports);
    mockBaseUrl = "http://localhost:" + mock.getPort();
  }

  @AfterEach
  void stopHttp() {
    mock.stop();
  }

  private URL mockEngineZip() throws MalformedURLException {
    return URI.create(mockBaseUrl + "/fakeEngine.zip").toURL();
  }

  @Test
  void testEngineDownload_defaultBehaviour() throws Exception {
    String defaultEngineName = "AxonIvyEngine" + DEFAULT_VERSION + ".46949_" + InstallEngineMojo.DEFAULT_ARCH;
    var list = "<a href=\"" + mockBaseUrl + "/" + defaultEngineName + ".zip\">the engine!</a>";
    mock.when(request().withPath("/listPageUrl.html/"))
        .respond(response(list));

    var zip = createFakeEngineZip(mojo.ivyVersion);
    mockZipResponse(zip);

    // test setup can not expand expression ${settings.localRepository}: so we
    // setup an explicit temp dir!
    mojo.engineCacheDirectory = Files.createTempDirectory("tmpRepo");
    mojo.engineListPageUrl = URI.create(mockBaseUrl + "/listPageUrl.html").toURL();

    var defaultEngineDir = mojo.engineCacheDirectory.resolve(DEFAULT_VERSION);
    assertThat(defaultEngineDir).doesNotExist();
    assertThat(mojo.engineDownloadUrl)
        .as("Default config should favour to download an engine from the 'list page url'.")
        .isNull();
    assertThat(mojo.autoInstallEngine).isTrue();

    mojo.execute();

    assertThat(defaultEngineDir)
        .as("Engine must be automatically downloaded")
        .exists()
        .isDirectory()
        .as("Engine directory should automatically be set to subdir of the local repository cache.")
        .isEqualTo(mojo.getRawEngineDirectory());
  }

  private void mockZipResponse(Path zip) throws IOException {
    var bos = toBytes(zip);
    mock.when(request())
        .respond(HttpResponse.response()
            .withContentType(MediaType.parse("application/zip"))
            .withBody(bos));
  }

  private static byte[] toBytes(Path zip) throws IOException {
    try (var bos = new ByteArrayOutputStream();
        var in = Files.newInputStream(zip)) {
      in.transferTo(bos);
      return bos.toByteArray();
    }
  }

  private static Path createFakeEngineZip(String ivyVersion) throws IOException, ZipException {
    var zipDir = createFakeEngineDir(ivyVersion);
    var zipFile = zipDir.resolve("fake.zip");
    try (var zip = new ZipFile(zipFile.toFile())) {
      zip.createSplitZipFileFromFolder(zipDir.resolve(OsgiDir.INSTALL_AREA).toFile(), new ZipParameters(), false, 0);
    }
    return zipFile;
  }

  private static Path createFakeEngineDir(String ivyVersion) throws IOException {
    var fakeDir = createTempDir("fake");
    var fakeLibToDeclareVersion = fakeDir.resolve(getFakeLibraryPath(ivyVersion));
    Files.createDirectories(fakeLibToDeclareVersion.getParent());
    Files.createFile(fakeLibToDeclareVersion);
    return fakeDir;
  }

  private static Path createTempDir(String namePrefix) throws IOException {
    var tmpDir = Files.createTempDirectory(namePrefix);
    tmpDir.toFile().deleteOnExit();
    return tmpDir;
  }

  @Test
  void testEngineDownload_alreadyInstalledVersionWithinRange() throws Exception {
    final String version = AbstractEngineMojo.MINIMAL_COMPATIBLE_VERSION;
    mojo.engineDirectory = createFakeEngineDir(version);
    assertThat(mojo.engineDirectory).isDirectory();
    assertThat(mojo.engineDirectory.resolve(getFakeLibraryPath(version))).exists();

    mojo.ivyVersion = "[7.0.0,800.0.0)";
    mojo.autoInstallEngine = true;
    mojo.engineDownloadUrl = URI.create("http://localhost/fakeUri").toURL();

    mojo.execute();
    assertThat(mojo.engineDirectory).isNotEmptyDirectory();
    assertThat(mojo.engineDirectory.resolve(getFakeLibraryPath(version))).exists();
  }

  @Test
  void testEngineDownload_alreadyInstalledVersionTooOld() throws Exception {
    mockZipResponse(createFakeEngineZip(DEFAULT_VERSION));

    final String outdatedVersion = "6.5.1";
    mojo.engineDirectory = createFakeEngineDir(outdatedVersion);
    assertThat(mojo.engineDirectory).isDirectory();
    assertThat(mojo.engineDirectory.resolve(getFakeLibraryPath(outdatedVersion))).exists();

    var fix = AbstractEngineMojo.MINIMAL_COMPATIBLE_VERSION;
    mojo.ivyVersion = "[" + fix + "," + fix + "]";
    mojo.autoInstallEngine = true;
    mojo.engineDownloadUrl = mockEngineZip();

    mojo.execute();
    assertThat(mojo.engineDirectory).isNotEmptyDirectory();
    assertThat(mojo.engineDirectory.resolve(getFakeLibraryPath(outdatedVersion))).doesNotExist();
    assertThat(mojo.engineDirectory.resolve(getFakeLibraryPath(DEFAULT_VERSION))).exists();
  }

  @Test
  void testEngineDownload_ifNotExisting() throws Exception {
    mojo.engineDirectory = createTempDir("tmpEngine");
    assertThat(mojo.engineDirectory)
        .isDirectory()
        .isEmptyDirectory();

    mojo.autoInstallEngine = true;
    mockZipResponse(createFakeEngineZip(mojo.ivyVersion));
    mojo.engineDownloadUrl = mockEngineZip();

    mojo.execute();
    assertThat(mojo.engineDirectory).isNotEmptyDirectory();
  }

  @Test
  void testEngineDownload_skipNonOsgiEngineInCache() throws Exception {
    mojo.engineDirectory = null;
    mojo.ivyVersion = "[7.0.0,7.1.0]";
    mojo.restrictVersionToMinimalCompatible = false;
    mojo.autoInstallEngine = true;

    var osgiDir = mojo.engineCacheDirectory.resolve("7.0.0").resolve(getFakeLibraryPath("7.0.0"));
    Files.createDirectories(osgiDir);

    var nonOsgiDir = mojo.engineCacheDirectory.resolve("7.1.0");
    Files.createDirectories(nonOsgiDir);

    mojo.execute();
    assertThat(mojo.engineDirectory.getFileName().toString()).isEqualTo("7.0.0");
  }

  @Test
  void testEngineDownload_existingTmpFileNotOverwritten() throws Exception {
    mojo.engineDirectory = createTempDir("tmpEngine");

    var alreadyExistingFile = mojo.getDownloadDirectory().resolve("fakeEngine.zip");
    if (!Files.exists(alreadyExistingFile)) {
      Files.createFile(alreadyExistingFile);
    }

    mojo.autoInstallEngine = true;
    mockZipResponse(createFakeEngineZip(DEFAULT_VERSION));
    mojo.engineDownloadUrl = mockEngineZip();

    mojo.execute();

    assertThat(alreadyExistingFile).exists();
  }

  @Test
  void testEngineDownload_overProxy() throws Exception {
    mojo.engineDirectory = createTempDir("tmpEngine");

    var alreadyExistingFile = mojo.getDownloadDirectory().resolve("fakeEngine.zip");
    if (!Files.exists(alreadyExistingFile)) {
      Files.createFile(alreadyExistingFile);
    }

    mojo.autoInstallEngine = true;
    mockZipResponse(createFakeEngineZip(DEFAULT_VERSION));

    mojo.engineDownloadUrl = URI.create("http://localhost:7123/fakeEngine.zip").toURL(); // not reachable: but proxy knows how :)
    var downloader = (URLEngineDownloader) mojo.getDownloader();
    try {
      downloader.downloadEngine();
      failBecauseExceptionWasNotThrown(MojoExecutionException.class);
    } catch (MojoExecutionException ex) {
      assertThat(ex).hasMessageStartingWith("Failed to download engine from 'http://localhost");
    }

    downloader.proxies = this::localTestProxy;
    var downloaded = downloader.downloadEngine();
    assertThat(downloaded)
        .as("served file via proxy")
        .exists();
  }

  private ProxyInfo localTestProxy(@SuppressWarnings("unused") String protocol) {
    var proxy = new ProxyInfo();
    proxy.setHost("localhost");
    proxy.setPort(mock.getPort());
    proxy.setType("http");
    return proxy;
  }

  @Test
  void testEngineDownload_validatesDownloadedVersion() throws Exception {
    mojo.engineDirectory = createTempDir("tmpEngine");
    mojo.autoInstallEngine = true;
    mojo.ivyVersion = "9999.0.0";
    mockZipResponse(createFakeEngineZip(DEFAULT_VERSION));
    mojo.engineDownloadUrl = mockEngineZip();

    try {
      mojo.execute();
      failBecauseExceptionWasNotThrown(MojoExecutionException.class);
    } catch (MojoExecutionException ex) {
      assertThat(ex).hasMessageStartingWith("Automatic installation of an ivyEngine failed.");
    }
  }

  @Test
  void testEngineDownload_canDisableAutoDownload() throws Exception {
    mojo.engineDirectory = createTempDir("tmpEngine");
    mojo.autoInstallEngine = false;

    try {
      mojo.execute();
      failBecauseExceptionWasNotThrown(MojoExecutionException.class);
    } catch (MojoExecutionException ex) {
      assertThat(ex).hasMessageContaining("no valid ivy Engine is available");
    }
  }

  @Test
  void testEngineLinkFinder_absolute_http() throws Exception {
    mojo.ivyVersion = "[7.0.0,7.1.0]";
    mojo.restrictVersionToMinimalCompatible = false;
    mojo.osArchitecture = "Windows_x86";
    assertThat(findLink("<a href=\"http://developer.axonivy.com/download/7.0.0/AxonIvyEngine7.0.0.46949_Windows_x86.zip\">the latest engine</a>"))
        .isEqualTo("http://developer.axonivy.com/download/7.0.0/AxonIvyEngine7.0.0.46949_Windows_x86.zip");
  }

  @Test
  void testEngineLinkFinder_absolute_https() throws Exception {
    mojo.ivyVersion = "[7.0.0,7.1.0]";
    mojo.restrictVersionToMinimalCompatible = false;
    mojo.osArchitecture = "Windows_x86";
    assertThat(findLink("<a href=\"https://developer.axonivy.com/download/7.0.0/AxonIvyEngine7.0.0.46949_Windows_x86.zip\">the latest engine</a>"))
        .isEqualTo("https://developer.axonivy.com/download/7.0.0/AxonIvyEngine7.0.0.46949_Windows_x86.zip");
  }

  @Test
  void testEngineLinkFinder_relative() throws Exception {
    mojo.ivyVersion = "[7.0.0,7.1.0]";
    mojo.restrictVersionToMinimalCompatible = false;
    mojo.osArchitecture = "Windows_x86";
    mojo.engineListPageUrl = URI.create("http://localhost/").toURL();
    assertThat(findLink("<a href=\"7.0.0/AxonIvyEngine7.0.0.46949_Windows_x86.zip\">the latest engine</a>"))
        .isEqualTo("http://localhost/7.0.0/AxonIvyEngine7.0.0.46949_Windows_x86.zip");
  }

  @Test
  void testEngineLinkFinder_sprintVersionQualifier() throws Exception {
    mojo.ivyVersion = "[7.0.0,7.1.0]";
    mojo.restrictVersionToMinimalCompatible = false;
    mojo.osArchitecture = "Windows_x64";
    assertThat(findLink(
        "<a href=\"http://www.ivyteam.ch/downloads/XIVY/Saentis/7.0.0-S2/AxonIvyEngine7.0.0.47245.S2_Windows_x64.zip\">Axon Ivy Engine Windows x64</a>"))
            .isEqualTo("http://www.ivyteam.ch/downloads/XIVY/Saentis/7.0.0-S2/AxonIvyEngine7.0.0.47245.S2_Windows_x64.zip");
  }

  @Test
  void testEngineLinkFinder_wrongVersion() throws Exception {
    mojo.ivyVersion = DEFAULT_VERSION;
    mojo.osArchitecture = "Windows_x86";
    try {
      findLink("<a href=\"6.2.0/AxonIvyEngine6.2.0.46949_Windows_x86.zip\">the latest engine</a>");
      failBecauseExceptionWasNotThrown(MojoExecutionException.class);
    } catch (MojoExecutionException ex) {
      assertThat(ex).hasMessageStartingWith(
          "Could not find a link to engine for version '" + DEFAULT_VERSION + "'");
    }
  }

  @Test
  void testEngineLinkFinder_wrongArchitecture() throws Exception {
    mojo.ivyVersion = DEFAULT_VERSION;
    mojo.osArchitecture = "Linux_x86";
    try {
      findLink("<a href=\"" + DEFAULT_VERSION + "/AxonIvyEngine"
          + DEFAULT_VERSION + ".46949_Windows_x86.zip\">the latest engine</a>");
      failBecauseExceptionWasNotThrown(MojoExecutionException.class);
    } catch (MojoExecutionException ex) {
      assertThat(ex).hasMessageStartingWith(
          "Could not find a link to engine for version '" + DEFAULT_VERSION + "'");
    }
  }

  @Test
  void testEngineLinkFinder_multipleLinks() throws Exception {
    mojo.ivyVersion = "[7.0.0,7.1.0]";
    mojo.restrictVersionToMinimalCompatible = false;
    mojo.osArchitecture = "Linux_x86";
    mojo.engineListPageUrl = URI.create("http://localhost/").toURL();

    assertThat(findLink(
        "<a href=\"7.0.0/AxonIvyEngine7.0.0.46949_Windows_x86.zip\">the latest engine</a>" // windows
            + "<a href=\"7.0.0/AxonIvyEngine7.0.0.46949_Linux_x86.zip\">the latest engine</a>")) // linux
                .isEqualTo("http://localhost/7.0.0/AxonIvyEngine7.0.0.46949_Linux_x86.zip");
  }

  private String findLink(String html) throws MojoExecutionException, URISyntaxException, IOException {
    try (var in = new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8))) {
      return getUrlDownloader().findEngineDownloadUrl(in).toExternalForm();
    }
  }

  @Test
  void testDefaultListPage_isAvailable() throws Exception {
    boolean run = Boolean.parseBoolean(System.getProperty("run.public.download.test"));
    if (!run) {
      return;
    }

    String engineUrl = getUrlDownloader().findEngineDownloadUrl(mojo.engineListPageUrl.openStream()).toExternalForm();
    assertThat(engineUrl)
        .as("The default engine list page url '" + mojo.engineListPageUrl.toExternalForm() + "' "
            + "must provide an engine for the current default engine version '" + mojo.ivyVersion
            + "'.")
        .contains(mojo.ivyVersion);
  }

  private URLEngineDownloader getUrlDownloader() throws MojoExecutionException {
    return (URLEngineDownloader) mojo.getDownloader();
  }

  @Test
  void testIvyVersion_mustMatchMinimalPluginVersion() {
    mojo.ivyVersion = "5.1.0";
    try {
      mojo.execute();
      failBecauseExceptionWasNotThrown(MojoExecutionException.class);
    } catch (MojoExecutionException ex) {
      assertThat(ex).hasMessageContaining("'5.1.0' is lower than the minimal compatible version");
    }
  }

  @Test
  void testZipFileEngineVersionParser() {
    assertThat(InstallEngineMojo.ivyEngineVersionOfZip("AxonIvyEngine6.1.1.51869_Linux_x64.zip"))
        .isEqualTo("6.1.1");
    assertThat(InstallEngineMojo.ivyEngineVersionOfZip("AxonIvyEngine6.2_Windows_x64.zip"))
        .isEqualTo("6.2");
    assertThat(InstallEngineMojo
        .ivyEngineVersionOfZip("AxonIvyDesigner6.1.1-SNAPSHOT.51869-win32.win32.x86_64.zip"))
            .isEqualTo("6.1.1");
    assertThat(InstallEngineMojo.ivyEngineVersionOfZip("AxonIvyEngine_Linux_x64.zip"))
        .isEqualTo("AxonIvyEngine_Linux_x64.zip"); // do not return null!
  }

  private static String getFakeLibraryPath(final String version) {
    return OsgiDir.PLUGINS + "/" + EngineVersionEvaluator.LIBRARY_ID + "_" + version + ".51869.jar";
  }

}
