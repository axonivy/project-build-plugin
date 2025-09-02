package ch.ivyteam.ivy.maven.generate;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.logging.Log;

public class JarUnpacker {

  private final Path targetDir;
  private final Log log;

  public JarUnpacker(Path targetDir, Log log) {
    this.targetDir = targetDir;
    this.log = log;
  }

  public void unpack(List<Path> jars) throws IOException {
    for (var jar : jars) {
      log.info("Unpacking jar " + jar + " to " + targetDir);
      unpack(jar);
    }
  }

  private void unpack(Path jar) throws IOException {
    try (var fs = FileSystems.newFileSystem(URI.create("jar:" + jar.toAbsolutePath().toUri()), Map.of())) {
      for (var root : fs.getRootDirectories()) {
        try (var walker = Files.walk(root)) {
          walker.map(root::relativize)
              .filter(path -> !path.startsWith("META-INF"))
              .filter(path -> !Files.isDirectory(path))
              .filter(path -> !path.getFileName().toString().endsWith(".java"))
              .forEach(this::copy);
        }
      }
    }
  }

  private void copy(Path source) {
    try {
      var target = targetDir.resolve(source.toString());
      Files.createDirectories(target.getParent());
      Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }
}
