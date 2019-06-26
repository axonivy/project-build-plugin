package ch.ivyteam.ivy.maven.engine.download;

import java.io.File;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

public class MavenEngineDownloader implements EngineDownloader 
{
  private final Log log;
  private final List<RemoteRepository> pluginRepositories;
  private final RepositorySystem repositorySystem;
  private final RepositorySystemSession repositorySession;
  private final DefaultArtifact engineArtifact;

  public MavenEngineDownloader(Log log, String ivyVersion, String osArchitecture, 
          List<RemoteRepository> pluginRepositories, RepositorySystem repositorySystem, RepositorySystemSession repositorySession)
  {
    this.log = log;
    this.engineArtifact = toEngineArtifact(ivyVersion, osArchitecture);
    this.repositorySystem = repositorySystem;
    this.repositorySession = repositorySession;
    this.pluginRepositories = pluginRepositories;
  }

  public static DefaultArtifact toEngineArtifact(String ivyVersion, String osArchitecture)
  {
    return new DefaultArtifact("com.axonivy.ivy", "engine", osArchitecture, "zip", ivyVersion);
  }

  private ArtifactResult resolveArtifact() throws MojoExecutionException 
  {
    final ArtifactRequest artifactRequest = new ArtifactRequest();
    artifactRequest.setArtifact(engineArtifact);
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
    log.info("Downloading engine " + engineArtifact.getVersion() + " using maven plugin repositories");
    return resolveArtifact().getArtifact().getFile();
  }

  @Override
  public String getZipFileNameFromDownloadLocation() throws MojoExecutionException
  {
    return resolveArtifact().getArtifact().getFile().getName();
  }
}
