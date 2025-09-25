package ch.ivyteam.ivy.maven.engine.deploy;

import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.MINIMIZE_QUOTES;
import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.WRITE_DOC_START_MARKER;

import java.io.IOException;
import java.io.StringWriter;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import ch.ivyteam.ivy.maven.deploy.AbstractDeployMojo;
import ch.ivyteam.ivy.maven.deploy.DeployToEngineMojo.DefaultDeployOptions;

/**
 * @since 7.1.0
 */
public class YamlOptionsFactory {
  private static YAMLFactory yamlFactory = initYamlFactory();

  private static YAMLFactory initYamlFactory() {
    YAMLFactory factory = new YAMLFactory();
    factory.configure(MINIMIZE_QUOTES, true);
    factory.configure(WRITE_DOC_START_MARKER, false);
    return factory;
  }

  public static String toYaml(AbstractDeployMojo config) throws IOException {
    StringWriter writer = new StringWriter();
    JsonGenerator gen = yamlFactory.createGenerator(writer);

    gen.writeStartObject(); // root
    writeTestUsers(config, gen);
    writeTarget(config, gen);
    gen.writeEndObject();

    gen.close();
    String yaml = writer.toString();
    if ("{}\n".equals(yaml)) {
      return null;
    }

    return yaml;
  }

  private static void writeTestUsers(AbstractDeployMojo config, JsonGenerator gen) throws IOException {
    String deployTestUsers = config.deployTestUsers;
    if (!DefaultDeployOptions.DEPLOY_TEST_USERS.equalsIgnoreCase(deployTestUsers)) {
      gen.writeStringField("deployTestUsers", StringUtils.defaultString(deployTestUsers).toUpperCase());
    }
  }

  private static void writeTarget(AbstractDeployMojo config, JsonGenerator gen) throws IOException {
    boolean defaultVersion = DefaultDeployOptions.VERSION_AUTO.equals(config.deployTargetVersion);
    boolean defaultState = DefaultDeployOptions.STATE_ACTIVE_AND_RELEASED.equals(config.deployTargetState);
    if (!defaultVersion || !defaultState) {
      gen.writeObjectFieldStart("target");
      if (!defaultVersion) {
        gen.writeStringField("version", config.deployTargetVersion);
      }
      if (!defaultState) {
        gen.writeStringField("state", config.deployTargetState);
      }
      gen.writeEndObject();
    }
  }

}
