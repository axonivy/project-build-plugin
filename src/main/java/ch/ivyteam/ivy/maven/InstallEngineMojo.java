/*
 * Copyright (C) 2018 AXON Ivy AG
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


import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

import ch.ivyteam.ivy.maven.engine.EngineVersionEvaluator;
import ch.ivyteam.ivy.maven.engine.download.EngineDownloader;
import ch.ivyteam.ivy.maven.engine.download.MavenEngineDownloader;
import ch.ivyteam.ivy.maven.engine.download.URLEngineDownloader;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

/**
 * Downloads an AXON.IVY Engine from the NET if it does not yet exists in the correct version.
 * 
 * <p>Command line invocation is supported. E.g.</p>
 * <pre>mvn com.axonivy.ivy.ci:project-build-plugin:6.2.0:installEngine
 * -Divy.engine.directory=c:/axonviy/engine
 * -Divy.engine.version=6.2.0
 * -Divy.engine.os.arch=Linux_x64</pre>
 * 
 * @author Reguel Wermelinger
 * @since 6.0.0
 */
@Mojo(name=InstallEngineMojo.GOAL, requiresProject=false)
public class InstallEngineMojo extends AbstractEngineMojo
{
  public static final String GOAL = "installEngine";
  public static final String ENGINE_LIST_URL_PROPERTY = "ivy.engine.list.url";
  public static final String DEFAULT_ARCH = "Slim_All_x64";

  /**
   * Indicate if the engine artifact should be downloaded using maven (from a configured maven repository) or if
   * the URL download way should be used.
   * 
   * <p>As there exist no official maven repository containing the axonivy engine, 
   * it must be published manually to an accessible plugin repository. The expected artifact descriptor is:</p> 
   * <pre>
   *    groupId=com.axonivy.ivy
   *    artifactId=engine
   *    version=!ivyVersion! (e.g. 7.0.0)
   *    classifier=!osArchitecture! (e.g. All_x64)
   *    extension=zip
   * </pre>
   * @since 7.4
   */
  @Parameter(property="ivy.engine.download.from.maven", defaultValue = "false")
  Boolean downloadUsingMaven;

  @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
  RepositorySystemSession repositorySession;

  @Parameter(defaultValue = "${project.remotePluginRepositories}", readonly = true)
  List<RemoteRepository> pluginRepositories;

  @Component
  private RepositorySystem repositorySystem;

  /**
   * URL where a packed ivy Engine can be downloaded. E.g.
   * <code>https://developer.axonivy.com/download/6.0.10/AxonIvyEngine6.0.10.55478_Windows_x64.zip</code>
   */
  @Parameter(property="ivy.engine.download.url")
  URL engineDownloadUrl;
  
  /** 
   * URL where a link to the ivy Engine in the expected {@link #ivyVersion} exists. 
   * The URL will be used to download the required engine if it does not yet exist.
   * The URL should point to a site providing HTML content with a link to the engine <br>e.g.
   * <code>&lt;a href="https://developer.axonivy.com/download/6.0.10/AxonIvyEngine6.0.10.55478_Windows_x64.zip"&gt; the engine&lt;/a&gt;</code>
   */
  @Parameter(property=ENGINE_LIST_URL_PROPERTY, defaultValue="https://developer.axonivy.com/download/maven.html")
  URL engineListPageUrl;
  
  /** 
   * Engine type that will be downloaded if {@link #autoInstallEngine} is set and the engine must be 
   * retrieved from the {@link #engineListPageUrl}.
   * Possible values are:
   * <ul>
   *    <li>All_x64</li>
   *    <li>Slim_All_x64</li>
   *    <li>Windows_x64</li>
   * </ul>
   * All_x64 supports Linux and Windows.
   * Slim_All_x64 supports Linux and Windows only with the necessary features (e.g. without demo applications).
   */
  @Parameter(property="ivy.engine.os.arch", defaultValue=DEFAULT_ARCH)
  String osArchitecture;
  
  /** 
   * Enables the automatic installation of an ivy Engine in the {@link #engineDirectory}.
   * If there is yet no engine installed, or the {@link #ivyVersion} does not match, the
   * engine will be downloaded from the {@link #engineDownloadUrl} and unpacked into the
   * {@link #engineDirectory}.
   */
  @Parameter(property="ivy.engine.auto.install", defaultValue="true") 
  boolean autoInstallEngine;

  @Override
  public void execute() throws MojoExecutionException
  {
    getLog().info("Provide engine for ivy version " + ivyVersion);
    ensureEngineIsInstalled();
  }

  private void ensureEngineIsInstalled() throws MojoExecutionException
  {
    VersionRange ivyVersionRange = getIvyVersionRange();
    if (identifyAndGetEngineDirectory() == null)
    {
      handleNoInstalledEngine();
    }
    else
    {
      if (engineDirectoryIsEmpty())
      {
        getRawEngineDirectory().mkdirs();
      }
      ArtifactVersion installedEngineVersion = getInstalledEngineVersion(getRawEngineDirectory());
      
      if (installedEngineVersion == null || 
              !ivyVersionRange.containsVersion(installedEngineVersion))
      {
        handleWrongIvyVersion(installedEngineVersion);
      }
    }
  }

  private void handleNoInstalledEngine() throws MojoExecutionException
  {
    getLog().info("No installed engine found for version '"+ivyVersion+"'");
    boolean cleanEngineDir = false;
    downloadAndInstallEngine(cleanEngineDir);
  }
  
  private void handleWrongIvyVersion(ArtifactVersion installedEngineVersion) throws MojoExecutionException
  {
    getLog().info("Installed engine has version '"+installedEngineVersion+"' instead of expected '"+ivyVersion+"'");
    boolean cleanEngineDir = installedEngineVersion != null;
    downloadAndInstallEngine(cleanEngineDir);
  }

  private void downloadAndInstallEngine(boolean cleanEngineDir) throws MojoExecutionException
  {
    if (autoInstallEngine)
    {
      getLog().info("Will automatically download Engine now.");
      final EngineDownloader engineDownloader = getDownloader();
      File downloadZip = engineDownloader.downloadEngine();

      if (cleanEngineDir)
      {
        removeOldEngineContent();
      }

      if (!isEngineDirectoryIdentified())
      {
        String engineZipFileName = engineDownloader.getZipFileNameFromDownloadLocation();
        engineDirectory = new File(engineCacheDirectory, ivyEngineVersionOfZip(engineZipFileName));
        engineDirectory.mkdirs();
      }

      unpackEngine(downloadZip);

      if(!downloadUsingMaven)
      {
        downloadZip.delete();
      }
      
      ArtifactVersion installedEngineVersion = getInstalledEngineVersion(getRawEngineDirectory());
      if (installedEngineVersion == null)
      {
        throw new MojoExecutionException("Can not determine installed engine version in directory '"+getRawEngineDirectory()+"'. "
                + "Possibly a non-OSGi engine.");
      }
      if (!getIvyVersionRange().containsVersion(installedEngineVersion))
      {
        throw new MojoExecutionException("Automatic installation of an ivyEngine failed. "
                + "Downloaded version is '"+installedEngineVersion+"' but expecting '"+ivyVersion+"'.");
      }
    }
    else
    {
      throw new MojoExecutionException("Aborting class generation as no valid ivy Engine is available! "
              + "Use the 'autoInstallEngine' parameter for an automatic installation.");
    }
  }

  public EngineDownloader getDownloader() throws MojoExecutionException
  {
    if(downloadUsingMaven)
    {
      return new MavenEngineDownloader(getLog(), ivyVersion, osArchitecture, pluginRepositories, repositorySystem, repositorySession);
    }
    else
    {
      return new URLEngineDownloader(engineDownloadUrl, engineListPageUrl, osArchitecture, ivyVersion, getIvyVersionRange(), getLog(), getDownloadDirectory());
    }
  }

  static String ivyEngineVersionOfZip(String engineZipFileName)
  {
    Matcher matcher = Pattern.compile("[a-zA-Z]*(([\\d]+\\.?)+)*").matcher(engineZipFileName);
    if (matcher.find())
    {
      String version = matcher.group(1);
      if (version != null)
      {
        return EngineVersionEvaluator.toReleaseVersion(matcher.group(1));
      }
    }
    return engineZipFileName; // fallback: no version in file name
  }

  private void removeOldEngineContent() throws MojoExecutionException
  {
    try
    {
      FileUtils.cleanDirectory(getRawEngineDirectory());
    }
    catch (IOException ex)
    {
      throw new MojoExecutionException("Failed to clean outdated ivy Engine directory '"+getRawEngineDirectory()+"'.", ex);
    }
  }

  private boolean engineDirectoryIsEmpty()
  {
    return !getRawEngineDirectory().isDirectory() || ArrayUtils.isEmpty(getRawEngineDirectory().listFiles());
  }

  private void unpackEngine(File downloadZip) throws MojoExecutionException
  {
    try
    {
      String targetLocation = getRawEngineDirectory().getAbsolutePath();
      getLog().info("Unpacking engine " + downloadZip.getAbsolutePath() + " to " + targetLocation);
      ZipFile engineZip = new ZipFile(downloadZip);
      engineZip.extractAll(targetLocation);
    }
    catch (ZipException ex)
    {
      throw new MojoExecutionException("Failed to unpack downloaded engine '" + downloadZip + "'.", ex);
    }
  }
  
  File getDownloadDirectory()
  {
    return SystemUtils.getJavaIoTmpDir(); 
  }

}
