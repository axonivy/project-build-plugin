package ch.ivyteam.ivy.maven.engine.download;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.wagon.providers.http.HttpWagon;
import org.apache.maven.wagon.proxy.ProxyInfoProvider;
import org.apache.maven.wagon.repository.Repository;

import ch.ivyteam.ivy.maven.engine.EngineVersionEvaluator;

public class URLEngineDownloader implements EngineDownloader {

  private final URL engineDownloadUrl;
  private final URL engineListPageUrl;
  private final String osArchitecture;
  private final String ivyVersion;
  private final VersionRange ivyVersionRange;
  private final Log log;
  private final Path downloadDirectory;
  private String zipFileName = null;
  public ProxyInfoProvider proxies;

  public URLEngineDownloader(URL engineDownloadUrl, URL engineListPageUrl, String osArchitecture,
      String ivyVersion, VersionRange ivyVersionRange, Log log, Path downloadDirectory,
      ProxyInfoProvider proxies) {
    this.engineDownloadUrl = engineDownloadUrl;
    this.engineListPageUrl = engineListPageUrl;
    this.osArchitecture = osArchitecture;
    this.ivyVersion = ivyVersion;
    this.ivyVersionRange = ivyVersionRange;
    this.log = log;
    this.downloadDirectory = downloadDirectory;
    this.proxies = proxies;
  }

  @Override
  public Path downloadEngine() throws MojoExecutionException {
    URL downloadUrlToUse = engineDownloadUrl;
    if (downloadUrlToUse == null) {
      downloadUrlToUse = findEngineDownloadUrlFromListPage();
    }
    return downloadEngineFromUrl(downloadUrlToUse);
  }

  private URL findEngineDownloadUrlFromListPage() throws MojoExecutionException {
    try {
      var repo = new Repository("engine.list.page", engineListPageUrl.toExternalForm());
      Path index = Files.createTempFile("page", ".html");
      wagonDownload(repo, "", index);
      try (InputStream pageStream = Files.newInputStream(index)) {
        return findEngineDownloadUrl(pageStream);
      } finally {
        Files.deleteIfExists(index);
      }
    } catch (Exception ex) {
      throw new MojoExecutionException("Failed to find engine download link in list page " + engineListPageUrl, ex);
    }
  }

  private Path downloadEngineFromUrl(URL engineUrl) throws MojoExecutionException {
    var downloadZip = evaluateTargetFile(engineUrl);
    try {
      log.info("Starting engine download from " + engineUrl);
      var repo = new Repository("engine.repo", StringUtils.substringBeforeLast(engineUrl.toExternalForm(), "/"));
      var resource = StringUtils.substringAfterLast(engineUrl.getPath(), "/");
      wagonDownload(repo, resource, downloadZip);
      return downloadZip;
    } catch (Exception ex) {
      throw new MojoExecutionException("Failed to download engine from '" + engineUrl + "' to '"
          + downloadDirectory + "'", ex);
    }
  }

  private Path evaluateTargetFile(URL engineUrl) {
    zipFileName = StringUtils.substringAfterLast(engineUrl.getPath(), "/");
    var downloadZip = downloadDirectory.resolve(zipFileName);
    int tempFileSuffix = 0;
    while (Files.exists(downloadZip)) {
      String suffixedZipFileName = zipFileName + "." + tempFileSuffix;
      downloadZip = downloadDirectory.resolve(suffixedZipFileName);
      tempFileSuffix++;
    }
    return downloadZip;
  }

  private void wagonDownload(Repository repo, String resource, Path target) throws MojoExecutionException {
    HttpWagon wagon = new HttpWagon();
    try {
      wagon.connect(repo, null, proxies);
      wagon.get(resource, target.toFile());
    } catch (Exception ex) {
      throw new MojoExecutionException("Download failed from repo " + repo + " with resource " + resource, ex);
    } finally {
      wagon.closeConnection();
    }
  }

  /**
   * Extracts the name of the engine zip-file from the url used to download the
   * engine. The zip-file name is only known <i>after</i> downloading the
   * engine. Since the download-url might be extracted from an engine list-page.
   * The returned zip-file name is not necessarily equal to the name of the
   * downloaded zip-file, since the downloaded file could have been renamed to
   * avoid name conflicts.
   *
   * @return engine zip file-name
   */
  @Override
  public String getZipFileNameFromDownloadLocation() {
    if (zipFileName == null) {
      throw new IllegalStateException("Engine zip file name is not set up.");
    }
    return zipFileName;
  }

  public URL findEngineDownloadUrl(InputStream htmlStream)
      throws MojoExecutionException, MalformedURLException, URISyntaxException {
    String engineFileNameRegex = "AxonIvyEngine[^.]+?\\.[^.]+?\\.+[^_]*?_" + osArchitecture + "\\.zip";
    Pattern enginePattern = Pattern.compile("href=[\"|'][^\"']*?" + engineFileNameRegex + "[\"|']");
    try (Scanner scanner = new Scanner(htmlStream)) {
      String engineLink = null;
      while (StringUtils.isBlank(engineLink)) {
        String engineLinkMatch = scanner.findWithinHorizon(enginePattern, 0);
        if (engineLinkMatch == null) {
          throw new MojoExecutionException("Could not find a link to engine for version '" + ivyVersion
              + "' on site '" + engineListPageUrl + "'");
        }
        String versionString = StringUtils.substringBetween(engineLinkMatch, "AxonIvyEngine",
            "_" + osArchitecture);
        ArtifactVersion version = new DefaultArtifactVersion(
            EngineVersionEvaluator.toReleaseVersion(versionString));
        if (ivyVersionRange.containsVersion(version)) {
          engineLink = Strings.CS.replace(engineLinkMatch, "\"", "'");
          engineLink = StringUtils.substringBetween(engineLink, "href='", "'");
        }
      }
      return toAbsoluteLink(engineListPageUrl, engineLink);
    }
  }

  private static URL toAbsoluteLink(URL baseUrl, String parsedEngineArchivLink) throws MalformedURLException, URISyntaxException {
    boolean isAbsoluteLink = Strings.CS.startsWithAny(parsedEngineArchivLink, "http://", "https://");
    if (isAbsoluteLink) {
      return URI.create(parsedEngineArchivLink).toURL();
    }
    return baseUrl.toURI().resolve(parsedEngineArchivLink).toURL();
  }
}
