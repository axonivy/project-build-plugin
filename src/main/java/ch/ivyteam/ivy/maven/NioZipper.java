package ch.ivyteam.ivy.maven;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.zip.ZipArchiver;

final class NioZipper extends ZipArchiver {
  @Override
  protected void execute() throws ArchiverException, IOException {
    if (!checkForced()) {
      return;
    }

    URI zipUri = java.net.URI.create("jar:" + getDestFile().toURI());
    Map<String, String> options = Map.of("create", Boolean.TRUE.toString());
    try (FileSystem zipFs = FileSystems.newFileSystem(zipUri, options)) {
      getResources().forEachRemaining(entry -> {
        String relative = entry.getName();
        Path zipped = zipFs.getPath(relative);
        // System.out.println("copying " + zipped);
        if (entry.getResource().isDirectory()) {
          try {
            Files.createDirectories(zipped);
          } catch (IOException ex) {
            throw new RuntimeException(ex);
          }
        } else if (entry.getResource().isFile()) {
          try {
            Path parent = zipped.getParent();
            if (parent != null) {
              Files.createDirectories(parent);
            }
            try (var archiveIn = entry.getInputStream()) {
              Files.copy(entry.getInputStream(), zipped);
            }
          } catch (IOException ex) {
            throw new RuntimeException(ex);
          }
        }

      });
    }
  }
}
