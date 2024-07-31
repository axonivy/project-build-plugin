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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.maven.plugin.logging.Log;

public record EngineVmOptions(String additionalClasspath, String additionalVmOptions, List<String> additionalVmArgs) {

  @Deprecated(since = "12.0.0", forRemoval = true)
  public String additionalVmOptions() {
    return additionalVmOptions;
  }

  public List<String> additionalVmArgs(Log log) {
    var args = new ArrayList<String>();
    if (additionalVmOptions != null && !additionalVmOptions.isEmpty()) {
      log.warn("additionalVmOptions set which is deprecated. Use additionalVmArgs.");
      args.addAll(Stream.of(additionalVmOptions.split(" ")).toList());
    }
    if (additionalVmArgs != null) {
      args.addAll(additionalVmArgs);
    }
    return args;
  }
}
