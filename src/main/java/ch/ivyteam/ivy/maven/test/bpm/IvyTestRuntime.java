package ch.ivyteam.ivy.maven.test.bpm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.maven.project.MavenProject;

public class IvyTestRuntime {
  public static final String RUNTIME_PROPS_RESOURCE = "ivyTestRuntime.properties";

  public static interface Key {
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

  public File store(MavenProject project) throws IOException {
    File target = new File(project.getBuild().getDirectory());
    File tstVmDir = new File(target, "ivyTestVm");
    tstVmDir.mkdir();
    store(tstVmDir);
    return tstVmDir;
  }

  private File store(File dir) throws IOException {
    File propsFile = new File(dir, RUNTIME_PROPS_RESOURCE);
    try (OutputStream os = new FileOutputStream(propsFile)) {
      props.store(os, "ivy test vm runtime properties");
    }
    return propsFile;
  }

}
