/*
 * Copyright (C) 2021 Axon Ivy AG
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package ch.ivyteam.ivy.maven.engine;

import java.io.File;
import java.io.IOException;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/**
 * Factory that provides an {@link URLClassLoader} for ivy Engine class access.
 * This makes invocation of engine parts possible without starting a new java
 * process.
 *
 * @author Reguel Wermelinger
 * @since 25.09.2014
 */
public class EngineClassLoaderFactory {
  public interface OsgiDir {
    String INSTALL_AREA = "system";
    String PLUGINS = INSTALL_AREA + "/plugins";
    String LIB_BOOT = "lib/boot";
  }

  private static final List<String> ENGINE_LIB_DIRECTORIES = Arrays.asList(
      OsgiDir.INSTALL_AREA + "/" + OsgiDir.LIB_BOOT,
      OsgiDir.PLUGINS,
      OsgiDir.INSTALL_AREA + "/configuration/org.eclipse.osgi", // unpacked
                                                                // jars from
                                                                // OSGI
                                                                // bundles
      "webapps" + File.separator + "ivy" + File.separator + "WEB-INF" + File.separator + "lib");

  public static List<Path> getOsgiBootstrapClasspath(Path engineDirectory) {
    if (engineDirectory == null || !Files.isDirectory(engineDirectory)) {
      throw new RuntimeException("The engineDirectory is missing: " + engineDirectory);
    }
    List<Path> classPathPaths = new ArrayList<>();
    var libBoot = engineDirectory.resolve(OsgiDir.INSTALL_AREA).resolve(OsgiDir.LIB_BOOT);
    addToClassPath(classPathPaths, libBoot, p -> p.getFileName().toString().endsWith(".jar"));
    return classPathPaths;
  }

  private static void addToClassPath(List<Path> classPathPaths, Path dir, Predicate<Path> filter) {
    if (Files.isDirectory(dir)) {
      try (var stream = Files.list(dir)) {
        var files = stream
            .filter(filter)
            .toList();
        classPathPaths.addAll(files);
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
    }
  }

  public static List<Path> getIvyEngineClassPathFiles(Path engineDirectory) {
    if (engineDirectory == null) {
      return List.of();
    }

    var classPathFiles = new ArrayList<Path>();
    for (String libDirPath : ENGINE_LIB_DIRECTORIES) {
      var jarDir = engineDirectory.resolve(libDirPath);
      if (!Files.isDirectory(jarDir)) {
        continue;
      }
      try (var walker = Files.walk(jarDir)) {
        var jars = walker
            .filter(p -> p.getFileName().toString().endsWith(".jar"))
            .toList();
        classPathFiles.addAll(jars);
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
    }
    return classPathFiles;
  }
}
