package ch.ivyteam.ivy.maven.engine.deploy;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.apache.maven.shared.filtering.MavenFilteringException;

public class DeploymentOptionsFileFactory
{
  private static final String YAML = "yaml";
  private final File deployableArtifact;

  public DeploymentOptionsFileFactory(File deployableArtifact)
  {
    this.deployableArtifact = deployableArtifact;
  }
  
  public File createFromTemplate(File optionsFile, MavenProject project, MavenSession session, MavenFileFilter fileFilter) throws MojoExecutionException
  {
    if (!isOptionsFile(optionsFile))
    {
      return null;
    }
    
    String fileFormat = FilenameUtils.getExtension(optionsFile.getName());
    File targetFile = getTargetFile(fileFormat);
    try
    {
      fileFilter.copyFile(optionsFile, targetFile, true, project, Collections.emptyList(), false, StandardCharsets.UTF_8.name(), session);
    }
    catch (MavenFilteringException ex)
    {
      throw new MojoExecutionException("Failed to resolve templates in options file", ex);
    }
    return targetFile;
  }
  
  private static boolean isOptionsFile(File optionsFile)
  {
    return optionsFile != null && 
           optionsFile.exists() && 
           optionsFile.isFile() && 
           optionsFile.canRead();
  }
  
  public File createFromConfiguration(String options) throws MojoExecutionException
  {
    File yamlOptionsFile = getTargetFile(YAML);
    try
    {
      FileUtils.write(yamlOptionsFile, options, StandardCharsets.UTF_8);
    }
    catch (IOException ex)
    {
      throw new MojoExecutionException("Failed to write options file '"+yamlOptionsFile+"'.", ex);
    }
    return yamlOptionsFile;
  }
  
  private File getTargetFile(String fileFormat)
  {
    String prefix = deployableArtifact.getName()+".options.";
    String targetFileName = prefix+fileFormat;
    return new File(deployableArtifact.getParentFile(), targetFileName);
  }
  
}
