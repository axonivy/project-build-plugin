/*
 * Copyright (C) 2021 Axon Ivy AG
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

package ch.ivyteam.ivy.maven.generate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.compiler.manager.CompilerManager;
import org.sonatype.plexus.build.incremental.BuildContext;

import ch.ivyteam.ivy.maven.AbstractEngineMojo;
import ch.ivyteam.ivy.maven.engine.EngineClassLoaderFactory.MavenContext;
import ch.ivyteam.ivy.webservice.exec.cxf.codegen.CxfClientGenerator;
import ch.ivyteam.ivy.webservice.exec.cxf.codegen.MavenCxfCompiler;

/**
 * Generates CXF clients for SOAP webservices.
 *
 * @since 9.3
 */
@Mojo(name = CxfClientGeneratorMojo.GOAL, requiresDependencyResolution=ResolutionScope.COMPILE)
public class CxfClientGeneratorMojo extends AbstractEngineMojo
{
  public static final String GOAL = "generate-cxf-client";

  @Component
  private CompilerManager compilerManager;

  @Component
  private BuildContext buildContext;

  @Component
  private RepositorySystem repository;

  @Parameter(defaultValue = "${localRepository}")
  private ArtifactRepository localRepository;

  @Parameter(property = "project", required = true, readonly = true)
  private MavenProject project;

  /**
   * the WSDL service descriptor
   */
  @Parameter(property="ivy.soap.client.wsdl")
  String wsdlUri;

  @Parameter(property="ivy.soap.client.id")
  String clientId;

  @Override
  public void execute() throws MojoExecutionException ,org.apache.maven.plugin.MojoFailureException
  {
    MavenCxfCompiler compiler = new MavenCxfCompiler(compilerManager, getLog());
    List<File> jars = getCxfIvyJars();
    jars.stream().forEach(jar -> {
      compiler.addClasspathEntry(jar.getAbsolutePath());
    });

    try {
      new CxfClientGenerator()
        .compiler(compiler)
        .generate(wsdlUri, this::integrate);
    } catch (Exception ex) {
      throw new MojoFailureException("Failed to compile CXF client "+clientId, ex);
    }
  }

  private void integrate(File tmpJar) {
    Path target = project.getBasedir().toPath().resolve("lib_ws").resolve("client");
    try {
      Files.createDirectories(target);
      Path clientJar = target.resolve("cxfClient_"+clientId+".jar");
      Files.move(tmpJar.toPath(), clientJar, StandardCopyOption.REPLACE_EXISTING);
      getLog().info("Created CXF client: "+clientJar);
      buildContext.refresh(clientJar.toFile());
    } catch (IOException ex) {
      getLog().error("Failed to integrate CXF client "+tmpJar, ex);
    }
  }

  private List<File> getCxfIvyJars() throws MojoExecutionException {
    MavenContext repoContext = new MavenContext(repository, localRepository, project, getLog());
    // TODO: can resolve from plugin context? though not a project dependency?
    File lang3 = repoContext.getJar("org.apache.commons", "commons-lang3", "3.3.2");
    File annotate = repoContext.getJar("javax.annotation", "javax.annotation-api", "1.3.2");
    File jaxb = repoContext.getJar("javax.xml.bind", "jaxb-api", "2.3.1");
    File jaxws = repoContext.getJar("javax.xml.ws", "jaxws-api", "2.3.1");
    File jeeWs = repoContext.getJar("javax.jws", "javax.jws-api", "1.1");

    File engine = identifyAndGetEngineDirectory(); // TODO give me a shortcut when running in Designer?
    File enginePlugins = new File(engine, "system/plugins");
    File scriptingObj = getEngineJar(enginePlugins, "ch.ivyteam.ivy.scripting.objects").map(Path::toFile).orElseThrow();
    File ivyapi = getEngineJar(enginePlugins, "ch.ivyteam.util.api").map(Path::toFile).orElseThrow();

    List<File> jars = List.of(lang3, annotate, jaxb, jaxws, jeeWs, scriptingObj, ivyapi);
    return jars;
  }

  private Optional<Path> getEngineJar(File enginePlugins, String jarNamePrefix) throws MojoExecutionException {
    try(var stream = Files.list(enginePlugins.toPath())) {
      return stream.filter(jar -> jar.getFileName().toString().startsWith(jarNamePrefix)).findAny();
    } catch (IOException ex) {
      throw new MojoExecutionException("Failed to get ivyEngine jar with prefix "+jarNamePrefix, ex);
    }
  }
}
