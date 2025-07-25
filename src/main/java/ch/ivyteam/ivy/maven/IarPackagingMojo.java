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
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.FileSet;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.util.DefaultFileSet;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.codehaus.plexus.util.AbstractScanner;

import ch.ivyteam.ivy.maven.util.FileSetConverter;

/**
 * Packs the compiled project as ivy-archive (IAR).
 *
 * @author Reguel Wermelinger
 * @since 6.0.0
 */
@Mojo(name = IarPackagingMojo.GOAL)
public class IarPackagingMojo extends AbstractMojo {
  public static final String GOAL = "pack-iar";

  public interface Defaults {
    String[] INCLUDES = {"**/*"};
    String[] EXCLUDES = {"target/"};
    String[] TARGET_INCLUDES = {
        "target/classes/**/*",
        "target/src_hd/**/*"
    };
    String PREFIX = "META-INF/ivy/";
  }

  @Parameter(property = "project", required = true, readonly = true)
  MavenProject project;

  /**
   * Define additional IAR excludes with ANT-style exclusion declarations.
   *
   * For Maven default excludes. See {@link AbstractScanner#DEFAULTEXCLUDES}.
   *
   * Sample:
   * <pre>
   * <code>&lt;iarExcludes&gt;
   * &nbsp;&nbsp;&lt;iarExclude&gt;target/com/acme/scret/*&lt;/iarExclude&gt;
   * &nbsp;&nbsp;&lt;iarExclude&gt;src/&lt;/iarExclude&gt;
   * &lt;/iarExcludes&gt;</code>
   * </pre>
   */
  @Parameter
  String[] iarExcludes;

  /**
   * Define additional IAR {@link FileSet fileSets} with ANT-style exclusion
   * declarations.
   *
   * From the 'target' directory only 'classes' and 'src_hd' are included by default.
   * See {@link ch.ivyteam.ivy.maven.IarPackagingMojo.Defaults#TARGET_INCLUDES}.
   *
   * <pre>
   * &lt;iarFileSets&gt;
   * &nbsp;&nbsp;&lt;iarFileSet&gt;
   * &nbsp;&nbsp;&nbsp;&nbsp;&lt;includes&gt;
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;include&gt;**&#47;*&lt;/include&gt;
   * &nbsp;&nbsp;&nbsp;&nbsp;&lt;/includes&gt;
   * &nbsp;&nbsp;&lt;/iarFileSet&gt;
   * &lt;/iarFileSets&gt;
   * </pre>
   */
  @Parameter
  FileSet[] iarFileSets;

  /**
   * Includes empty directories in the packed IAR. If set to <code>false</code>,
   * the IAR can not be re-imported as Designer project as standard project
   * artifacts (e.g. source folders) could be missing.
   */
  @Parameter(defaultValue = "true")
  boolean iarIncludesEmptyDirs;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    String iarName = project.getArtifactId() + "-" + project.getVersion() + ".iar";
    var iar = Path.of(project.getBuild().getDirectory()).resolve(iarName);
    createIvyArchive(project.getBasedir(), iar);

    Artifact artifact = project.getArtifact();
    artifact.setFile(iar.toFile());
    project.setArtifact(artifact);
    getLog().info("Attached " + artifact + ".");
  }

  private void createIvyArchive(File projectDir, Path targetIar) throws MojoExecutionException {
    ZipArchiver archiver = new ZipArchiver();
    archiver.setDuplicateBehavior(Archiver.DUPLICATES_SKIP);
    archiver.setDestFile(targetIar.toFile());
    FileSetConverter fsConverter = new FileSetConverter(project.getBasedir().toPath());
    for (var fs : fsConverter.toPlexusFileSets(iarFileSets)) {
      fs.setPrefix(Defaults.PREFIX);
      archiver.addFileSet(fs);
    }
    archiver.addFileSet(getIarFs_exceptTarget(projectDir));
    archiver.addFileSet(getIarTargetFs(projectDir));
    var targetClasses = projectDir.toPath().resolve(Path.of(project.getBuild().getOutputDirectory()));
    if (Files.exists(targetClasses)) {
      archiver.addFileSet(createFs(targetClasses.toFile()));
    }
    try {
      archiver.createArchive();
    } catch (ArchiverException | IOException ex) {
      throw new MojoExecutionException("Failed to create IAR: " + targetIar.toAbsolutePath(), ex);
    }
  }

  private DefaultFileSet getIarFs_exceptTarget(File projectDir) {
    var fileSet = createFs(projectDir);
    fileSet.setPrefix(Defaults.PREFIX);
    fileSet.setIncludes(Defaults.INCLUDES);
    fileSet.setExcludes(ArrayUtils.addAll(Defaults.EXCLUDES, iarExcludes));
    return fileSet;
  }

  private DefaultFileSet getIarTargetFs(File projectDir) {
    var fileSet = createFs(projectDir);
    fileSet.setPrefix(Defaults.PREFIX);
    fileSet.setIncludes(Defaults.TARGET_INCLUDES);
    fileSet.setExcludes(iarExcludes);
    return fileSet;
  }

  private DefaultFileSet createFs(File sourceDir) {
    var fileSet = new DefaultFileSet();
    fileSet.setDirectory(sourceDir);
    fileSet.setIncludingEmptyDirectories(iarIncludesEmptyDirs);
    return fileSet;
  }

}
