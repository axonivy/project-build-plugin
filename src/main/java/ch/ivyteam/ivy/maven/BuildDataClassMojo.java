package ch.ivyteam.ivy.maven;

import java.io.File;
import java.io.OutputStream;
import java.nio.charset.Charset;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.project.MavenProject;
import org.sonatype.plexus.build.incremental.BuildContext;

@Mojo(name = "build-data-class", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true)
public class BuildDataClassMojo extends AbstractMojo {

  @Component
  private BuildContext buildContext;

  @Parameter(property = "project", required = true, readonly = true)
  MavenProject project;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    File file = new File(project.getBasedir(), "blabla.txt");
    try (OutputStream os = buildContext.newFileOutputStream(file)) {
      os.write("aasdasdasd".getBytes(Charset.forName("UTF-8")));
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }
}
