package ch.ivyteam.ivy.maven.engine.deploy;

import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.MINIMIZE_QUOTES;
import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.WRITE_DOC_START_MARKER;

import java.io.IOException;
import java.io.StringWriter;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import ch.ivyteam.ivy.maven.DeployToEngineMojo;
import ch.ivyteam.ivy.maven.DeployToEngineMojo.DefaultDeployOptions;

/**
 * @since 7.1.0
 */
public class YamlOptionsFactory
{

  private static YAMLFactory yamlFactory;
  static
  {
    yamlFactory = new YAMLFactory();
    yamlFactory.configure(MINIMIZE_QUOTES, true);
    yamlFactory.configure(WRITE_DOC_START_MARKER, false);
  }

  public static String toYaml(DeployToEngineMojo config) throws IOException
  {
    StringWriter writer = new StringWriter();
    JsonGenerator gen = yamlFactory.createGenerator(writer);
    
    gen.writeStartObject(); // root
    writeTestUsers(config, gen);
    writeConfig(config, gen);
    writeTarget(config, gen);
    gen.writeEndObject();
    
    gen.close();
    String yaml = writer.toString();
    if (yaml.equals("{}\n"))
    {
      return null;
    }
    
    return yaml;
  }

  private static void writeTestUsers(DeployToEngineMojo config, JsonGenerator gen) throws IOException
  {
    String deployTestUsers = config.deployTestUsers;
    if (!DefaultDeployOptions.DEPLOY_TEST_USERS.equalsIgnoreCase(deployTestUsers))
    {
      gen.writeStringField("deployTestUsers", StringUtils.defaultString(deployTestUsers).toUpperCase());
    }
  }
  
  private static void writeConfig(DeployToEngineMojo config, JsonGenerator gen) throws IOException
  {
    boolean defaultCleanup = DefaultDeployOptions.CLEANUP_DISABLED.equals(config.deployConfigCleanup);
    if (config.deployConfigOverwrite || !defaultCleanup)
    {
      gen.writeObjectFieldStart("configuration");
      if (config.deployConfigOverwrite)
      {
        gen.writeBooleanField("overwrite", config.deployConfigOverwrite);
      }
      if (!defaultCleanup)
      {
        // validate and log invalid values!
        gen.writeStringField("cleanup", config.deployConfigCleanup);
      }
      gen.writeEndObject();
    }
  }

  private static void writeTarget(DeployToEngineMojo config, JsonGenerator gen) throws IOException
  {
    boolean defaultVersion = DefaultDeployOptions.VERSION_AUTO.equals(config.deployTargetVersion);
    boolean defaultState = DefaultDeployOptions.STATE_ACTIVE_AND_RELEASED.equals(config.deployTargetState);
    boolean defaultFileFormat = DefaultDeployOptions.FILE_FORMAT_AUTO.equals(config.deployTargetFileFormat);
    if (!defaultVersion || !defaultState || !defaultFileFormat)
    {
      gen.writeObjectFieldStart("target");
      if (!defaultVersion)
      {
        gen.writeStringField("version", config.deployTargetVersion);
      }
      if (!defaultState)
      {
        gen.writeStringField("state", config.deployTargetState);
      }
      if (!defaultFileFormat)
      {
        gen.writeStringField("fileFormat", config.deployTargetFileFormat);
      }
      
      gen.writeEndObject();
    }
  }

}
