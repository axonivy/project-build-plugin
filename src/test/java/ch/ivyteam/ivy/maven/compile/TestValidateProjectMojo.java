package ch.ivyteam.ivy.maven.compile;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;

import org.apache.commons.lang3.Strings;
import org.apache.maven.api.di.Provides;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import ch.ivyteam.ivy.maven.BaseEngineProjectMojoTest;
import ch.ivyteam.ivy.maven.InstallEngineMojo;
import ch.ivyteam.ivy.maven.engine.Slf4jSimpleEngineProperties;
import ch.ivyteam.ivy.maven.extension.LocalRepoTest;
import ch.ivyteam.ivy.maven.extension.ProjectExtension;
import ch.ivyteam.ivy.maven.extension.SysoutExtension;
import ch.ivyteam.ivy.maven.extension.SysoutExtension.Sysout;
import ch.ivyteam.ivy.maven.generate.GenerateDataClassSourcesMojo;
import ch.ivyteam.ivy.maven.generate.GenerateDialogFormSourcesMojo;
import ch.ivyteam.ivy.maven.generate.GenerateWebServiceSourcesMojo;
import ch.ivyteam.ivy.maven.util.PathUtils;

@MojoTest
@ExtendWith(ProjectExtension.class)
@ExtendWith(SysoutExtension.class)
class TestValidateProjectMojo {

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
  void provideEngine(InstallEngineMojo install) throws Exception {
    BaseEngineProjectMojoTest.provideEngine(install);
  }

  private GenerateDataClassSourcesMojo data;
  private GenerateDialogFormSourcesMojo form;
  private GenerateWebServiceSourcesMojo soap;
  private CompileProjectMojo compile;

  @Provides
  MavenProject mockProject() throws IOException {
    return ProjectExtension.project();
  }

  @Test
  @InjectMojo(goal = ValidateProjectMojo.GOAL)
  void validate(ValidateProjectMojo validate) throws Exception {
    BaseEngineProjectMojoTest.configureMojo(validate);
    validate.localRepository = LocalRepoTest.repo();

    var project = validate.project.getBasedir().toPath();
    var dataClassDir = project.resolve("src_dataClasses");
    var wsProcDir = project.resolve("src_wsproc");
    PathUtils.clean(wsProcDir);
    PathUtils.clean(dataClassDir);

    var ws = project.resolve("processes").resolve("myWebService.p.json");
    String wsJson = Files.readString(ws);
    var patched = Strings.CS.replace(wsJson, "//TEMPLATE!!", "ivy.session.assignRole(null);");
    Files.writeString(ws, patched);

    assertThat(wsProcDir).isEmptyDirectory();
    validate.buildApplicationDirectory = Files.createTempDirectory("MyBuildApplicationVald");
    execGenerateAndCompileMojo(validate);

    validate.execute();

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
  @InjectMojo(goal = CompileProjectMojo.GOAL)
  void setUpCompile(CompileProjectMojo mojo) {
    this.compile = mojo;
  }

  void execGenerateAndCompileMojo(ValidateProjectMojo validate) throws Exception {
    data.execute();
    var builder = validate.getMavenProjectBuilder();
    form.project = validate.project;
    form.engineExec(builder);
    soap.project = validate.project;
    soap.engineExec(builder);
    compile.project = validate.project;
    compile.engineExec(builder);
  }

}
