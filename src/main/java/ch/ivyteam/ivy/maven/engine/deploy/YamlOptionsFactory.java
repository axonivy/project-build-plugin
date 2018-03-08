package ch.ivyteam.ivy.maven.engine.deploy;

import ch.ivyteam.ivy.maven.DeployToEngineMojo;
import ch.ivyteam.ivy.maven.DeployToEngineMojo.DefaultDeployOptions;

/**
 * @since 7.1.0
 */
public class YamlOptionsFactory
{

  public static String generate(DeployToEngineMojo config)
    {
      StringBuilder options = new StringBuilder();
      if (config.deployTestUsers)
      {
        options.append("deployTestUsers: ").append(config.deployTestUsers).append("\n");
      }

      boolean defaultCleanup = DefaultDeployOptions.CLEANUP_DISABLED.equals(config.deployConfigCleanup);
      if (config.deployConfigOverwrite || !defaultCleanup)
      {
        options.append("configuration:\n");
        if (config.deployConfigOverwrite)
        {
          options.append("  overwrite: ").append(config.deployConfigOverwrite).append("\n");
        }
        if (!defaultCleanup)
        {
          options.append("  cleanup: ").append(config.deployConfigCleanup).append("\n"); // validate and log invalid values!
        }
      }

      boolean defaultVersion = DefaultDeployOptions.VERSION_AUTO.equals(config.deployTargetVersion);
      boolean defaultState = DefaultDeployOptions.STATE_ACTIVE_AND_RELEASED.equals(config.deployTargetState);
      boolean defaultFileFormat = DefaultDeployOptions.FILE_FORMAT_AUTO.equals(config.deployTargetFileFormat);
      if (!defaultVersion || !defaultState || !defaultFileFormat)
      {
        options.append("target:\n");
        if (!defaultVersion)
        {
          options.append("  version: ").append(config.deployTargetVersion).append("\n");
        }
        if (!defaultState)
        {
          options.append("  state: ").append(config.deployTargetState).append("\n");
        }
        if (!defaultFileFormat)
        {
          options.append("  fileFormat: ").append(config.deployTargetFileFormat).append("\n");
        }
      }

      return options.toString();
    }

}
