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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.DefaultArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.plugin.internal.DefaultLegacySupport;

import ch.ivyteam.ivy.maven.BaseEngineProjectMojoTest;
import ch.ivyteam.ivy.maven.BaseEngineProjectMojoTest.EngineMojoRule;

@SuppressWarnings("deprecation")
public class LocalRepoMojoRule<T extends AbstractEngineInstanceMojo> extends EngineMojoRule<T> {
  public LocalRepoMojoRule(String mojoName) {
    super(mojoName);
  }

  @Override
  protected void before() throws Throwable {
    super.before();
    configureMojo(getMojo());
  }

  public void configureMojo(AbstractEngineInstanceMojo newMojo) throws IllegalAccessException {
    newMojo.localRepository = provideLocalRepository();
  }

  /**
   * maven-plugin-testing-harness can not inject local repositories (though the
   * real runtime supports it). and the default stubs have no sufficient
   * implementation of getPath():
   * @see "http://maven.apache.org/plugin-testing/maven-plugin-testing-harness/examples/repositories.html"
   */
  private ArtifactRepository provideLocalRepository() throws IllegalAccessException {
    DefaultArtifactRepositoryFactory factory = new DefaultArtifactRepositoryFactory();
    setVariableValueToObject(factory, "factory",
        new org.apache.maven.repository.legacy.repository.DefaultArtifactRepositoryFactory());

    LegacySupport legacySupport = new DefaultLegacySupport();
    setVariableValueToObject(factory, "legacySupport", legacySupport);

    ArtifactRepository localRepository = factory.createArtifactRepository("local", "http://localhost",
        new DefaultRepositoryLayout(), new ArtifactRepositoryPolicy(), new ArtifactRepositoryPolicy());

    setVariableValueToObject(localRepository, "basedir", BaseEngineProjectMojoTest.LOCAL_REPOSITORY);

    return localRepository;
  }
}
