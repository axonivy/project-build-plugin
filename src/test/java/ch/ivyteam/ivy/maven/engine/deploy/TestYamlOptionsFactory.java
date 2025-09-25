package ch.ivyteam.ivy.maven.engine.deploy;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import ch.ivyteam.ivy.maven.deploy.DeployToEngineMojo;
import ch.ivyteam.ivy.maven.deploy.DeployToEngineMojo.DefaultDeployOptions;

class TestYamlOptionsFactory {

  @Test
  void yamlWithAllNonDefaultOptions() throws Exception {
    DeployToEngineMojo config = new DeployToEngineMojo();
    config.deployTestUsers = "true";
    config.deployTargetVersion = "RELEASED";
    config.deployTargetState = "ACTIVE";

    String yamlOptions = YamlOptionsFactory.toYaml(config);
    assertThat(yamlOptions).isEqualTo(getFileContent("allOptionsSet.yaml"));
  }

  private String getFileContent(String file) throws IOException {
    try (var in = getClass().getResourceAsStream(file)) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @Test
  void yamlWithAllDefaultOptions() throws Exception {
    DeployToEngineMojo config = new DeployToEngineMojo();
    config.deployTestUsers = DefaultDeployOptions.DEPLOY_TEST_USERS;
    config.deployTargetVersion = DefaultDeployOptions.VERSION_AUTO;
    config.deployTargetState = DefaultDeployOptions.STATE_ACTIVE_AND_RELEASED;

    String yamlOptions = YamlOptionsFactory.toYaml(config);
    assertThat(yamlOptions).isNullOrEmpty();
  }
}
