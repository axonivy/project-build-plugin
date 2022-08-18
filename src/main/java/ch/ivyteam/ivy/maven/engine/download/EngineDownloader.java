package ch.ivyteam.ivy.maven.engine.download;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;

public interface EngineDownloader {
  File downloadEngine() throws MojoExecutionException;

  String getZipFileNameFromDownloadLocation() throws MojoExecutionException;
}
