package ch.ivyteam.ivy.maven;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;
import static org.fest.assertions.api.Assertions.failBecauseExceptionWasNotThrown;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;

import mockit.Mock;
import mockit.MockUp;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.wink.client.MockHttpServer;
import org.junit.Rule;
import org.junit.Test;

import ch.ivyteam.ivy.maven.EnsureInstalledEngineMojo.EngineDownloader;

public class TestEnsureInstalledEngineMojo
{
  private EnsureInstalledEngineMojo mojo;

  @Rule
  public ProjectMojoRule<EnsureInstalledEngineMojo> rule = 
    new ProjectMojoRule<EnsureInstalledEngineMojo>(new File("src/test/resources/base"), EnsureInstalledEngineMojo.GOAL)
    {
      @Override
      protected void before() throws Throwable 
      {
        super.before();
        TestEnsureInstalledEngineMojo.this.mojo = getMojo();
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
      listPageResponse.setMockResponseContent("<a href=\""+baseUrl+"/AxonIvyEngine6.0.0.46949_Windows_x64.zip\">the engine!</a>");
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
    File zipDir = createTempDir("zip");
    File fakeLibToDeclareVersion = new File(zipDir, "lib/ivy/ch.ivyteam.fake-"+ivyVersion+"-server.jar");
    fakeLibToDeclareVersion.getParentFile().mkdirs();
    fakeLibToDeclareVersion.createNewFile();
    File zipFile = new File(zipDir, "fake.zip");
    ZipFile zip = new ZipFile(zipFile);
    zip.createZipFileFromFolder(new File(zipDir, "lib"), new ZipParameters(), false, 0);
    return zipFile;
  }
  
  private static File createTempDir(String namePrefix) throws IOException
  {
    File tmpDir = Files.createTempDirectory(namePrefix).toFile();
    tmpDir.deleteOnExit();
    return tmpDir;
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
      return "http://localhost/fakeEngine.zip";
    }
  }

  @Test
  public void testEngineLinkFinder_absolute() throws Exception
  {
    mojo.ivyVersion = "5.1.0";
    mojo.osArchitecture = "Windows_x86";
    assertThat(findLink("<a href=\"http://developer.axonivy.com/download/5.1.0/AxonIvyEngine5.1.0.46949_Windows_x86.zip\">the latest engine</a>"))
      .isEqualTo("http://developer.axonivy.com/download/5.1.0/AxonIvyEngine5.1.0.46949_Windows_x86.zip");
  }
  
  @Test
  public void testEngineLinkFinder_relative() throws Exception
  {
    mojo.ivyVersion = "5.1.0";
    mojo.osArchitecture = "Windows_x86";
    mojo.engineListPageUrl = new URL("http://localhost/");
    assertThat(findLink("<a href=\"5.1.0/AxonIvyEngine5.1.0.46949_Windows_x86.zip\">the latest engine</a>"))
      .isEqualTo("http://localhost/5.1.0/AxonIvyEngine5.1.0.46949_Windows_x86.zip");
  }
  

  @Test
  public void testEngineLinkFinder_sprintVersionQualifier() throws Exception
  {
    mojo.ivyVersion = "6.0.0";
    mojo.osArchitecture = "Windows_x64";
    assertThat(findLink("<a href=\"http://www.ivyteam.ch/downloads/XIVY/Saentis/6.0.0-S2/AxonIvyEngine6.0.0.47245.S2_Windows_x64.zip\">Axon.ivy Engine Windows x64</a>"))
      .isEqualTo("http://www.ivyteam.ch/downloads/XIVY/Saentis/6.0.0-S2/AxonIvyEngine6.0.0.47245.S2_Windows_x64.zip");
  }
  
  @Test
  public void testEngineLinkFinder_wrongVersion() throws Exception
  {
    mojo.ivyVersion = "6.0.0";
    mojo.osArchitecture = "Windows_x86";
    try
    {
      findLink("<a href=\"5.1.0/AxonIvyEngine5.1.0.46949_Windows_x86.zip\">the latest engine</a>");
      failBecauseExceptionWasNotThrown(MojoExecutionException.class);
    }
    catch(MojoExecutionException ex)
    {
      assertThat(ex).hasMessageStartingWith("Could not find a link to engine in version '6.0.0'");
    }
  }
  
  @Test
  public void testEngineLinkFinder_wrongArchitecture() throws Exception
  {
    mojo.ivyVersion = "5.1.0";
    mojo.osArchitecture = "Linux_x86";
    try
    {
      findLink("<a href=\"5.1.0/AxonIvyEngine5.1.0.46949_Windows_x86.zip\">the latest engine</a>");
      failBecauseExceptionWasNotThrown(MojoExecutionException.class);
    }
    catch(MojoExecutionException ex)
    {
      assertThat(ex).hasMessageStartingWith("Could not find a link to engine in version '5.1.0'");
    }
  }
  
  @Test
  public void testEngineLinkFinder_multipleLinks() throws Exception
  {
    mojo.ivyVersion = "5.1.0";
    mojo.osArchitecture = "Linux_x86";
    mojo.engineListPageUrl = new URL("http://localhost/");

    assertThat(findLink(
            "<a href=\"5.1.0/AxonIvyEngine5.1.0.46949_Windows_x86.zip\">the latest engine</a>" // windows
          + "<a href=\"5.1.0/AxonIvyEngine5.1.0.46949_Linux_x86.zip\">the latest engine</a>")) // linux
             .isEqualTo("http://localhost/5.1.0/AxonIvyEngine5.1.0.46949_Linux_x86.zip");
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
  
}
