package ch.ivyteam.ivy.maven.engine.download;

import ch.ivyteam.ivy.maven.engine.EngineVersionEvaluator;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;
import java.util.regex.Pattern;

public class EngineDownloadURLResolver {

    public static URL findEngineDownloadUrl(InputStream htmlStream, String osArchitecture, String ivyVersion, URL engineListPageUrl, VersionRange ivyVersionRange) throws MojoExecutionException, MalformedURLException
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
