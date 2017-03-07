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

package ch.ivyteam.ivy.maven.engine;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * Provides project build functionality that can only be accessed trough reflection on an ivy Engine classloader.
 * 
 * @author Reguel Wermelinger
 * @since 6.0.0
 */
public class MavenProjectBuilderProxy
{
  private static final String FQ_DELEGATE_CLASS_NAME = "ch.ivyteam.ivy.project.build.MavenProjectBuilder";
  private Object delegate;
  private Class<?> delegateClass;
  private File baseDirToBuildIn;

  public MavenProjectBuilderProxy(URLClassLoader ivyEngineClassLoader, File workspace, File baseDirToBuildIn, List<File> engineJars) throws Exception
  {
    this.baseDirToBuildIn = baseDirToBuildIn;
    
    delegateClass = getOsgiBundledDelegate(ivyEngineClassLoader);
    Constructor<?> constructor = delegateClass.getDeclaredConstructor(File.class, String.class);
    
    delegate = constructor.newInstance(workspace, getEngineClasspath(engineJars));
  }

  private Class<?> getOsgiBundledDelegate(URLClassLoader ivyEngineClassLoader) throws ClassNotFoundException,
          NoSuchMethodException, Exception, IllegalAccessException, InvocationTargetException
  {
    Object bundleContext = startEclipseOsgiImpl(ivyEngineClassLoader);
    installSlf4j(bundleContext);
    Object buildBundle = findBundle(bundleContext, "ch.ivyteam.ivy.dataclasses.build");
    return (Class<?>) buildBundle.getClass().getDeclaredMethod("loadClass", String.class)
            .invoke(buildBundle, FQ_DELEGATE_CLASS_NAME);
  }

  private void installSlf4j(Object bundleContext) throws IllegalAccessException, InvocationTargetException,
          NoSuchMethodException, MalformedURLException
  {
    Object slf4j_api = installBundle(bundleContext, "C:/Users/rew.SORECO/.m2/repository/org/slf4j/slf4j-api/1.7.7/slf4j-api-1.7.7.jar");
    Object slf4j_simple = installBundle(bundleContext, "C:/Users/rew.SORECO/.m2/repository/org/slf4j/slf4j-simple/1.7.7/slf4j-simple-1.7.7.jar");
    Object slf4j_brige_log4j = installBundle(bundleContext, "C:/Users/rew.SORECO/.m2/repository/org/slf4j/log4j-over-slf4j/1.7.7/log4j-over-slf4j-1.7.7.jar");
    startBundle(slf4j_api);
    startBundle(slf4j_brige_log4j);
  }

  private Object installBundle(Object bundleContext, String path) throws IllegalAccessException, InvocationTargetException,
          NoSuchMethodException, MalformedURLException
  {
    return bundleContext.getClass().getDeclaredMethod("installBundle", String.class)
            .invoke(bundleContext, uriOf(path));
  }

  private static Object startBundle(Object bundle)
          throws IllegalAccessException, InvocationTargetException, NoSuchMethodException
  {
    return bundle.getClass().getMethod("start").invoke(bundle);
  }

  private static String uriOf(String fileAbsolute) throws MalformedURLException
  {
    File file = new File(fileAbsolute);
    return file.toURI().toASCIIString();
  }
  
  private Object startEclipseOsgiImpl(URLClassLoader ivyEngineClassLoader) throws ClassNotFoundException,
          NoSuchMethodException, Exception, IllegalAccessException, InvocationTargetException
  {
    Class<?> osgiBooter = ivyEngineClassLoader.loadClass("org.eclipse.core.runtime.adaptor.EclipseStarter");
    Method mainMethod = osgiBooter.getDeclaredMethod("main", String[].class);
    String[] args = new String[]{"-debug"};
    executeInEngineDir(() -> mainMethod.invoke(null, (Object)args));
    Object bundleContext = osgiBooter.getDeclaredMethod("getSystemBundleContext").invoke(null);
    return bundleContext;
  }

  private static Object findBundle(Object bundleContext, String symbolicName)
          throws IllegalAccessException, InvocationTargetException, NoSuchMethodException
  {
    Object[] bundles = (Object[]) bundleContext.getClass().getDeclaredMethod("getBundles").invoke(bundleContext);
    for(Object bundleObj : bundles)
    {
      Object bundleSymbolicName = bundleObj.getClass().getMethod("getSymbolicName").invoke(bundleObj);
      if (symbolicName.equals(bundleSymbolicName))
      {
        return bundleObj;
      }
    }
    return null;
  }
  
  private static String getEngineClasspath(List<File> jars)
  {
    return jars.stream()
            .map(file -> file.getAbsolutePath())
            .collect(Collectors.joining(File.pathSeparator));
  }

  /**
   * @param iarDependencies dependencies of type IAR
   * @return create IAR-JARs
   * @throws Exception if creation fails
   */
  @SuppressWarnings("unchecked")
  public List<File> createIarJars(List<File> iarDependencies) throws Exception
  {
    Method iarJarMethod = getMethod("createIarJars", List.class);
    return (List<File>) executeInEngineDir(() -> 
      iarJarMethod.invoke(delegate, iarDependencies)
    );
  }
  
  @SuppressWarnings("unchecked")
  public Map<String, Object> compile(File projectDirToBuild, List<File> iarJars, Map<String, String> options) throws Exception
  {
    Method compileMethod = getMethod("compile", File.class, List.class, Map.class);
    return (Map<String, Object>) executeInEngineDir(() -> 
      compileMethod.invoke(delegate, projectDirToBuild, iarJars, options)
    );
  }
  
  @SuppressWarnings("unchecked")
  public Map<String, Object> testCompile(File projectDirToBuild, List<File> iarJars, Map<String, String> options) throws Exception
  {
    Method compileMethod = getMethod("testCompile", File.class, List.class, Map.class);
    return (Map<String, Object>) executeInEngineDir(() -> 
      compileMethod.invoke(delegate, projectDirToBuild, iarJars, options)
    );
  }
  
  private Method getMethod(String name, Class<?>... parameterTypes)
  {
    try
    {
      return delegateClass.getDeclaredMethod(name, parameterTypes);
    }
    catch (NoSuchMethodException ex)
    {
      throw new RuntimeException(
              "Method "+name+"("+parameterTypes+") does not exist in engine '"+baseDirToBuildIn+"'. \n"
                      + "You might need to configer another version to work with.");
    }
  }
  
  private <T> T executeInEngineDir(Callable<T> function) throws Exception
  {
    String originalBaseDirectory = System.getProperty("user.dir");
    System.setProperty("user.dir", baseDirToBuildIn.getAbsolutePath());
    try
    {
      return function.call();
    }
    finally
    {
      System.setProperty("user.dir", originalBaseDirectory);
    }
  }
  
  
  public static interface Options
  {
    String TEST_SOURCE_DIR = "project.build.testSourceDirectory";
    String COMPILE_CLASSPATH = "maven.dependency.classpath";
    String SOURCE_ENCODING = "project.source.encoding";
  }
  
  public static interface Result
  {
    String TEST_OUTPUT_DIR = "ivy.project.test.output.dir";
  }

}