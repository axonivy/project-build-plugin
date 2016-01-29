package ch.ivyteam.ivy.maven.engine.deploy;

import java.io.File;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;

/**
 * Engine status marker files to steer the deployment.
 */
public class DeploymentMarkerFile
{
  private static final String DO_DEPLOY = ".dodeploy";
  private static final String ERROR_LOG = ".deploymentError";
  
  private File iar;
  
  public DeploymentMarkerFile(File iar)
  {
    this.iar = iar;
  }
  
  File getDeployCandidate()
  {
    return iar;
  }

  public File doDeploy()
  {
    return getFile(DO_DEPLOY);
  }
  
  public File errorLog()
  {
    return getFile(ERROR_LOG);
  }

  private File getFile(String markerExtension)
  {
    return new File(iar.getParent(), iar.getName()+markerExtension);
  }
  
  public void clearAll()
  {
    for(String markerExtension : Arrays.asList(DO_DEPLOY, ERROR_LOG))
    {
      FileUtils.deleteQuietly(getFile(markerExtension));
    }
  }
}