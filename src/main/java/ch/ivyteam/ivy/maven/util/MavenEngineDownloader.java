package ch.ivyteam.ivy.maven.util;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

import java.io.File;
import java.util.List;

import static ch.ivyteam.ivy.maven.InstallEngineMojo.ENGINE_ZIP_GAV_ARTIFACT;
import static ch.ivyteam.ivy.maven.InstallEngineMojo.ENGINE_ZIP_GAV_GROUP;

public class MavenEngineDownloader implements EngineDownloader {

  private Log log;
  private String ivyVersion;
  private String osArchitecture;
  private List<RemoteRepository> pluginRepositories;
  private RepositorySystem repositorySystem;
  private RepositorySystemSession repositorySession;

  public MavenEngineDownloader(Log log, String ivyVersion, String osArchitecture, List<RemoteRepository> pluginRepositories, RepositorySystem repositorySystem, RepositorySystemSession repositorySession)
  {
    this.log = log;
    this.ivyVersion = ivyVersion;
    this.osArchitecture = osArchitecture;
    this.pluginRepositories = pluginRepositories;
    this.repositorySystem = repositorySystem;
    this.repositorySession = repositorySession;
  }

  private ArtifactResult resolveArtifact() throws MojoExecutionException {
    final ArtifactRequest artifactRequest = new ArtifactRequest();
    artifactRequest.setArtifact(new DefaultArtifact(ENGINE_ZIP_GAV_GROUP, ENGINE_ZIP_GAV_ARTIFACT, osArchitecture, "zip", ivyVersion));
    artifactRequest.setRepositories(pluginRepositories);
    try
    {
      return repositorySystem.resolveArtifact(repositorySession, artifactRequest);
    }
    catch(ArtifactResolutionException e)
    {
      throw new MojoExecutionException("Failed to resolve artifact " + artifactRequest + "!", e);
    }
  }

  @Override
  public File downloadEngine() throws MojoExecutionException
  {
    log.info("Downloading engine " + ivyVersion + " using maven plugin repositories");
    return resolveArtifact().getArtifact().getFile();
  }

  @Override
  public String getZipFileNameFromDownloadLocation() throws MojoExecutionException
  {
    return resolveArtifact().getArtifact().getFile().getName();
  }
}
