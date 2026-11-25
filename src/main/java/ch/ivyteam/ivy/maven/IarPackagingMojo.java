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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.maven.model.FileSet;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.util.DefaultFileSet;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.codehaus.plexus.util.AbstractScanner;

import ch.ivyteam.ivy.maven.util.FileSetConverter;
import ch.ivyteam.ivy.project.model.ProjectVersion;

/**
 * Packs the compiled project as ivy-archive (IAR).
 *
 * @author Reguel Wermelinger
 * @since 6.0.0
 */
@Mojo(name = IarPackagingMojo.GOAL, threadSafe = true)
public class IarPackagingMojo extends AbstractMojo {
  public static final String GOAL = "pack-iar";
  private static final String IVY_PROJECT = ".ivyproject";

  public interface Defaults {
    String[] INCLUDES = {"**/*"};
    String[] EXCLUDES = {"target/"};
    String[] TARGET_INCLUDES = {
        "target/dialog/**/*",
        "target/lib/mvn-deps/*"
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
   * From the 'target' directory only 'classes', 'dialog' and 'lib/mvn-deps' are included by default.
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
   * Directory containing the generated IAR.
   */
  @Parameter(defaultValue = "${project.build.directory}", property = "ivy.output.directory")
  Path outputDirectory;

  /**
   * Name of the generated IAR.
   */
  @Parameter(defaultValue = "${project.build.finalName}", property = "ivy.final.name")
  String finalName;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    var iar = getIarFile();
    createIvyArchive(project.getBasedir(), iar);

    var artifact = project.getArtifact();
    artifact.setFile(iar.toFile());
    project.setArtifact(artifact);
    getLog().info("Attached " + artifact + ".");
  }

  private Path getIarFile() {
    var basedir = getTargetDir();
    var resultFinalName = finalName != null ? finalName : project.getBuild().getFinalName();
    Objects.requireNonNull(basedir, "basedir is not allowed to be null");
    Objects.requireNonNull(resultFinalName, "finalName is not allowed to be null");
    var fileName = resultFinalName + ".iar";
    return basedir.resolve(fileName);
  }

  private Path getTargetDir() {
    return outputDirectory != null ? outputDirectory : Path.of(project.getBuild().getDirectory());
  }

  private void createIvyArchive(File projectDir, Path targetIar) throws MojoExecutionException {
    var archiver = new ZipArchiver();
    archiver.setDestFile(targetIar.toFile());
    try {
      var fsConverter = new FileSetConverter(project.getBasedir().toPath());
      for (var fs : fsConverter.toPlexusFileSets(iarFileSets)) {
        fs.setPrefix(Defaults.PREFIX);
        archiver.addFileSet(fs);
      }
      archiver.addFileSet(createFs(getIvyProject(projectDir).getParent().toFile())
          .prefixed(Defaults.PREFIX)
          .include(new String[] {IVY_PROJECT})
          .exclude(iarExcludes));
      archiver.addFileSet(getIarFs_exceptTarget(projectDir));
      archiver.addFileSet(getIarTargetFs(projectDir));
      archiveClasses(projectDir.toPath(), archiver);
      archiver.createArchive();
    } catch (ArchiverException | IOException ex) {
      throw new MojoExecutionException("Failed to create IAR: " + targetIar.toAbsolutePath(), ex);
    }
  }

  private Path getIvyProject(File projectDir) throws MojoExecutionException, IOException {
    var ivyProject = projectDir.toPath().resolve(IVY_PROJECT);
    String meta = Files.readString(ivyProject, StandardCharsets.UTF_8);
    
    var version = ProjectVersion.CURRENT;
    var targetMeta = meta.replaceAll("(?m)^version=.*$", "version=" + version);
    
    var targetProject = getTargetDir().resolve(IVY_PROJECT);
    Files.writeString(targetProject, targetMeta, StandardCharsets.UTF_8);
    return targetProject;
  }

  private void archiveClasses(Path projectDir, ZipArchiver archiver) {
    var outputDir = projectDir.resolve(Path.of(project.getBuild().getOutputDirectory()));
    if (!Files.exists(outputDir)) {
      outputDir = projectDir.resolve("classes");
      if (!Files.exists(outputDir)) {
        return;
      }
    }
    archiver.addFileSet(createFs(outputDir.toFile()));
  }

  private DefaultFileSet getIarFs_exceptTarget(File projectDir) {
    return createFs(projectDir)
        .prefixed(Defaults.PREFIX)
        .include(Defaults.INCLUDES)
        .exclude(ArrayUtils.add(ArrayUtils.addAll(Defaults.EXCLUDES, iarExcludes), IVY_PROJECT));
  }

  private DefaultFileSet getIarTargetFs(File projectDir) {
    return createFs(projectDir)
        .prefixed(Defaults.PREFIX)
        .include(Defaults.TARGET_INCLUDES)
        .exclude(iarExcludes);
  }

  private DefaultFileSet createFs(File sourceDir) {
    return DefaultFileSet.fileSet(sourceDir)
        .includeEmptyDirs(false);
  }

}
