/*
 * Copyright (C) 2015 AXON IVY AG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.ivyteam.ivy.maven;

import static org.fest.assertions.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.DefaultArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.plugin.internal.DefaultLegacySupport;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestCompileProjectMojo
{
  private static final String LOCAL_REPOSITORY = getLocalRepoPath();
  private static final String CACHE_DIR = LOCAL_REPOSITORY + "/.cache/ivy-dev";
  
  private static String getLocalRepoPath()
  {
    String locaRepoGlobalProperty = System.getProperty("maven.repo.local");
    if (locaRepoGlobalProperty != null)
    {
      return locaRepoGlobalProperty;
    }
    
    StringBuilder defaultHomePath = new StringBuilder(SystemUtils.USER_HOME)
      .append(File.separatorChar).append(".m2")
      .append(File.separatorChar).append("repository");
    return defaultHomePath.toString();
  }

  @Test
  public void buildWithExistingProject() throws Exception
  {
    CompileProjectMojo mojo = rule.getMojo();
    
    File dataClassDir = new File(mojo.project.getBasedir(), "src_dataClasses");
    File wsProcDir = new File(mojo.project.getBasedir(), "src_wsproc");
    File classDir = new File(mojo.project.getBasedir(), "classes");
    FileUtils.cleanDirectory(wsProcDir);
    FileUtils.cleanDirectory(dataClassDir);
    FileUtils.deleteDirectory(classDir);
    
    mojo.buildApplicationDirectory = Files.createTempDirectory("MyBuildApplication").toFile();
    mojo.execute();
    
    assertThat(findFiles(dataClassDir, "java")).hasSize(2);
    assertThat(findFiles(wsProcDir, "java")).hasSize(1);
    assertThat(findFiles(classDir, "class")).hasSize(5);
  }
  
  private static Collection<File> findFiles(File dir, String fileExtension)
  {
    if (!dir.exists())
    {
      return Collections.emptyList();
    }
    return FileUtils.listFiles(dir, new String[]{fileExtension}, true);
  }

  @Rule
  public ProjectMojoRule<EnsureInstalledEngineMojo> installUpToDateEngineRule = new ProjectMojoRule<EnsureInstalledEngineMojo>(
          new File("src/test/resources/base"), EnsureInstalledEngineMojo.GOAL){
    @Override
    protected void before() throws Throwable {
      super.before();
      
      String alternateEngineListPageUrl = System.getProperty("engineListPageUrl");
      if (alternateEngineListPageUrl != null)
      {
    	  getMojo().engineListPageUrl = new URL(alternateEngineListPageUrl);
      }
      getMojo().engineCacheDirectory = new File(CACHE_DIR);
      deleteOutdatedEngine();
      getMojo().execute();
    }
  
    private void deleteOutdatedEngine() throws IOException
    {
      File engineDir = getMojo().getEngineDirectory();
      getMojo().engineDirectory = engineDir;
      if (!engineDir.exists())
      {
        return;
      }
      
      File releaseNotes = new File(engineDir, "ReleaseNotes.txt");
      if (isOlderThan24h(releaseNotes))
      {
        System.out.println("Deleting cached outdated engine.");
        FileUtils.deleteDirectory(engineDir);
      }
    }

    private boolean isOlderThan24h(File releaseNotes)
    {
      try
      {
        BasicFileAttributes attr = Files.readAttributes(releaseNotes.toPath(), BasicFileAttributes.class);
        long createTimeMillis = attr.creationTime().toMillis();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -1);
        long yesterday = cal.getTimeInMillis();
        boolean cachedEngineIsOlderThan24h = yesterday > createTimeMillis;
        return cachedEngineIsOlderThan24h;
      }
      catch(IOException ex)
      { // corrupt state: previous build did not finish or completely unpack
        return true;
      }
    }
  };
  
  @Rule
  public ProjectMojoRule<CompileProjectMojo> rule = new ProjectMojoRule<CompileProjectMojo>(
          new File("src/test/resources/base"), CompileProjectMojo.GOAL){
    @Override
    protected void before() throws Throwable {
      super.before();
      
      getMojo().localRepository = provideLocalRepository();
      getMojo().engineCacheDirectory = new File(CACHE_DIR);
    }
  
    /**
     * maven-plugin-testing-harness can not inject local repositories (though the real runtime supports it).
     * and the default stubs have no sufficient implementation of getPath(): 
     * @see "http://maven.apache.org/plugin-testing/maven-plugin-testing-harness/examples/repositories.html"
     */
    private ArtifactRepository provideLocalRepository() throws IllegalAccessException
    {
      DefaultArtifactRepositoryFactory factory = new DefaultArtifactRepositoryFactory();
      setVariableValueToObject(factory, "factory", new org.apache.maven.repository.legacy.repository.DefaultArtifactRepositoryFactory());
      
      LegacySupport legacySupport = new DefaultLegacySupport();
      setVariableValueToObject(factory, "legacySupport", legacySupport);
      
      ArtifactRepository localRepository = factory.createArtifactRepository("local", "http://localhost", 
              new DefaultRepositoryLayout(), new ArtifactRepositoryPolicy(), new ArtifactRepositoryPolicy());
      
      setVariableValueToObject(localRepository, "basedir", LOCAL_REPOSITORY);
      
      return localRepository;
    }
  };

}
