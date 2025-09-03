package ch.ivyteam.ivy.maven;

import java.io.File;
import java.io.IOException;

import org.apache.commons.compress.parallel.InputStreamSupplier;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.zip.ConcurrentJarCreator;
import org.codehaus.plexus.archiver.zip.ZipArchiver;

public class IvyZipArchiver extends ZipArchiver {

  @Override
  protected void zipFile(
      InputStreamSupplier is,
      ConcurrentJarCreator zOut,
      String vPath,
      long lastModified,
      File fromArchive,
      int mode,
      String symlinkDestination,
      boolean addInParallel)
      throws IOException, ArchiverException {
    super.zipFile(is, zOut, vPath, lastModified, fromArchive, mode, symlinkDestination, false);
  }

}
