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
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;

import ch.ivyteam.ivy.maven.engine.EngineVersionEvaluator;
import ch.ivyteam.ivy.maven.engine.download.LatestMinorVersionRange;

/**
 * A MOJO that relies on an unpacked ivy engine.
 *
 * @author Reguel Wermelinger
 * @since 6.0.0
 */
public abstract class AbstractEngineMojo extends AbstractMojo {
  /**
   * keep synch with pom.xml &gt; reporting &gt; maven-plugin-plugin &gt;
   * requirements
   */
  protected static final String MINIMAL_COMPATIBLE_VERSION = "11.3.0";
  protected static final String DEFAULT_VERSION = "12.0.0";

  protected static final String ENGINE_DIRECTORY_PROPERTY = "ivy.engine.directory";

  /**
   * Location where an unpacked (may pre-configured) ivy Engine in the
   * {@link #ivyVersion required version} exists.
   * <p>
   * If parameter is not set it will be a sub-directory of the
   * {@link #engineCacheDirectory}.
   *
   * <p>
   * If the Engine does not yet exist, it can be automatically downloaded.
   */
  @Parameter(property = ENGINE_DIRECTORY_PROPERTY)
  public Path engineDirectory;

  /**
   * Location where ivy engines in required version can be extracted to.
   * <p>
   * If the Engine does not yet exist, it can be automatically downloaded.
   */
  @Parameter(defaultValue = "${settings.localRepository}/.cache/ivy", property = "ivy.engine.cache.directory")
  public Path engineCacheDirectory;

  /**
   * The ivy Engine version or version-range that must be used. Must be equal or
   * higher than {@value #MINIMAL_COMPATIBLE_VERSION} Examples: <br>
   * <ul>
   * <li>"<code>6.1.2</code>" means ivyVersion = 6.1.2</li>
   * <li>"<code>[6.1.0,7.0.0)</code>" means 6.1.0 &lt;= ivyVersion &lt;
   * 7.0.0</li>
   * <li>"<code>(6.0.0,]</code>" means ivyVersion &gt; 6.0.0</li>
   * </ul>
   */
  @Parameter(property = "ivy.engine.version", defaultValue = DEFAULT_VERSION, required = true)
  protected String ivyVersion;

  /** If set to true it will download the latest available minor version */
  @Parameter(property = "ivy.engine.version.latest.minor", defaultValue = "false")
  Boolean useLatestMinor;

  /** testing only: avoid restriction to minimal version! */
  boolean restrictVersionToMinimalCompatible = true;

  public AbstractEngineMojo() {
    super();
  }

  /**
   * <b style="color:red">Caution</b>: normally you should favor
   * {@link #identifyAndGetEngineDirectory()}. Otherwise the returned
   * 'directory' could be yet invalid!
   * @return the raw engine directory
   */
  protected final Path getRawEngineDirectory() {
    return engineDirectory;
  }

  protected final Path identifyAndGetEngineDirectory() throws MojoExecutionException {
    if (!isEngineDirectoryIdentified()) {
      engineDirectory = findMatchingEngineInCacheDirectory();
    }
    return engineDirectory;
  }

  protected final boolean isEngineDirectoryIdentified() {
    return engineDirectory != null;
  }

  protected final Path findMatchingEngineInCacheDirectory() throws MojoExecutionException {
    if (engineCacheDirectory == null || !Files.exists(engineCacheDirectory)) {
      return null;
    }

    File engineDirToTake = null;
    ArtifactVersion versionOfEngineToTake = null;
    for (File engineDirCandidate : engineCacheDirectory.toFile().listFiles()) {
      if (!engineDirCandidate.isDirectory()) {
        continue;
      }

      ArtifactVersion candidateVersion = getInstalledEngineVersion(engineDirCandidate.toPath());
      if (candidateVersion == null || !getIvyVersionRange().containsVersion(candidateVersion)) {
        continue;
      }
      if (versionOfEngineToTake == null || versionOfEngineToTake.compareTo(candidateVersion) < 0) {
        engineDirToTake = engineDirCandidate;
        versionOfEngineToTake = candidateVersion;
      }
    }
    return toPath(engineDirToTake);
  }

  private Path toPath(File file) {
    return file == null ? null : file.toPath();
  }

  protected final ArtifactVersion getInstalledEngineVersion(Path engineDir) throws MojoExecutionException {
    try {
      return new EngineVersionEvaluator(getLog(), engineDir).evaluateVersion();
    } catch (Exception ex) {
      throw new MojoExecutionException("Cannot evaluate engine version", ex);
    }
  }

  protected final VersionRange getIvyVersionRange() throws MojoExecutionException {
    try {
      VersionRange ivyVersionRange = VersionRange.createFromVersionSpec(ivyVersion);
      if (ivyVersionRange.getRecommendedVersion() != null) {
        ivyVersionRange = VersionRange.createFromVersionSpec("[" + ivyVersion + "]");
      }

      if (useLatestMinor) {
        ivyVersionRange = new LatestMinorVersionRange(DEFAULT_VERSION).get();
      }

      if (restrictVersionToMinimalCompatible) {
        return restrictToMinimalCompatible(ivyVersionRange);
      }
      return ivyVersionRange;
    } catch (InvalidVersionSpecificationException ex) {
      throw new MojoExecutionException("Invalid ivyVersion '" + ivyVersion + "'.", ex);
    }
  }

  private VersionRange restrictToMinimalCompatible(VersionRange ivyVersionRange)
          throws InvalidVersionSpecificationException, MojoExecutionException {
    VersionRange minimalCompatibleVersionRange = VersionRange
            .createFromVersionSpec("[" + AbstractEngineMojo.MINIMAL_COMPATIBLE_VERSION + ",)");
    VersionRange restrictedIvyVersionRange = ivyVersionRange.restrict(minimalCompatibleVersionRange);
    if (!restrictedIvyVersionRange.hasRestrictions()) {
      throw new MojoExecutionException(
              "The ivyVersion '" + ivyVersion + "' is lower than the minimal compatible version"
                      + " '" + MINIMAL_COMPATIBLE_VERSION + "'.");
    }
    return restrictedIvyVersionRange;
  }

}
