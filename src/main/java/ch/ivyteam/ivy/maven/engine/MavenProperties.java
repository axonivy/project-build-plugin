package ch.ivyteam.ivy.maven.engine;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

public class MavenProperties
{

  private MavenProject project;
  private Log log;

  public MavenProperties(MavenProject project, Log log)
  {
    this.project = project;
    this.log = log;
  }

  public void setMavenProperty(String key, String value)
  {
    log.debug("share property '" + key + "' with value '" + StringUtils.abbreviate(value, 500) + "'");
    project.getProperties().put(key, value);
  }

}
