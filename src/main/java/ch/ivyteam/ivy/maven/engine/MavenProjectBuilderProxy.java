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
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.maven.plugin.logging.Log;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import ch.ivyteam.ivy.maven.engine.EngineClassLoaderFactory.OsgiDir;

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

  public MavenProjectBuilderProxy(EngineClassLoaderFactory classLoaderFactory, File workspace, File baseDirToBuildIn, Log log) throws Exception
  {
    this.baseDirToBuildIn = baseDirToBuildIn;
    this.log = log;
    
    URLClassLoader ivyEngineClassLoader = classLoaderFactory.createEngineClassLoader(baseDirToBuildIn);
    delegateClass = getOsgiBundledDelegate(ivyEngineClassLoader);
    Constructor<?> constructor = delegateClass.getDeclaredConstructor(File.class);
    
    delegate = executeInEngineDir(() -> constructor.newInstance(workspace));

    List<File> engineJars = EngineClassLoaderFactory.getIvyEngineClassPathFiles(baseDirToBuildIn);
    engineClasspath = getEngineClasspath(engineJars);
  }

  private Class<?> getOsgiBundledDelegate(URLClassLoader ivyEngineClassLoader) throws ClassNotFoundException,
          NoSuchMethodException, Exception, IllegalAccessException, InvocationTargetException
  { 
    Object bundleContext = startEclipseOsgiImpl(ivyEngineClassLoader);
    Object buildBundle = findBundle(bundleContext, "ch.ivyteam.ivy.dataclasses.build");
    return (Class<?>) buildBundle.getClass().getDeclaredMethod("loadClass", String.class)
            .invoke(buildBundle, FQ_DELEGATE_CLASS_NAME);
  }
  
  private Object startEclipseOsgiImpl(URLClassLoader ivyEngineClassLoader) throws ClassNotFoundException,
          NoSuchMethodException, Exception, IllegalAccessException, InvocationTargetException
  {
    Class<?> osgiBooter = ivyEngineClassLoader.loadClass("org.eclipse.core.runtime.adaptor.EclipseStarter");
    Method mainMethod = osgiBooter.getDeclaredMethod("main", String[].class);
    List<String> mainArgs = new ArrayList<>(Arrays.asList("-application", "ch.ivyteam.ivy.server.exec.engine.maven"));
    if (log.isDebugEnabled())
    {
      mainArgs.add("-consoleLog");
    }
    final String[] args = mainArgs.toArray(new String[mainArgs.size()]);
    startWithOsgiProperties(() -> mainMethod.invoke(null, (Object)args));
    Object bundleContext = osgiBooter.getDeclaredMethod("getSystemBundleContext").invoke(null);
    return bundleContext;
  }

  private Object findBundle(Object bundleContext, String symbolicName)
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
    throw new RuntimeException("Failed to resolve bundle with symbolice name '"+symbolicName+"'.");
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
    Method compileMethod = getMethod("compile", File.class, List.class, String.class, Map.class);
    return (Map<String, Object>) executeInEngineDir(() -> 
      compileMethod.invoke(delegate, projectDirToBuild, iarJars, engineClasspath, options)
    );
  }
  
  @SuppressWarnings("unchecked")
  public Map<String, Object> testCompile(File projectDirToBuild, List<File> iarJars, Map<String, String> options) throws Exception
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
                      + "You might need to configer another version to work with.");
    }
  }
  
  private void startWithOsgiProperties(Callable<?> function) throws Exception
  {
    Map<String, String> properties = new LinkedHashMap<>();
    
    properties.put("osgi.framework.useSystemProperties", Boolean.TRUE.toString());
    properties.put("user.dir", baseDirToBuildIn.getAbsolutePath());
    properties.put("osgi.install.area", new File(baseDirToBuildIn, OsgiDir.INSTALL_AREA).getAbsolutePath());
    properties.put("org.osgi.framework.bundle.parent", "framework");
    properties.put("org.osgi.framework.bootdelegation",
            "javax.annotation,ch.ivyteam.ivy.boot.osgi.win,ch.ivyteam.ivy.jaas," // original
            + "org.apache.log4j,org.apache.log4j.helpers,org.apache.log4j.spi,org.apache.log4j.xml," // add log4j
            + "org.slf4j.impl,org.slf4j,org.slf4j.helpers,org.slf4j.spi," // add slf4j
            
            + "javax.management,javax.management.openmbean,javax.xml.parsers," // since oxygen platform
            + "sun.net.www.protocol.http.ntlm,com.sun.xml.internal.ws.util,com.sun.nio.zipfs,org.xml.sax" // since oxygen platform
            );
    if (log.isDebugEnabled())
    {
      log.debug("Configuration OSGi system properties:");
      properties.entrySet().forEach(entry -> log.debug("   " + entry.getKey() + " = " + entry.getValue()));
    }
    
    Map<String, String> oldProperties = setSystemProperties(properties);
    try
    {
      ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("Init Engine Thread").build();
      ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor(threadFactory);
      Future<?> result = singleThreadExecutor.submit(function);
      try
      {
        result.get(60, TimeUnit.SECONDS);
      }
      catch (Exception ex)
      {
        throw new Exception("Could not initialize engine", ex);
      }
    }
    finally
    {
      setSystemProperties(oldProperties);
    }
  }
  
  private Map<String, String> setSystemProperties(Map<String, String> properties)
  {
    Map<String, String> oldProperties = new LinkedHashMap<>();
    for (Map.Entry<String, String> entry : properties.entrySet())
    {
      String key = entry.getKey();
      String value = entry.getValue();
      String oldValue;
      if (value == null)
      {
        oldValue = System.clearProperty(key);
      }
      else
      {
        oldValue = System.setProperty(key, value);
      }
      oldProperties.put(key, oldValue);
    }
    return oldProperties;
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