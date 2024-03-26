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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.maven.model.FileSet;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.MatchPattern;
import org.codehaus.plexus.util.StringUtils;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Reguel Wermelinger
 * @since 03.11.2014
 */
public class TestIarPackagingMojo {

  @Rule
  public ProjectMojoRule<IarPackagingMojo> rule = new ProjectMojoRule<IarPackagingMojo>(
          new File("src/test/resources/base"), IarPackagingMojo.GOAL) {
    @Override
    protected void before() throws Throwable {
      super.before();
      createEmptySrcDirs();
    }

    private void createEmptySrcDirs() throws IOException {
      List<String> emptySrcDirNames = Arrays.asList("src_dataClasses", "src_hd", "src_rd", "src_ws",
              "src_wsproc");
      for (String emptySrcDirName : emptySrcDirNames) {
        File srcDir = new File(projectDir, emptySrcDirName);
        FileUtils.deleteDirectory(srcDir);
        srcDir.mkdir();
      }
    }
  };

  /**
   * Happy path creation tests
   * @throws Exception
   */
  @Test
  public void archiveCreationDefault() throws Exception {
    IarPackagingMojo mojo = rule.getMojo();
    File svn = new File(mojo.project.getBasedir(), ".svn/svn.txt");
    FileUtils.write(svn, "svn", StandardCharsets.UTF_8);
    mojo.execute();
    Collection<File> iarFiles = FileUtils.listFiles(new File(mojo.project.getBasedir(), "target"),
            new String[] {"iar"}, false);
    assertThat(iarFiles).hasSize(1);

    File iarFile = iarFiles.iterator().next();
    assertThat(iarFile.getName()).isEqualTo("base-1.0.0.iar");
    assertThat(mojo.project.getArtifact().getFile())
            .as("Created IAR must be registered as artifact for later repository installation.")
            .isEqualTo(iarFile);

    try (ZipFile archive = new ZipFile(iarFile)) {
      assertThat(archive.getEntry(".classpath")).as(".classpath must be packed for internal binary retrieval")
              .isNotNull();
      assertThat(archive.getEntry("src_hd")).as("Empty directories should be included (by default) "
              + "so that the IAR can be re-imported into the designer").isNotNull();
      assertThat(archive.getEntry("target/sampleOutput.txt"))
              .as("'target/sampleOutput.txt' should not be packed").isNull();
      assertThat(archive.getEntry("target")).as("'target' must be packed because there are target/classes")
              .isNotNull();
      assertThat(archive.getEntry(".svn/svn.txt")).as("'.svn' folder should not be packed").isNull();
      assertThat(archive.getEntry(".svn")).as("'target'.svn should not be packed").isNull();
      assertThat(archive.getEntry("classes/gugus.txt")).as("classes content should be included by default")
              .isNotNull();
      assertThat(archive.getEntry("target/classes/gugus.txt"))
              .as("target/classes content should be included by default").isNotNull();
      assertThat(archive.getEntry("target/classesAnother/gugus.txt"))
              .as("target/classesAnother should not be packed by default").isNull();
      assertThat(archive.getEntry("target/anythingelse/gugus.txt"))
              .as("target/anythingelse should not be packed by default").isNull();
    }
  }

  @Test
  public void canDefineCustomExclusions() throws Exception {
    IarPackagingMojo mojo = rule.getMojo();
    String filterCandidate = "private/notPublic.txt";
    assertThat(new File(mojo.project.getBasedir(), filterCandidate)).exists();

    mojo.iarExcludes = new String[] {"private", "private/**/*"};
    mojo.execute();
    try (ZipFile archive = new ZipFile(mojo.project.getArtifact().getFile())) {
      assertThat(archive.getEntry("private")).as("Custom exclusion must be filtered").isNull();
      assertThat(archive.getEntry(filterCandidate)).as("Custom exclusion must be filtered").isNull();
      assertThat(archive.size()).isGreaterThan(50).as("archive must contain content");
    }
  }

  @Test
  public void canDefineCustomInclusions() throws Exception {
    IarPackagingMojo mojo = rule.getMojo();
    File outputDir = new File(mojo.project.getBasedir(), "target");
    File customPomXml = new File(outputDir, "myCustomPom.xml");
    FileUtils.write(customPomXml, "customPomContent", StandardCharsets.UTF_8);

    String relativeCustomIncludePath = "target/" + customPomXml.getName();
    FileSet fs = new FileSet();
    fs.setIncludes(Arrays.asList(relativeCustomIncludePath));
    mojo.iarFileSets = new FileSet[] {fs};

    mojo.execute();
    try (ZipFile archive = new ZipFile(mojo.project.getArtifact().getFile())) {
      assertThat(archive.getEntry(relativeCustomIncludePath)).as("Custom inclusions must be included")
              .isNotNull();
    }
  }

  @Test
  public void canOverwriteDefaultInclusions() throws Exception {
    IarPackagingMojo mojo = rule.getMojo();
    File outputDir = new File(mojo.project.getBasedir(), "target");
    File flatPomXML = new File(outputDir, "pom.xml");
    FileUtils.write(flatPomXML, "<artifactId>flattened</artifactId>", StandardCharsets.UTF_8);

    FileSet fs = new FileSet();
    fs.setDirectory("target");
    fs.setIncludes(Arrays.asList(flatPomXML.getName()));
    mojo.iarFileSets = new FileSet[] {fs};

    mojo.execute();
    try (ZipFile archive = new ZipFile(mojo.project.getArtifact().getFile())) {
      try (InputStream is = archive.getInputStream(archive.getEntry("pom.xml"))) {
        String pomInArchive = IOUtil.toString(is);
        assertThat(pomInArchive)
                .as("customer should be able to overwrite pre-defined resource with their own includes.")
                .contains("flattened");
      }

      List<? extends ZipEntry> pomEntries = Collections.list(archive.entries()).stream()
              .filter(entry -> entry.getName().equals("pom.xml"))
              .collect(Collectors.toList());
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
      assertThat(archive.getEntry("src_hd")).as("Empty directory should be excluded by mojo configuration")
              .isNull();
    }
  }

  @Test
  public void doNotPackTargetFolderIfThereAreNoTargetClasses() throws Exception {
    IarPackagingMojo mojo = rule.getMojo();
    File targetClasses = new File(mojo.project.getBasedir(), "target/classes");
    FileUtils.deleteDirectory(targetClasses);
    mojo.execute();

    Collection<File> iarFiles = FileUtils.listFiles(new File(mojo.project.getBasedir(), "target"),
            new String[] {"iar"}, false);
    assertThat(iarFiles).hasSize(1);

    File iarFile = iarFiles.iterator().next();
    try (ZipFile archive = new ZipFile(iarFile)) {
      assertThat(archive.getEntry("target"))
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

    Collection<File> iarFiles = FileUtils.listFiles(new File(mojo.project.getBasedir(), "target"),
            new String[] {"iar"}, false);
    assertThat(iarFiles).hasSize(1);

    File iarFile = iarFiles.iterator().next();
    try (ZipFile archive = new ZipFile(iarFile)) {
      assertThat(archive.getEntry("target/src_hd/com/acme/FormDialog/FormDialog.xhtml"))
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
}
