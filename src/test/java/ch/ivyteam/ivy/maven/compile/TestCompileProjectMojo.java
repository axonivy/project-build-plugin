package ch.ivyteam.ivy.maven.compile;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.commons.lang3.Strings;
import org.apache.maven.api.di.Provides;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import ch.ivyteam.ivy.maven.BaseEngineProjectMojoTest;
import ch.ivyteam.ivy.maven.InstallEngineMojo;
import ch.ivyteam.ivy.maven.engine.MavenProjectBuilderProxy;
import ch.ivyteam.ivy.maven.engine.Slf4jSimpleEngineProperties;
import ch.ivyteam.ivy.maven.extension.LocalRepoTest;
import ch.ivyteam.ivy.maven.extension.SysoutExtension;
import ch.ivyteam.ivy.maven.extension.SysoutExtension.Sysout;
import ch.ivyteam.ivy.maven.extension.ProjectExtension;
import ch.ivyteam.ivy.maven.generate.GenerateDataClassSourcesMojo;
import ch.ivyteam.ivy.maven.generate.GenerateDialogFormSourcesMojo;
import ch.ivyteam.ivy.maven.generate.GenerateWebServiceSourcesMojo;
import ch.ivyteam.ivy.maven.log.LogCollector;
import ch.ivyteam.ivy.maven.util.PathUtils;

@MojoTest
@ExtendWith(ProjectExtension.class)
@ExtendWith(SysoutExtension.class)
@TestMethodOrder(MethodOrderer.MethodName.class)
class TestCompileProjectMojo {

  private Sysout sysout;

  @BeforeAll
  static void log() {
    Slf4jSimpleEngineProperties.install();
  }

  @BeforeEach
  void setup(Sysout sysout) {
    Slf4jSimpleEngineProperties.enforceSimpleConfigReload();
    this.sysout = sysout;
  }

  @BeforeEach
  @InjectMojo(goal = InstallEngineMojo.GOAL)
  void provideEngine2(InstallEngineMojo install) throws Exception {
    BaseEngineProjectMojoTest.provideEngine(install);
  }

  private CompileTestProjectMojo test;
  private GenerateDataClassSourcesMojo data;
  private GenerateDialogFormSourcesMojo form;
  private GenerateWebServiceSourcesMojo soap;

  @Provides
  MavenProject mockProject() throws IOException {
    return ProjectExtension.project();
  }

  @Test
  @InjectMojo(goal = CompileProjectMojo.GOAL)
  void buildWithExistingProject(CompileProjectMojo compile) throws Exception {
    CompileProjectMojo mojo = compile;

    BaseEngineProjectMojoTest.configureMojo(mojo);
    mojo.localRepository = ch.ivyteam.ivy.maven.extension.LocalRepoTest.repo();

    Build build = mojo.project.getBuild();
    build.setTestSourceDirectory("src_test");
    build.setTestOutputDirectory("target/notMyTestClasses");

    var dataClassDir = mojo.project.getBasedir().toPath().resolve("src_dataClasses");
    var wsProcDir = mojo.project.getBasedir().toPath().resolve("src_wsproc");
    var classDir = mojo.project.getBasedir().toPath().resolve("classes");
    PathUtils.clean(wsProcDir);
    PathUtils.clean(dataClassDir);

    assertThat(classDir)
        .isDirectoryRecursivelyContaining("glob:**gugus.txt")
        .isDirectoryRecursivelyContaining("glob:**ch/ivyteam/test/bla.class");

    mojo.buildApplicationDirectory = Files.createTempDirectory("MyBuildApplication");
    execGenerateMojo(mojo);
    mojo.execute();

    assertThat(findFiles(dataClassDir, "java")).hasSize(3);
    assertThat(findFiles(wsProcDir, "java")).hasSize(1);

    assertThat(classDir)
        .as("nothing is expected to be cleaned during compile phase")
        .isDirectoryRecursivelyContaining("glob:**gugus.txt")
        .isDirectoryRecursivelyContaining("glob:**ch/ivyteam/test/bla.class");
    assertThat(findFiles(classDir, "class"))
        .as("compiled classes must exist, as well as old class files if clean phase was not executed.")
        .hasSize(6);

    test.execute();
    assertThat(findFiles(classDir, "class"))
        .as("compiled classes must contain test resources as well")
        .hasSize(7);
  }

  private static List<Path> findFiles(Path dir, String fileExtension) throws IOException {
    if (!Files.exists(dir)) {
      return List.of();
    }
    try (var stream = Files.walk(dir)) {
      return stream
          .filter(p -> p.getFileName().toString().endsWith("." + fileExtension))
          .toList();
    }
  }

  @Test
  @InjectMojo(goal = CompileProjectMojo.GOAL)
  void compilerSettingsFile_notFoundWarnings(CompileProjectMojo compile) throws Exception {
    LogCollector log = new LogCollector();

    CompileProjectMojo mojo = compile;
    BaseEngineProjectMojoTest.configureMojo(mojo);
    mojo.localRepository = ch.ivyteam.ivy.maven.extension.LocalRepoTest.repo();
    mojo.setLog(log);

    mojo.compilerWarnings = false;
    mojo.compilerSettings = Path.of("path/to/oblivion");

    execGenerateMojo(mojo);
    mojo.execute();
    Assertions.assertThat(log.getWarnings().toString())
        .doesNotContain("Could not locate compiler settings file");

    mojo.compilerWarnings = true;
    mojo.execute();
    Assertions.assertThat(log.getWarnings().toString())
        .contains("Could not locate compiler settings file");
  }

  @Test
  @InjectMojo(goal = CompileProjectMojo.GOAL)
  void A_validateProcess(CompileProjectMojo compile) throws Exception {
    CompileProjectMojo mojo = compile;
    BaseEngineProjectMojoTest.configureMojo(mojo);
    mojo.localRepository = LocalRepoTest.repo();

    Path project = mojo.project.getBasedir().toPath();
    var dataClassDir = project.resolve("src_dataClasses");
    var wsProcDir = project.resolve("src_wsproc");
    PathUtils.clean(wsProcDir);
    PathUtils.clean(dataClassDir);

    var ws = project.resolve("processes").resolve("myWebService.p.json");
    String wsJson = Files.readString(ws);
    var patched = Strings.CS.replace(wsJson, "//TEMPLATE!!", "ivy.session.assignRole(null);");
    Files.writeString(ws, patched);

    assertThat(wsProcDir.toFile().list()).isEmpty();
    mojo.buildApplicationDirectory = Files.createTempDirectory("MyBuildApplicationVald");
    execGenerateMojo(mojo);

    mojo.execute();

    assertThat(sysout.toString())
        .contains("processes/myWebService.p.json /element=148CA74B16C580BF-ws0 : "
            + "Start code: Method assignRole of class ch.ivyteam.ivy.workflow.IWorkflowSession "
            + "is deprecated");

    var warning = sysout.toString().lines()
        .filter(l -> l.contains("/element=148CA74B16C580BF-ws0"))
        .findFirst().get();
    assertThat(warning)
        .as("WARNING prefix is streamlined with Maven CLI")
        .startsWith("[WARNING]");
  }

  @BeforeEach
  @InjectMojo(goal = GenerateDataClassSourcesMojo.GOAL)
  void setUpDataGen(GenerateDataClassSourcesMojo mojo) {
    this.data = mojo;
  }

  @BeforeEach
  @InjectMojo(goal = GenerateDialogFormSourcesMojo.GOAL)
  void setUpFormGen(GenerateDialogFormSourcesMojo mojo) {
    this.form = mojo;
  }

  @BeforeEach
  @InjectMojo(goal = GenerateWebServiceSourcesMojo.GOAL)
  void setUpWebServiceGen(GenerateWebServiceSourcesMojo mojo) {
    this.soap = mojo;
  }

  @BeforeEach
  @InjectMojo(goal = CompileTestProjectMojo.GOAL)
  void setupTestMojo(CompileTestProjectMojo mojo) {
    this.test = mojo;
  }

  void execGenerateMojo(CompileProjectMojo compile) throws Exception {
    data.execute();
    MavenProjectBuilderProxy builder = compile.getMavenProjectBuilder();
    form.project = compile.project;
    form.engineExec(builder);
    soap.project = compile.project;
    soap.engineExec(builder);
  }

}
