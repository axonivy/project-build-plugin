package ch.ivyteam.ivy.maven;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.FileSet;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.util.DefaultFileSet;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.codehaus.plexus.util.AbstractScanner;

import ch.ivyteam.ivy.maven.conversion.FileSetConverter;

/**
 * @author Reguel Wermelinger
 * @since 03.10.2014
 */
@Mojo(name = IarPackagingMojo.GOAL)
public class IarPackagingMojo extends AbstractMojo
{
  public static final String GOAL = "pack-iar";
  private static final String[] DEFAULT_INCLUDES = new String[] {"**/*"};
  private static final String[] DEFAULT_EXCLUDES = new String[] {"target", "target/**/*"};

  @Parameter(property = "project", required = true, readonly = true)
  MavenProject project;
  
  /** 
   * Define additional IAR excludes with ANT-style exclusion declarations. 
   * 
   * <p>The default (always active) exclusions are:
   * <ul>
   * <li>All maven default excludes. See {@link AbstractScanner#DEFAULTEXCLUDES}</li>
   * <li>
   * <pre><code>&lt;iarExcludes&gt;
   *    &lt;iarExclude&gt;target/**&#47;*&lt;/iarExclude&gt;
   *    &lt;iarExclude&gt;target&lt;/iarExclude&gt;
   *&lt;/iarExcludes&gt;</code></pre></li>
   *</ul>
   */
  @Parameter
  String[] iarExcludes;
  
  /** 
   * Define additional IAR {@link FileSet fileSets} with ANT-style exclusion declarations. 
   * <pre><code>&lt;iarFileSets&gt;
   *    &lt;iarFileSet&gt;
   *        &lt;includes&gt;
   *            &lt;include&gt;**&#47;*&lt;/include&gt;
   *        &lt;/includes&gt;
   *    &lt;/iarFileSet&gt;
   *&lt;/iarFileSets&gt;</code></pre>
   */
  @Parameter
  FileSet[] iarFileSets;
  
  /** 
   * Includes empty directories in the packed IAR.
   * If set to <code>false</code>, the IAR can not be re-imported as 
   * Designer project as standard project artifacts (e.g. source folders) could be missing.
   */
  @Parameter(defaultValue="true")
  boolean iarIncludesEmptyDirs;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException
  {
    String iarName = project.getArtifactId() + "-" + project.getVersion() + "." + project.getPackaging();
    File iar = new File(project.getBuild().getDirectory(), iarName);
    createIvyArchive(project.getBasedir(), iar);

    Artifact artifact = project.getArtifact();
    artifact.setFile(iar);
    project.setArtifact(artifact);
    getLog().info("Attached " + artifact.toString() + ".");
  }

  private void createIvyArchive(File sourceDir, File targetIar) throws MojoExecutionException
  {
    ZipArchiver archiver = new ZipArchiver();
    archiver.setDestFile(targetIar);
    archiver.addFileSet(getDefaultFileset(sourceDir));
    FileSetConverter fsConverter = new FileSetConverter(project.getBasedir());
    for(org.codehaus.plexus.archiver.FileSet fs : fsConverter.toPlexusFileSets(iarFileSets))
    {
      archiver.addFileSet(fs);
    }
    try
    {
      archiver.createArchive();
    }
    catch (ArchiverException | IOException ex)
    {
      throw new MojoExecutionException("Failed to create IAR: " + targetIar.getAbsolutePath(), ex);
    }
  }

  private DefaultFileSet getDefaultFileset(File sourceDir)
  {
    DefaultFileSet fileSet = new DefaultFileSet();
    fileSet.setDirectory(sourceDir);
    fileSet.setIncludingEmptyDirectories(iarIncludesEmptyDirs);
    fileSet.setIncludes(DEFAULT_INCLUDES);
    fileSet.setExcludes(ArrayUtils.addAll(DEFAULT_EXCLUDES, iarExcludes));
    return fileSet;
  }

}
