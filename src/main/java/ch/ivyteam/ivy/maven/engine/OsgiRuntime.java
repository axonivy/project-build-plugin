package ch.ivyteam.ivy.maven.engine;

import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.nio.file.Path;
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

import org.apache.maven.plugin.logging.Log;

import ch.ivyteam.ivy.maven.engine.EngineClassLoaderFactory.OsgiDir;

/**
 * Isolates the bootstrap of the OSGI framework in reflective manor.
 *
 * @since 7.0
 */
class OsgiRuntime {

  private final Path engineDir;
  private final Log log;

  OsgiRuntime(Path engineDir, Log log) {
    this.engineDir = engineDir;
    this.log = log;
  }

  Object startEclipseOsgiImpl(URLClassLoader ivyEngineClassLoader, int timeoutEngineStartInSeconds)
          throws Exception {
    Class<?> osgiBooter = ivyEngineClassLoader.loadClass("org.eclipse.core.runtime.adaptor.EclipseStarter");
    Method mainMethod = osgiBooter.getDeclaredMethod("main", String[].class);
    List<String> mainArgs = new ArrayList<>(
            Arrays.asList("-application", "ch.ivyteam.ivy.server.exec.engine.maven"));
    if (log.isDebugEnabled()) {
      mainArgs.add("-debug");
    }
    final String[] args = mainArgs.toArray(new String[mainArgs.size()]);
    runThreadWithProperties(() -> mainMethod.invoke(null, (Object) args), timeoutEngineStartInSeconds);
    Object bundleContext = osgiBooter.getDeclaredMethod("getSystemBundleContext").invoke(null);
    return bundleContext;
  }

  void runThreadWithProperties(Callable<?> function, int timeoutEngineStartInSeconds) throws Exception {
    Map<String, String> properties = createOsgiConfigurationProps();
    Map<String, String> oldProperties = setSystemProperties(properties);
    try {
      ThreadFactory threadFactory = new ThreadFactory() {

        @Override
        public Thread newThread(Runnable r) {
          return new Thread(r, "Init Engine Thread");
        }
      };
      ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor(threadFactory);
      Future<?> result = singleThreadExecutor.submit(function);
      try {
        TimeUnit timeUnit = TimeUnit.SECONDS;
        if (log.isDebugEnabled()) {
          log.debug("Waiting " + timeoutEngineStartInSeconds + " " + timeUnit.name().toLowerCase()
                  + " on engine start");
        }
        result.get(timeoutEngineStartInSeconds, timeUnit);
      } catch (Exception ex) {
        throw new Exception("Could not initialize engine", ex);
      }
    } finally {
      setSystemProperties(oldProperties);
    }
  }

  private static Map<String, String> setSystemProperties(Map<String, String> properties) {
    Map<String, String> oldProperties = new LinkedHashMap<>();
    for (Map.Entry<String, String> entry : properties.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();
      String oldValue;
      if (value == null) {
        oldValue = System.clearProperty(key);
      } else {
        oldValue = System.setProperty(key, value);
      }
      oldProperties.put(key, oldValue);
    }
    return oldProperties;
  }

  private Map<String, String> createOsgiConfigurationProps() {
    Map<String, String> properties = new LinkedHashMap<>();

    properties.put("osgi.framework.useSystemProperties", Boolean.TRUE.toString());
    properties.put("user.dir", engineDir.toAbsolutePath().toString());
    var osgiDir = engineDir.resolve(OsgiDir.INSTALL_AREA);
    properties.put("osgi.install.area", osgiDir.toAbsolutePath().toString());
    properties.put("org.osgi.framework.bundle.parent", "framework");
    properties.put("org.osgi.framework.bootdelegation",
            "javax.annotation,ch.ivyteam.ivy.boot.osgi.win,ch.ivyteam.ivy.jaas," // original
                    + "org.apache.log4j,org.apache.log4j.helpers,org.apache.log4j.spi,org.apache.log4j.xml," // add
                                                                                                             // log4j
                    + "org.slf4j.impl,org.slf4j,org.slf4j.helpers,org.slf4j.spi," // add
                                                                                  // slf4j
                    + "javax.net.ssl," // validate openApi

                    // jdt compiler
                    + "javax.lang.model,"
                    + "javax.annotation.processing,"
                    + "javax.tools,"
                    + "javax.lang.model.util,"
                    + "javax.lang.model.element,"
                    + "javax.lang.model.type,"

                    // oxygen platform
                    + "javax.management,javax.management.openmbean,javax.xml.parsers,"
                    + "sun.net.www.protocol.http.ntlm,com.sun.xml.internal.ws.util,com.sun.nio.zipfs,org.xml.sax,"
                    + "org.w3c.dom,"

                    // for java 11
                    + "javax.xml,javax.xml.datatype,javax.xml.namespace,javax.xml.transform,javax.xml.transform.dom,javax.xml.transform.sax,javax.xml.transform.stream,javax.xml.validation,javax.xml.xpath,"
                    + "org.xml.sax.ext,org.xml.sax.helpers,"
                    + "javax.xml.stream,javax.xml.stream.events,javax.xml.stream.util");

    if (log.isDebugEnabled()) {
      log.debug("Configuration OSGi system properties:");
      properties.entrySet().forEach(entry -> log.debug("   " + entry.getKey() + " = " + entry.getValue()));
    }
    return properties;
  }

}