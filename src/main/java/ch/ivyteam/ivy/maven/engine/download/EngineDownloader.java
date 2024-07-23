package ch.ivyteam.ivy.maven.engine.download;

import java.nio.file.Path;

import org.apache.maven.plugin.MojoExecutionException;

public interface EngineDownloader {

  Path downloadEngine() throws MojoExecutionException;
  String getZipFileNameFromDownloadLocation() throws MojoExecutionException;
}
