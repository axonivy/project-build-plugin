package ch.ivyteam.ivy.maven.engine.deploy.dir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Engine status files from deployment.
 */
public class DeploymentFiles {

  private static final String LOG = ".deploymentLog";
  private static final String ERROR_LOG = ".deploymentError";

  private final Path deployable;

  public DeploymentFiles(Path deployable) {
    this.deployable = deployable;
  }

  Path getDeployCandidate() {
    return deployable;
  }

  public Path log() {
    return toFile(LOG);
  }

  public Path errorLog() {
    return toFile(ERROR_LOG);
  }

  private Path toFile(String ext) {
    return deployable.resolveSibling(deployable.getFileName() + ext);
  }

  public void clearAll() {
    for (var ext : List.of(LOG, ERROR_LOG)) {
      var file = toFile(ext);
      if (Files.exists(file)) {
        try {
          Files.delete(file);
        } catch (IOException ex) {
          throw new RuntimeException("Could not delete " + file, ex);
        }
      }
    }
  }
}
