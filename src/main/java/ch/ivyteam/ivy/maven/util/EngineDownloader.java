package ch.ivyteam.ivy.maven.util;

import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;

public interface EngineDownloader {
    File downloadEngine() throws MojoExecutionException;
    String getZipFileNameFromDownloadLocation();
}
