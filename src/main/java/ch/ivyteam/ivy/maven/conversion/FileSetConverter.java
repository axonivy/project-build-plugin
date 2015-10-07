package ch.ivyteam.ivy.maven.conversion;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.plexus.archiver.util.DefaultFileSet;

public class FileSetConverter
{
  private File pomFileDir;
  
  public FileSetConverter(File pomFileDir)
  {
    this.pomFileDir = pomFileDir;
  }
  
  public List<org.codehaus.plexus.archiver.FileSet> toPlexusFileSets(org.apache.maven.model.FileSet[] mavenFileSets)
  {
    if (mavenFileSets == null)
    {
      return Collections.emptyList();
    }
    
    List<org.codehaus.plexus.archiver.FileSet> plexusFileSets = new ArrayList<>();
    for(org.apache.maven.model.FileSet fs : mavenFileSets)
    {
      plexusFileSets.add(toPlexusFileset(fs));
    }
    return plexusFileSets;
  }

  private org.codehaus.plexus.archiver.FileSet toPlexusFileset(org.apache.maven.model.FileSet mavenFs)
  {
    DefaultFileSet plexusFs = new DefaultFileSet();
    plexusFs.setDirectory(readDirectory(mavenFs));
    plexusFs.setIncludes(mavenFs.getIncludes().toArray(new String[0]));
    plexusFs.setExcludes(mavenFs.getExcludes().toArray(new String[0]));
    plexusFs.setUsingDefaultExcludes(false);
    plexusFs.setIncludingEmptyDirectories(true);
    return plexusFs;
  }

  private File readDirectory(org.apache.maven.model.FileSet mavenFs)
  {
    if (StringUtils.isBlank(mavenFs.getDirectory()))
    {
      return pomFileDir;
    }
    return new File(pomFileDir, mavenFs.getDirectory());
  }
  
}