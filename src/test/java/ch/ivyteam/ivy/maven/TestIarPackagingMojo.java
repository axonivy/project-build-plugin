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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.maven.model.FileSet;
import org.codehaus.plexus.util.MatchPattern;
import org.codehaus.plexus.util.StringUtils;
import org.junit.Rule;
import org.junit.Test;

import ch.ivyteam.ivy.maven.util.PathUtils;

/**
 * @author Reguel Wermelinger
 * @since 03.11.2014
 */
public class TestIarPackagingMojo {

  @Rule
  public ProjectMojoRule<IarPackagingMojo> rule = new ProjectMojoRule<>(
      Path.of("src/test/resources/base"), IarPackagingMojo.GOAL){

    @Override
    protected void before() throws Throwable {
      super.before();
      createEmptySrcDirs();
    }

    private void createEmptySrcDirs() {
      var emptySrcDirNames = List.of("src_dataClasses", "src_hd", "src_rd", "src_ws", "src_wsproc");
      for (var emptySrcDirName : emptySrcDirNames) {
        var srcDir = projectDir.resolve(emptySrcDirName);
        PathUtils.clean(srcDir);
      }
    }
  };

  /**
   * Happy path creation tests
   */
  @Test
  public void archiveCreationDefault() throws Exception {
    IarPackagingMojo mojo = rule.getMojo();
    var dir = mojo.project.getBasedir().toPath().resolve(".svn");
    Files.createDirectories(dir);
    var svn = dir.resolve("svn.txt");
    Files.writeString(svn, "svn");
    mojo.execute();
    var targetDir = mojo.project.getBasedir().toPath().resolve("target");

    var iarFiles = iarFiles(targetDir);
    assertThat(iarFiles).hasSize(1);

    var iarFile = iarFiles.getFirst();
    assertThat(iarFile).hasFileName("base-1.0.0.iar");
    assertThat(mojo.project.getArtifact().getFile())
        .as("Created IAR must be registered as artifact for later repository installation.")
        .isEqualTo(iarFile.toFile());

    try (ZipFile archive = new ZipFile(iarFile.toFile())) {
      assertThat(getProjectZipFileEntry(archive, ".classpath")).as(".classpath must be packed for internal binary retrieval")
          .isNotNull();
      assertThat(getProjectZipFileEntry(archive, "src_hd")).as("Empty directories should be included (by default) "
          + "so that the IAR can be re-imported into the designer").isNotNull();
      assertThat(getProjectZipFileEntry(archive, "target/sampleOutput.txt"))
          .as("'target/sampleOutput.txt' should not be packed").isNull();
      assertThat(getProjectZipFileEntry(archive, "target")).as("'target' must be packed because there are target/classes")
          .isNotNull();
      assertThat(getProjectZipFileEntry(archive, ".svn/svn.txt")).as("'.svn' folder should not be packed").isNull();
      assertThat(getProjectZipFileEntry(archive, ".svn")).as("'target'.svn should not be packed").isNull();
      assertThat(getProjectZipFileEntry(archive, "classes/gugus.txt")).as("classes content should be included by default")
          .isNotNull();
      assertThat(getProjectZipFileEntry(archive, "target/classes/gugus.txt"))
          .as("target/classes content should be included by default").isNotNull();
      assertThat(getProjectZipFileEntry(archive, "target/classesAnother/gugus.txt"))
          .as("target/classesAnother should not be packed by default").isNull();
      assertThat(getProjectZipFileEntry(archive, "target/anythingelse/gugus.txt"))
          .as("target/anythingelse should not be packed by default").isNull();
    }
  }

  private List<Path> iarFiles(Path dir) throws IOException {
    try (var dirs = Files.list(dir)) {
      return dirs.filter(p -> p.getFileName().toString().endsWith(".iar")).toList();
    }
  }

  @Test
  public void canDefineCustomExclusions() throws Exception {
    IarPackagingMojo mojo = rule.getMojo();
    String filterCandidate = "private/notPublic.txt";
    assertThat(mojo.project.getBasedir().toPath().resolve(filterCandidate)).exists();

    mojo.iarExcludes = new String[] {"private", "private/**/*"};
    mojo.execute();
    try (ZipFile archive = new ZipFile(mojo.project.getArtifact().getFile())) {
      assertThat(getProjectZipFileEntry(archive, "private")).as("Custom exclusion must be filtered").isNull();
      assertThat(getProjectZipFileEntry(archive, filterCandidate)).as("Custom exclusion must be filtered").isNull();
      assertThat(archive.size()).isGreaterThan(50).as("archive must contain content");
    }
  }

  @Test
  public void canDefineCustomInclusions() throws Exception {
    IarPackagingMojo mojo = rule.getMojo();
    var outputDir = mojo.project.getBasedir().toPath().resolve("target");
    var customPomXml = outputDir.resolve("myCustomPom.xml");
    Files.writeString(customPomXml, "customPomContent");

    String relativeCustomIncludePath = "target/" + customPomXml.getFileName().toString();
    FileSet fs = new FileSet();
    fs.setIncludes(List.of(relativeCustomIncludePath));
    mojo.iarFileSets = new FileSet[] {fs};

    mojo.execute();
    try (ZipFile archive = new ZipFile(mojo.project.getArtifact().getFile())) {
      assertThat(archive.getEntry(IarPackagingMojo.Defaults.PREFIX + relativeCustomIncludePath))
          .as("Custom inclusions must be included")
          .isNotNull();
    }
  }

  @Test
  public void canOverwriteDefaultInclusions() throws Exception {
    IarPackagingMojo mojo = rule.getMojo();
    var outputDir = mojo.project.getBasedir().toPath().resolve("target");
    var flatPomXML = outputDir.resolve("pom.xml");
    Files.writeString(flatPomXML, "<artifactId>flattened</artifactId>");

    FileSet fs = new FileSet();
    fs.setDirectory("target");
    fs.setIncludes(List.of(flatPomXML.getFileName().toString()));
    mojo.iarFileSets = new FileSet[] {fs};

    mojo.execute();
    try (ZipFile archive = new ZipFile(mojo.project.getArtifact().getFile())) {
      try (InputStream is = archive.getInputStream(getProjectZipFileEntry(archive, "pom.xml"))) {
        String pomInArchive = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        assertThat(pomInArchive)
            .as("customer should be able to overwrite pre-defined resource with their own includes.")
            .contains("flattened");
      }
      var pomEntries = Collections.list(archive.entries()).stream()
          .filter(entry -> entry.getName().endsWith("pom.xml"))
          .toList();
      assertThat(pomEntries)
          .as("if same path is specified twice, the users entry should win and no duplicates must exist.")
          .hasSize(1);
    }
  }

  @Test
  public void canExcludeEmptyDirectories() throws Exception {
    IarPackagingMojo mojo = rule.getMojo();
    mojo.iarIncludesEmptyDirs = false;
    mojo.execute();

    try (ZipFile archive = new ZipFile(mojo.project.getArtifact().getFile())) {
      assertThat(getProjectZipFileEntry(archive, "src_hd")).as("Empty directory should be excluded by mojo configuration")
          .isNull();
    }
  }

  @Test
  public void doNotPackTargetFolderIfThereAreNoTargetClasses() throws Exception {
    IarPackagingMojo mojo = rule.getMojo();
    var targetClasses = mojo.project.getBasedir().toPath().resolve("target/classes");
    PathUtils.delete(targetClasses);
    mojo.execute();

    var dir = mojo.project.getBasedir().toPath().resolve("target");
    var iarFiles = iarFiles(dir);
    assertThat(iarFiles).hasSize(1);

    var iarFile = iarFiles.getFirst();
    try (ZipFile archive = new ZipFile(iarFile.toFile())) {
      assertThat(getProjectZipFileEntry(archive, "target"))
          .as("'target' will not be packed when there are no target/classes").isNull();
    }
  }

  @Test
  public void includeTarget_srcHdGenerated() throws Exception {
    IarPackagingMojo mojo = rule.getMojo();

    var target = mojo.project.getBasedir().toPath().resolve("target");
    var viewGenerated = target.resolve("src_hd")
        .resolve("com").resolve("acme").resolve("FormDialog").resolve("FormDialog.xhtml");
    Files.createDirectories(viewGenerated.getParent());
    Files.writeString(viewGenerated, "<html/>", StandardOpenOption.CREATE_NEW);

    mojo.execute();

    var dir = mojo.project.getBasedir().toPath().resolve("target");
    var iarFiles = iarFiles(dir);
    assertThat(iarFiles).hasSize(1);

    var iarFile = iarFiles.getFirst();
    try (ZipFile archive = new ZipFile(iarFile.toFile())) {
      assertThat(getProjectZipFileEntry(archive, "target/src_hd/com/acme/FormDialog/FormDialog.xhtml"))
          .as("generated jsf.dialog views are included").isNotNull();
    }
  }

  @Test
  public void validDefaultExcludePatternsForWindows() {
    for (var defaultExclude : IarPackagingMojo.Defaults.EXCLUDES) {
      defaultExclude = StringUtils.replace(defaultExclude, "/", "\\\\"); // see
                                                                         // org.codehaus.plexus.util.AbstractScanner.normalizePattern(String)
      var matchPattern = MatchPattern.fromString(defaultExclude);
      assertThat(matchPattern.matchPath("never-matching-path", false)).isFalse();
    }
  }

  @Test
  public void validDefaultExcludePatternsForLinux() {
    for (var defaultExclude : IarPackagingMojo.Defaults.EXCLUDES) {
      defaultExclude = StringUtils.replace(defaultExclude, "\\\\", "/"); // see
                                                                         // org.codehaus.plexus.util.AbstractScanner.normalizePattern(String)
      var matchPattern = MatchPattern.fromString(defaultExclude);
      assertThat(matchPattern.matchPath("never-matching-path", false)).isFalse();
    }
  }

  @Test
  public void rootClassFiles() throws Exception {
    IarPackagingMojo mojo = rule.getMojo();
    var classFilePath = Path.of("target", "classes", "ch", "ivyteam");
    var dir = mojo.project.getBasedir().toPath().resolve(classFilePath);
    Files.createDirectories(dir);
    var classFile = dir.resolve("MyTest.class");
    Files.writeString(classFile, "hello class file");
    mojo.execute();

    try (ZipFile archive = new ZipFile(mojo.project.getArtifact().getFile())) {
      assertThat(archive.getInputStream(archive.getEntry("ch/ivyteam/MyTest.class")))
          .hasContent("hello class file");
      assertThat(archive.getInputStream(archive.getEntry("gugus.txt")))
          .hasContent("gugus");
    }
  }

  @Test
  public void rootClassFiles_ifNoTargetExists() throws Exception {
    IarPackagingMojo mojo = rule.getMojo();
    var target = mojo.project.getBasedir().toPath().resolve("target");
    PathUtils.delete(target);
    mojo.execute();

    try (ZipFile archive = new ZipFile(mojo.project.getArtifact().getFile())) {
      assertThat(archive.getEntry("ch/ivyteam/MyTest.class")).isNull();
      assertThat(archive.getEntry("gugus.txt")).isNull();
    }
  }

  @Test
  public void testLargeIar() throws Exception {
    var mojo = rule.getMojo();
    var target = mojo.project.getBasedir().toPath().resolve("target").resolve("classes");
    var rand = new Random();
    for (var i = 0; i < 20_000; i++) {
      var file = target.resolve("large-iar-file" + i);
      var data = new byte[512];
      rand.nextBytes(data);
      Files.write(file, data);
    }
    mojo.execute();

    try (var archive = new ZipFile(mojo.project.getArtifact().getFile())) {
      assertThat(archive.getEntry("large-iar-file19999").getSize()).isEqualTo(512);
    }
  }

  static ZipEntry getProjectZipFileEntry(ZipFile archive, String fileName) {
    return archive.getEntry(IarPackagingMojo.Defaults.PREFIX + fileName);
  }
}
