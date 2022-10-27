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

package ch.ivyteam.ivy.maven.compile;

import java.io.File;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugins.annotations.Parameter;

import ch.ivyteam.ivy.maven.engine.MavenProjectBuilderProxy;
import ch.ivyteam.ivy.maven.util.MavenRuntime;

public abstract class AbstractProjectCompileMojo extends AbstractEngineInstanceMojo {
  /**
   * Specifies the default encoding for all source files. By default this is the
   * charset of the JVM according to {@link Charset#defaultCharset()}. You may
   * set it to another value like 'UTF-8'.
   * @since 6.3.1
   */
  @Parameter(property = "ivy.compiler.encoding")
  private String encoding;

  /**
   * Set to <code>false</code> to disable compilation warnings.
   * @since 8.0.3
   */
  @Parameter(property = "ivy.compiler.warnings", defaultValue = "true")
  boolean compilerWarnings;

  /**
   * Define a compiler settings file to configure compilation warnings. Such
   * file can be created in the Designer: <i>Window - Preferences - Java -
   * Compiler - Errors/Warnings</i>, the corresponding file can be found in:
   * <i>designer-workspace/.metadata/.plugins/org.eclipse.core.runtime/.settings/org.eclipse.jdt.core.prefs</i>
   * <br>
   * If left empty the plugin will try to load the project specific settings
   * file <i>project/.settings/org.eclipse.jdt.core.prefs</i> <br>
   * These settings are only active when
   * {@link AbstractProjectCompileMojo#compilerWarnings} is set to
   * <code>true</code>.
   * @since 8.0.3
   */
  @Parameter(property = "ivy.compiler.settings", defaultValue = ".settings/org.eclipse.jdt.core.prefs")
  File compilerSettings;

  /**
   * Define compiler options. <br>
   * {@literal
   *   <compilerOptions>
   *      <arg>-help<arg>
   *   </compilerOptions>
   * }
   * @since 8.0.3
   */
  @Parameter
  List<String> compilerOptions;

  protected Map<String, Object> getOptions() {
    Map<String, Object> options = new HashMap<>();
    options.put(MavenProjectBuilderProxy.Options.TEST_SOURCE_DIR,
            project.getBuild().getTestSourceDirectory());
    options.put(MavenProjectBuilderProxy.Options.COMPILE_CLASSPATH, getDependencyClasspath());
    options.put(MavenProjectBuilderProxy.Options.SOURCE_ENCODING, encoding);
    options.put(MavenProjectBuilderProxy.Options.WARNINGS_ENABLED, Boolean.toString(compilerWarnings));
    options.put(MavenProjectBuilderProxy.Options.JDT_SETTINGS_FILE, getCompilerSettings());
    options.put(MavenProjectBuilderProxy.Options.JDT_OPTIONS, compilerOptions);
    return options;
  }

  private String getDependencyClasspath() {
    return StringUtils.join(getDependencies("jar").stream()
            .map(jar -> jar.getAbsolutePath())
            .collect(Collectors.toList()), File.pathSeparatorChar);
  }

  private File getCompilerSettings() {
    if (compilerSettings.exists()) {
      return compilerSettings;
    } else if (compilerWarnings) {
      getLog().warn("Could not locate compiler settings file: " + compilerSettings
              + " continuing with default compiler settings");
    }
    return null;
  }

  protected final List<File> getDependencies(String type) {
    return MavenRuntime.getDependencies(project, type);
  }

}