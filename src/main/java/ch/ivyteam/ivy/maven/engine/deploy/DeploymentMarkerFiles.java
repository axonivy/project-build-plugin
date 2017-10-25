package ch.ivyteam.ivy.maven.engine.deploy;

import java.io.File;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;

/**
 * Engine status marker files to steer the deployment.
 */
public class DeploymentMarkerFiles
{
  private static final String DO_DEPLOY = ".doDeploy";
  private static final String LOG = ".deploymentLog";
  private static final String ERROR_LOG = ".deploymentError";
  
  private File deployable;
  
  public DeploymentMarkerFiles(File deployable)
  {
    this.deployable = deployable;
  }
  
  File getDeployCandidate()
  {
    return deployable;
  }

  public File doDeploy()
  {
    return getFile(DO_DEPLOY);
  }
  
  public File log()
  {
    return getFile(LOG);
  }
  
  public File errorLog()
  {
    return getFile(ERROR_LOG);
  }

  private File getFile(String markerExtension)
  {
    return new File(deployable.getParent(), deployable.getName()+markerExtension);
  }
  
  public void clearAll()
  {
    for(String markerExtension : Arrays.asList(DO_DEPLOY, LOG, ERROR_LOG))
    {
      FileUtils.deleteQuietly(getFile(markerExtension));
    }
  }
}