package ch.ivyteam.ivy.maven.engine.deploy.dir;

import java.io.File;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;

/**
 * Engine status files from deployment.
 */
public class DeploymentFiles
{
  private static final String LOG = ".deploymentLog";
  private static final String ERROR_LOG = ".deploymentError";

  private File deployable;

  public DeploymentFiles(File deployable)
  {
    this.deployable = deployable;
  }

  File getDeployCandidate()
  {
    return deployable;
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
    for(String markerExtension : Arrays.asList(LOG, ERROR_LOG))
    {
      FileUtils.deleteQuietly(getFile(markerExtension));
    }
  }
}