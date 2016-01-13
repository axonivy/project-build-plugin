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

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * A MOJO that relies on an unpacked ivy engine.
 * 
 * @author Reguel Wermelinger
 * @since 6.0.0
 */
public abstract class AbstractEngineMojo extends AbstractMojo
{
  /** keep synch with pom.xml -> reporting->maven-plugin-plugin->requirements */
  protected static final String MINIMAL_COMPATIBLE_VERSION = "6.1.0";
  protected static final String DEFAULT_VERSION = "6.1.0";
  
  /**
   * Location where an unpacked (may pre-configured) ivy Engine in the {@link #ivyVersion required version} exists. 
   * <p>If parameter is not set it will be a sub-directory of the {@link #engineCacheDirectory}.
   * 
   * <p>If the Engine does not yet exist, it can be automatically downloaded. 
   */
  @Parameter(property="engineDirectory")
  File engineDirectory;
  
  /**
   * Location where ivy engines in required version can be extracted to. 
   * <p>If the Engine does not yet exist, it can be automatically downloaded. 
   */
  @Parameter(defaultValue = "${settings.localRepository}/.cache/ivy", property="engineCacheDirectory")
  protected File engineCacheDirectory;
  
  /**
   * The ivy Engine version that is used. Must be equal or higher than {@value #MINIMAL_COMPATIBLE_VERSION}
   */
  @Parameter(defaultValue = DEFAULT_VERSION, required = true, property="ivyVersion")
  protected String ivyVersion;

  public AbstractEngineMojo()
  {
    super();
  }
  
  protected final File getEngineDirectory()
  {
    if (engineDirectory == null)
    {
      return new File(engineCacheDirectory, ivyVersion);
    }
    return engineDirectory;
  }

}