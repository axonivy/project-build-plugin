package ch.ivyteam.ivy.maven.engine;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * Provides project build functionality that can only be accessed trough reflection on an ivy Engine classloader.
 * 
 * @author Reguel Wermelinger
 * @since 25.09.2014
 */
public class MavenProjectBuilderProxy
{
  private static final String FQ_DELEGATE_CLASS_NAME = "ch.ivyteam.ivy.project.build.MavenProjectBuilder";
  private Object delegate;
  private Class<?> delegateClass;

  public MavenProjectBuilderProxy(URLClassLoader ivyEngineClassLoader, File workspace) throws Exception
  {
    delegateClass = ivyEngineClassLoader.loadClass(FQ_DELEGATE_CLASS_NAME);
    Constructor<?> constructor = delegateClass.getDeclaredConstructor(File.class, String.class);
    delegate = constructor.newInstance(workspace, getClassPath(ivyEngineClassLoader));
  }
  
  private static String getClassPath(URLClassLoader classLoader) throws URISyntaxException
  {
    List<String> pathEntries = new ArrayList<>();
    for(URL url : classLoader.getURLs())
    {
      pathEntries.add(url.toURI().getSchemeSpecificPart());
    }
    return StringUtils.join(pathEntries, File.pathSeparator);
  }
  
  public void execute(File projectDirToBuild, List<File> iarDependencies, File baseDirToBuildIn) throws Exception
  {
    Method mainMethod = delegateClass.getDeclaredMethod("execute", File.class, List.class);
    String originalBaseDirectory = System.getProperty("user.dir");
    System.setProperty("user.dir", baseDirToBuildIn.getAbsolutePath());
    try
    {
      mainMethod.invoke(delegate, projectDirToBuild, iarDependencies);
    }
    finally
    {
      System.setProperty("user.dir", originalBaseDirectory);
    }
  }
}