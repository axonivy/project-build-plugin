/*
 * Copyright (C) 2015 AXON IVY AG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.ivyteam.ivy.maven;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.wink.client.MockHttpServer;
import org.junit.Rule;
import org.junit.Test;

import ch.ivyteam.ivy.maven.InstallEngineMojo.EngineDownloader;
import ch.ivyteam.ivy.maven.engine.EngineVersionEvaluator;
import mockit.Mock;
import mockit.MockUp;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;

public class TestInstallEngineMojo
{
  private InstallEngineMojo mojo;

  @Rule
  public ProjectMojoRule<InstallEngineMojo> rule = 
    new ProjectMojoRule<InstallEngineMojo>(new File("src/test/resources/base"), InstallEngineMojo.GOAL)
    {
      @Override
      protected void before() throws Throwable 
      {
        super.before();
        TestInstallEngineMojo.this.mojo = getMojo();
      }
    };
    
  @Test
  public void testEngineDownload_defaultBehaviour() throws Exception
  {
    MockHttpServer mockServer = new MockHttpServer(3333);
    try
    {
      mockServer.startServer();
      String baseUrl = "http://localhost:" + mockServer.getServerPort();
      MockHttpServer.MockHttpServerResponse listPageResponse = new MockHttpServer.MockHttpServerResponse();
      String defaultEngineName = "AxonIvyEngine" + AbstractEngineMojo.DEFAULT_VERSION + ".46949_Windows_x64";
      listPageResponse.setMockResponseContent("<a href=\""+baseUrl+"/" + defaultEngineName + ".zip\">the engine!</a>");
      File engineZip = createFakeEngineZip(mojo.ivyVersion);
      MockHttpServer.MockHttpServerResponse engineZipResponse = createFakeZipResponse(engineZip);
      mockServer.setMockHttpServerResponses(listPageResponse, engineZipResponse);
      
      // test setup can not expand expression ${settings.localRepository}: so we setup an explicit temp dir!
      mojo.engineCacheDirectory = Files.createTempDirectory("tmpRepo").toFile(); 
      mojo.engineListPageUrl = new URL(baseUrl + "/listPageUrl.html");
      
      File defaultEngineDir = new File(mojo.engineCacheDirectory, AbstractEngineMojo.DEFAULT_VERSION);
      assertThat(defaultEngineDir).doesNotExist();
      assertThat(mojo.engineDownloadUrl).as("Default config should favour to download an engine from the 'list page url'.").isNull();
      assertThat(mojo.autoInstallEngine).isTrue();
      
      mojo.execute();
      
      assertThat(defaultEngineDir)
        .as("Engine must be automatically downloaded")
        .exists().isDirectory();
      assertThat(defaultEngineDir)
        .as("Engine directory should automatically be set to subdir of the local repository cache.")
        .isEqualTo(mojo.getEngineDirectory());
    }
    finally
    {
      mockServer.stopServer();
    }
  }

  private static MockHttpServer.MockHttpServerResponse createFakeZipResponse(File zip) throws IOException, FileNotFoundException
  {
    MockHttpServer.MockHttpServerResponse engineZipResponse = new MockHttpServer.MockHttpServerResponse();
    engineZipResponse.setMockResponseContentType("application/zip");
    FileInputStream fis = new FileInputStream(zip);
    byte[] zipBytes = IOUtils.toByteArray(fis);
    engineZipResponse.setMockResponseContent(zipBytes);
    return engineZipResponse;
  }
  
  private static File createFakeEngineZip(String ivyVersion) throws IOException, ZipException
  {
    File zipDir = createFakeEngineDir(ivyVersion);
    File zipFile = new File(zipDir, "fake.zip");
    ZipFile zip = new ZipFile(zipFile);
    zip.createZipFileFromFolder(new File(zipDir, "lib"), new ZipParameters(), false, 0);
    return zipFile;
  }

  private static File createFakeEngineDir(String ivyVersion) throws IOException
  {
    File fakeDir = createTempDir("fake");
    File fakeLibToDeclareVersion = new File(fakeDir, getFakeLibraryPath(ivyVersion));
    fakeLibToDeclareVersion.getParentFile().mkdirs();
    fakeLibToDeclareVersion.createNewFile();
    return fakeDir;
  }
  
  private static File createTempDir(String namePrefix) throws IOException
  {
    File tmpDir = Files.createTempDirectory(namePrefix).toFile();
    tmpDir.deleteOnExit();
    return tmpDir;
  }
  
  @Test
  public void testEngineDownload_alreadyInstalledVersionWithinRange() throws Exception
  {
    final String version = "7.1.0";
    mojo.engineDirectory = createFakeEngineDir(version);
    assertThat(mojo.engineDirectory).isDirectory();
    assertThat(new File(mojo.engineDirectory, getFakeLibraryPath(version))).exists();

    mojo.ivyVersion = "[7.0.0,8.0.0)";
    mojo.autoInstallEngine = true;
    mojo.engineDownloadUrl = new MockUp<java.net.URL>()
      {
        @Mock(maxInvocations = 0)
        public String toExternalForm()
        {
          return null;
        }
      
        @Mock(maxInvocations = 0)
        public InputStream openStream()
        {
          return null;
        }
      }.getMockInstance();

    mojo.execute();
    assertThat(mojo.engineDirectory.listFiles()).isNotEmpty();
    assertThat(new File(mojo.engineDirectory, getFakeLibraryPath(version))).exists();
  }

  @Test
  public void testEngineDownload_alreadyInstalledVersionTooOld() throws Exception
  {
    final String outdatedVersion = "6.1.0";
    mojo.engineDirectory = createFakeEngineDir(outdatedVersion);
    assertThat(mojo.engineDirectory).isDirectory();
    assertThat(new File(mojo.engineDirectory, getFakeLibraryPath(outdatedVersion))).exists();
    
    mojo.ivyVersion = "[7.0.0,8.0.0)";
    mojo.autoInstallEngine = true;
    final String downloadVersion = "7.2.0";
    mojo.engineDownloadUrl = new MockedIvyEngineDownloadUrl(downloadVersion).getMockInstance();
    
    mojo.execute();
    assertThat(mojo.engineDirectory.listFiles()).isNotEmpty();
    assertThat(new File(mojo.engineDirectory, getFakeLibraryPath(outdatedVersion))).doesNotExist();
    assertThat(new File(mojo.engineDirectory, getFakeLibraryPath(downloadVersion))).exists();
  }

  @Test
  public void testEngineDownload_ifNotExisting() throws Exception
  {
    mojo.engineDirectory = createTempDir("tmpEngine");
    assertThat(mojo.engineDirectory).isDirectory();
    assertThat(mojo.engineDirectory.listFiles()).isEmpty();
    
    mojo.autoInstallEngine = true;
    mojo.engineDownloadUrl = new MockedIvyEngineDownloadUrl(mojo.ivyVersion).getMockInstance();
    
    mojo.execute();
    assertThat(mojo.engineDirectory.listFiles()).isNotEmpty();
  }
  
  @Test
  public void testEngineDownload_existingTmpFileNotOverwritten() throws Exception
  {
    mojo.engineDirectory = createTempDir("tmpEngine");

    File alreadyExistingFile = new File(mojo.getDownloadDirectory(), MockedIvyEngineDownloadUrl.ENGINE_FILE_NAME); 
    alreadyExistingFile.createNewFile();
    
    mojo.autoInstallEngine = true;
    mojo.engineDownloadUrl = new MockedIvyEngineDownloadUrl(mojo.ivyVersion).getMockInstance();
    
    mojo.execute();
    
    assertThat(alreadyExistingFile).exists();
  }
  
  @Test
  public void testEngineDownload_validatesDownloadedVersion() throws Exception
  {
    mojo.engineDirectory = createTempDir("tmpEngine");
    mojo.autoInstallEngine = true;
    mojo.ivyVersion = "9999.0.0";
    mojo.engineDownloadUrl = new MockedIvyEngineDownloadUrl(AbstractEngineMojo.DEFAULT_VERSION).getMockInstance();
    
    try
    {
      mojo.execute();
      failBecauseExceptionWasNotThrown(MojoExecutionException.class);
    }
    catch(MojoExecutionException ex)
    {
      assertThat(ex).hasMessageStartingWith("Automatic installation of an ivyEngine failed.");
    }
  }
  
  @Test
  public void testEngineDownload_canDisableAutoDownload() throws Exception
  {
    mojo.engineDirectory = createTempDir("tmpEngine");
    mojo.autoInstallEngine = false;
    
    try
    {
      mojo.execute();
      failBecauseExceptionWasNotThrown(MojoExecutionException.class);
    }
    catch(MojoExecutionException ex)
    {
      assertThat(ex).hasMessageContaining("no valid ivy Engine is available");
    }
  }
  
  private static class MockedIvyEngineDownloadUrl extends MockUp<java.net.URL>
  {
    private static final String ENGINE_FILE_NAME = "fakeEngine.zip";
    private String ivyVersion;

    private MockedIvyEngineDownloadUrl(String ivyVersion)
    {
      this.ivyVersion = ivyVersion;
    }
    
    @Mock
    public InputStream openStream()
    {
      try
      {
        return new FileInputStream(createFakeEngineZip(ivyVersion));
      }
      catch (Exception ex)
      {
        fail("Mock URL error", ex);
        return null;
      }
    }
    
    @Mock
    public String toExternalForm()
    {
      return "http://localhost/" + ENGINE_FILE_NAME;
    }
  }

  @Test
  public void testEngineLinkFinder_absolute() throws Exception
  {
    mojo.ivyVersion = "6.1.0";
    mojo.osArchitecture = "Windows_x86";
    assertThat(findLink("<a href=\"http://developer.axonivy.com/download/6.1.0/AxonIvyEngine6.1.0.46949_Windows_x86.zip\">the latest engine</a>"))
      .isEqualTo("http://developer.axonivy.com/download/6.1.0/AxonIvyEngine6.1.0.46949_Windows_x86.zip");
  }
  
  @Test
  public void testEngineLinkFinder_relative() throws Exception
  {
    mojo.ivyVersion = "6.1.0";
    mojo.osArchitecture = "Windows_x86";
    mojo.engineListPageUrl = new URL("http://localhost/");
    assertThat(findLink("<a href=\"6.1.0/AxonIvyEngine6.1.0.46949_Windows_x86.zip\">the latest engine</a>"))
      .isEqualTo("http://localhost/6.1.0/AxonIvyEngine6.1.0.46949_Windows_x86.zip");
  }
  

  @Test
  public void testEngineLinkFinder_sprintVersionQualifier() throws Exception
  {
    mojo.ivyVersion = "6.1.0";
    mojo.osArchitecture = "Windows_x64";
    assertThat(findLink("<a href=\"http://www.ivyteam.ch/downloads/XIVY/Saentis/6.1.0-S2/AxonIvyEngine6.1.0.47245.S2_Windows_x64.zip\">Axon.ivy Engine Windows x64</a>"))
      .isEqualTo("http://www.ivyteam.ch/downloads/XIVY/Saentis/6.1.0-S2/AxonIvyEngine6.1.0.47245.S2_Windows_x64.zip");
  }
  
  @Test
  public void testEngineLinkFinder_wrongVersion() throws Exception
  {
    mojo.ivyVersion = "6.1.0";
    mojo.osArchitecture = "Windows_x86";
    try
    {
      findLink("<a href=\"6.2.0/AxonIvyEngine6.2.0.46949_Windows_x86.zip\">the latest engine</a>");
      failBecauseExceptionWasNotThrown(MojoExecutionException.class);
    }
    catch(MojoExecutionException ex)
    {
      assertThat(ex).hasMessageStartingWith("Could not find a link to engine for version '6.1.0'");
    }
  }
  
  @Test
  public void testEngineLinkFinder_wrongArchitecture() throws Exception
  {
    mojo.ivyVersion = "6.1.0";
    mojo.osArchitecture = "Linux_x86";
    try
    {
      findLink("<a href=\"6.1.0/AxonIvyEngine6.1.0.46949_Windows_x86.zip\">the latest engine</a>");
      failBecauseExceptionWasNotThrown(MojoExecutionException.class);
    }
    catch(MojoExecutionException ex)
    {
      assertThat(ex).hasMessageStartingWith("Could not find a link to engine for version '6.1.0'");
    }
  }
  
  @Test
  public void testEngineLinkFinder_multipleLinks() throws Exception
  {
    mojo.ivyVersion = "6.1.0";
    mojo.osArchitecture = "Linux_x86";
    mojo.engineListPageUrl = new URL("http://localhost/");

    assertThat(findLink(
            "<a href=\"6.1.0/AxonIvyEngine6.1.0.46949_Windows_x86.zip\">the latest engine</a>" // windows
          + "<a href=\"6.1.0/AxonIvyEngine6.1.0.46949_Linux_x86.zip\">the latest engine</a>")) // linux
             .isEqualTo("http://localhost/6.1.0/AxonIvyEngine6.1.0.46949_Linux_x86.zip");
  }
  
  private String findLink(String html) throws MojoExecutionException, MalformedURLException
  {
    EngineDownloader engineDownloader = mojo.new EngineDownloader();
    return engineDownloader.findEngineDownloadUrl(IOUtils.toInputStream(html)).toExternalForm();
  }
  
  @Test
  public void testDefaultListPage_isAvailable() throws Exception
  {
    EngineDownloader engineDownloader = mojo.new EngineDownloader();
    String engineUrl = engineDownloader.findEngineDownloadUrl(mojo.engineListPageUrl.openStream()).toExternalForm();
    assertThat(engineUrl)
      .as("The default engine list page url '"+mojo.engineListPageUrl.toExternalForm()+"' "
              + "must provide an engine for the current default engine version '"+mojo.ivyVersion+"'.")
      .contains(mojo.ivyVersion);
  }
  
  @Test
  public void testIvyVersion_mustMatchMinimalPluginVersion()
  {
    mojo.ivyVersion = "5.1.0";
    try
    {
      mojo.execute();
      failBecauseExceptionWasNotThrown(MojoExecutionException.class);
    }
    catch (MojoExecutionException ex)
    {
      assertThat(ex).hasMessageContaining("'5.1.0' is lower than the minimal compatible version");
    }
  }
  
  @Test
  public void testZipFileEngineVersionParser()
  {
    assertThat(InstallEngineMojo.ivyEngineVersionOfZip("AxonIvyEngine6.1.1.51869_Linux_x64.zip"))
      .isEqualTo("6.1.1");
    assertThat(InstallEngineMojo.ivyEngineVersionOfZip("AxonIvyEngine6.2_Windows_x64.zip"))
      .isEqualTo("6.2");
    assertThat(InstallEngineMojo.ivyEngineVersionOfZip("AxonIvyDesigner6.1.1-SNAPSHOT.51869-win32.win32.x86_64.zip"))
      .isEqualTo("6.1.1");
    assertThat(InstallEngineMojo.ivyEngineVersionOfZip("AxonIvyEngine_Linux_x64.zip"))
      .isEqualTo("AxonIvyEngine_Linux_x64.zip"); // do not return null!
  }
  
  private static String getFakeLibraryPath(final String version)
  {
    return "lib/ivy/"+EngineVersionEvaluator.LIBRARY_ID+"-" + version + "-server.jar";
  }

  
}
