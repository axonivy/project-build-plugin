package ch.ivyteam.ivy.maven.engine.deploy;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.PrefixFileFilter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.apache.maven.shared.filtering.MavenFilteringException;

public class DeploymentOptionsFile
{
  public static final DeploymentOptionsFile NO_OPTIONS = new DeploymentOptionsFile(null, null, null, null);
  
  private MavenFileFilter fileFilter;
  private File optionsFile;
  private File deployableFile;
  private MavenProject project;
  private MavenSession session;
  
  public DeploymentOptionsFile(File optionsFile, MavenProject project, MavenSession session, MavenFileFilter fileFilter)
  {
    this.optionsFile = optionsFile;
    this.project = project;
    this.session = session;
    this.fileFilter = fileFilter;
  }
  
  public void setDeployableFile(File deployableFile)
  {
    this.deployableFile = deployableFile;
  }
  
  public void copy() throws MavenFilteringException
  {
    if (!hasOptionsFile())
    {
      return;
    }
    File targetFile = getTargetFile();
    targetFile.getParentFile().mkdirs();
    fileFilter.copyFile(optionsFile, targetFile, true, project, Collections.emptyList(), false, StandardCharsets.UTF_8.name(), session);
  }
  
  public void clear()
  {
    File deployFolder = deployableFile.getParentFile();
    if (!deployFolder.exists())
    {
      return;
    }

    PrefixFileFilter optionsFileFilter = new PrefixFileFilter(getTargetFilePrefix());
    Collection<File> targetOptionsFiles = FileUtils.listFiles(deployFolder, optionsFileFilter, null);
    for (File targetOptionFile : targetOptionsFiles)
    {
      FileUtils.deleteQuietly(targetOptionFile);
    }
  }

  private File getTargetFile()
  {
    String targetFileName = getTargetFilePrefix()+FilenameUtils.getExtension(optionsFile.getName());
    return new File(deployableFile.getParentFile(), targetFileName);
  }

  private String getTargetFilePrefix()
  {
    return deployableFile.getName()+".options.";
  }

  private boolean hasOptionsFile()
  {
    return optionsFile != null && 
           optionsFile.exists() && 
           optionsFile.isFile() && 
           optionsFile.canRead();
  }
  
}
