/*
 * Copyright (C) 2018 AXON Ivy AG
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
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.apache.maven.plugin.logging.Log;

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
  private String engineClasspath;
  private final Log log;

  public MavenProjectBuilderProxy(EngineClassLoaderFactory classLoaderFactory, File workspace, File baseDirToBuildIn, Log log, int timeoutEngineStartInSeconds) throws Exception
  {
    this.baseDirToBuildIn = baseDirToBuildIn;
    this.log = log;
    
    URLClassLoader ivyEngineClassLoader = classLoaderFactory.createEngineClassLoader(baseDirToBuildIn);
    delegateClass = getOsgiBundledDelegate(ivyEngineClassLoader, timeoutEngineStartInSeconds);
    Constructor<?> constructor = delegateClass.getDeclaredConstructor(File.class);
    
    delegate = executeInEngineDir(() -> constructor.newInstance(workspace));

    List<File> engineJars = EngineClassLoaderFactory.getIvyEngineClassPathFiles(baseDirToBuildIn);
    engineClasspath = getEngineClasspath(engineJars);
  }

  private Class<?> getOsgiBundledDelegate(URLClassLoader ivyEngineClassLoader, int timeoutEngineStartInSeconds) throws Exception
  { 
    Object bundleContext = new OsgiRuntime(baseDirToBuildIn, log).startEclipseOsgiImpl(ivyEngineClassLoader, timeoutEngineStartInSeconds);
    hackProvokeEagerStartOfJdt(bundleContext);
    Object buildBundle = findBundle(bundleContext, "ch.ivyteam.ivy.dataclasses.build");
    return loadClassInBundle(buildBundle, FQ_DELEGATE_CLASS_NAME);
  }

  private static Class<?> loadClassInBundle(Object bundle, String className) throws Exception
  {
    return (Class<?>) bundle.getClass().getDeclaredMethod("loadClass", String.class).invoke(bundle, className);
  }
  
  private static Object findBundle(Object bundleContext, String symbolicName) throws Exception
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
    throw new RuntimeException("Failed to resolve bundle with symbolice name '"+symbolicName+"'.");
  }
  
  private static String getEngineClasspath(List<File> jars)
  {
    return jars.stream()
            .map(file -> file.getAbsolutePath())
            .collect(Collectors.joining(File.pathSeparator));
  }
  
  /**
   * @since 7.3.0
   */
  private static void hackProvokeEagerStartOfJdt(Object bundleContext) throws Exception
  {
    Object jdtBundle = findBundle(bundleContext, "org.eclipse.jdt.core");
    Class<?> javaCore = loadClassInBundle(jdtBundle, "org.eclipse.jdt.core.JavaCore");
    javaCore.getConstructor().newInstance();
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
  public Map<String, Object> compile(File projectDirToBuild, List<File> iarJars, Map<String, Object> options) throws Exception
  {
    Method compileMethod = getMethod("compile", File.class, List.class, String.class, Map.class);
    return (Map<String, Object>) executeInEngineDir(() -> 
      compileMethod.invoke(delegate, projectDirToBuild, iarJars, engineClasspath, options)
    );
  }
  
  @SuppressWarnings("unchecked")
  public Map<String, Object> validate(File projectDirToBuild, List<File> dependentProjects, Map<String, Object> options) throws Exception
  {
    Method validate = getMethod("validate", File.class, List.class, String.class, Map.class);
    return (Map<String, Object>) executeInEngineDir(() -> 
      validate.invoke(delegate, projectDirToBuild, dependentProjects, engineClasspath, options)
    );
  }
  
  @SuppressWarnings("unchecked")
  public Map<String, Object> testCompile(File projectDirToBuild, List<File> iarJars, Map<String, Object> options) throws Exception
  {
    Method compileMethod = getMethod("testCompile", File.class, List.class, String.class, Map.class);
    return (Map<String, Object>) executeInEngineDir(() -> 
      compileMethod.invoke(delegate, projectDirToBuild, iarJars, engineClasspath, options)
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
                      + "You might need to configure another version to work with.");
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
    String WARNINGS_ENABLED = "jdt.warnings.enabled";
    String JDT_SETTINGS_FILE = "jdt.settings.file";
    String JDT_OPTIONS = "jdt.options";
  }
  
  public static interface Result
  {
    String TEST_OUTPUT_DIR = "ivy.project.test.output.dir";
  }

}