package ch.ivyteam.ivy.maven.test.bpm;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.maven.project.MavenProject;

public class IvyTestRuntime {

  public static final String RUNTIME_PROPS_RESOURCE = "ivyTestRuntime.properties";

  public interface Key {
    String PRODUCT_DIR = "product.dir";
    String PROJECT_LOCATIONS = "project.locations";
  }

  private final Properties props = new Properties();

  public void setProductDir(Path engineDir) {
    props.put(Key.PRODUCT_DIR, engineDir.toAbsolutePath().toString());
  }

  public void setProjectLocations(List<URI> locations) {
    var joinedUris = locations
        .stream()
        .map(URI::toASCIIString)
        .map(uri -> "<" + uri + ">") // RFC 3986 Appendix C
        .collect(Collectors.joining(", "));
    props.setProperty(Key.PROJECT_LOCATIONS, joinedUris);
  }

  public Path store(MavenProject project) throws IOException {
    var tstVmDir = Path.of(project.getBuild().getDirectory()).resolve("ivyTestVm");
    Files.createDirectories(tstVmDir);
    store(tstVmDir);
    return tstVmDir;
  }

  private Path store(Path dir) throws IOException {
    var propsFile = dir.resolve(RUNTIME_PROPS_RESOURCE);
    try (var out = Files.newOutputStream(propsFile)) {
      props.store(out, "ivy test vm runtime properties");
    }
    return propsFile;
  }
}
