package ch.ivyteam.ivy.maven.engine.download;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import ch.ivyteam.ivy.maven.engine.EngineVersionEvaluator;

public class URLEngineDownloader implements EngineDownloader {
  private String zipFileName = null;
  private URL engineDownloadUrl = null;
  private URL engineListPageUrl = null;
  private String osArchitecture = null;
  private String ivyVersion = null;
  private VersionRange ivyVersionRange;
  private Log log;
  private File downloadDirectory;

  public URLEngineDownloader(URL engineDownloadUrl, URL engineListPageUrl, String osArchitecture, String ivyVersion, VersionRange ivyVersionRange, Log log, File downloadDirectory)
  {
    this.engineDownloadUrl = engineDownloadUrl;
    this.engineListPageUrl = engineListPageUrl;
    this.osArchitecture = osArchitecture;
    this.ivyVersion = ivyVersion;
    this.ivyVersionRange = ivyVersionRange;
    this.log = log;
    this.downloadDirectory = downloadDirectory;
  }

  @Override
  public File downloadEngine() throws MojoExecutionException
  {
    URL downloadUrlToUse = (engineDownloadUrl != null) ? engineDownloadUrl : findEngineDownloadUrlFromListPage();
    return downloadEngineFromUrl(downloadUrlToUse);
  }

  private URL findEngineDownloadUrlFromListPage() throws MojoExecutionException
  {
    try (InputStream pageStream = new UrlRedirectionResolver().followRedirections(engineListPageUrl))
    {
      return findEngineDownloadUrl(pageStream);
    }
    catch (IOException ex)
    {
      throw new MojoExecutionException("Failed to find engine download link in list page "+engineListPageUrl, ex);
    }
  }

  private File downloadEngineFromUrl(URL engineUrl) throws MojoExecutionException
  {
    try
    {
      File downloadZip = evaluateTargetFile(engineUrl);
      log.info("Starting engine download from "+engineUrl);
      Files.copy(engineUrl.openStream(), downloadZip.toPath(), StandardCopyOption.REPLACE_EXISTING);
      return downloadZip;
    }
    catch (IOException ex)
    {
      throw new MojoExecutionException("Failed to download engine from '" + engineUrl + "' to '"
              + downloadDirectory + "'", ex);
    }
  }

  private File evaluateTargetFile(URL engineUrl)
  {
    zipFileName = StringUtils.substringAfterLast(engineUrl.toExternalForm(), "/");
    File downloadZip = new File(downloadDirectory, zipFileName);
    int tempFileSuffix = 0;
    while (downloadZip.exists())
    {
      String suffixedZipFileName = zipFileName + "." + tempFileSuffix;
      downloadZip = new File(downloadDirectory, suffixedZipFileName);
      tempFileSuffix++;
    }
    return downloadZip;
  }

  /**
   * Extracts the name of the engine zip-file from the url used to download the engine.
   * The zip-file name is only known <i>after</i> downloading the engine. Since the download-url might
   * be extracted from an engine list-page.
   * The returned zip-file name is not necessarily equal to the name of the downloaded zip-file, since the
   * downloaded file could have been renamed to avoid name conflicts.
   *
   * @return engine zip file-name
   */
  @Override
  public String getZipFileNameFromDownloadLocation()
  {
    if (zipFileName == null)
    {
      throw new IllegalStateException("Engine zip file name is not set up.");
    }
    return zipFileName;
  }
  
  public URL findEngineDownloadUrl(InputStream htmlStream) throws MojoExecutionException, MalformedURLException
  {
      String engineFileNameRegex = "AxonIvyEngine[^.]+?\\.[^.]+?\\.+[^_]*?_"+osArchitecture+"\\.zip";
      Pattern enginePattern = Pattern.compile("href=[\"|'][^\"']*?"+engineFileNameRegex+"[\"|']");
      try(Scanner scanner = new Scanner(htmlStream))
      {
          String engineLink = null;
          while (StringUtils.isBlank(engineLink))
          {
              String engineLinkMatch = scanner.findWithinHorizon(enginePattern, 0);
              if (engineLinkMatch == null)
              {
                  throw new MojoExecutionException("Could not find a link to engine for version '"+ivyVersion+"' on site '"+engineListPageUrl+"'");
              }
              String versionString = StringUtils.substringBetween(engineLinkMatch, "AxonIvyEngine", "_"+osArchitecture);
              ArtifactVersion version = new DefaultArtifactVersion(EngineVersionEvaluator.toReleaseVersion(versionString));
              if (ivyVersionRange.containsVersion(version))
              {
                  engineLink = StringUtils.replace(engineLinkMatch, "\"", "'");
                  engineLink = StringUtils.substringBetween(engineLink, "href='", "'");
              }
          }
          return toAbsoluteLink(engineListPageUrl, engineLink);
      }
  }

  private static URL toAbsoluteLink(URL baseUrl, String parsedEngineArchivLink) throws MalformedURLException
  {
      boolean isAbsoluteLink = StringUtils.startsWithAny(parsedEngineArchivLink, "http://", "https://");
      if (isAbsoluteLink)
      {
          return new URL(parsedEngineArchivLink);
      }
      return new URL(baseUrl, parsedEngineArchivLink);
  }

}
