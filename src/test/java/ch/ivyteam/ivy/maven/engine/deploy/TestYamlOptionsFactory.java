package ch.ivyteam.ivy.maven.engine.deploy;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import ch.ivyteam.ivy.maven.DeployToEngineMojo;

public class TestYamlOptionsFactory
{
  @Test
  public void yamlWithAllNonDefaultOptions() throws Exception
  {
    DeployToEngineMojo config = new DeployToEngineMojo();
    config.deployTestUsers = true;
    config.deployConfigCleanup = "REMOVE_ALL";
    config.deployConfigOverwrite = true;
    config.deployTargetVersion = "RELEASED";
    config.deployTargetState = "ACTIVE";
    config.deployTargetFileFormat = "EXPANDED";
    
    String yamlOptions = YamlOptionsFactory.generate(config);
    assertThat(yamlOptions).isEqualTo(getFileContent("allOptionsSet.yaml"));
  }
  
  private String getFileContent(String file) throws IOException
  {
    try(InputStream is = getClass().getResourceAsStream(file))
    {
      return IOUtils.toString(is);
    }
  }
}
