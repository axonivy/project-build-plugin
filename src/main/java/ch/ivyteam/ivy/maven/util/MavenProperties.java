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

package ch.ivyteam.ivy.maven.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

public class MavenProperties {
  private final MavenProject project;
  private final Log log;

  public MavenProperties(MavenProject project, Log log) {
    this.project = project;
    this.log = log;
  }

  public void setMavenProperty(String key, String value) {
    set(key, value);
  }

  public void set(String key, String value) {
    log.debug("share property '" + key + "' with value '" + StringUtils.abbreviate(value, 500) + "'");
    project.getProperties().put(key, value);
  }

  @SuppressWarnings("unchecked")
  public <T extends Object> T get(String key) {
    return (T) project.getProperties().get(key);
  }

  public void append(String key, String value) {
    String current = get(key);
    if (StringUtils.isNotEmpty(current)) {
      value = current += value;
    }
    set(key, value);
  }

}
