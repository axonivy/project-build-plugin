package ch.ivyteam.ivy.maven;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Date;

import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.sonatype.plexus.build.incremental.BuildContext;

@Mojo(name = "build-data-class", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class BuildDataClassMojo extends AbstractMojo {

  @Component
  private BuildContext buildContext;

  @Parameter(property = "project", required = true, readonly = true)
  private MavenProject project;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    var scanner = buildContext.newScanner(project.getBasedir());
    scanner.setIncludes(new String[] { "**/*.ivyClass" });
    scanner.scan();
    String[] includedFiles = scanner.getIncludedFiles();
    if (includedFiles == null) {
      return;
    }
    var sourceDirectory = createAndGetSourceDirectory();
    for (String includedFile : includedFiles) {
      File dataClass = new File(scanner.getBasedir(), includedFile);
      if (!buildContext.isIncremental() || buildContext.hasDelta(dataClass)) {
        var dataClassName = FileNameUtils.getBaseName(dataClass.toPath());
        File javaFile = new File(sourceDirectory, dataClassName + ".java");
        try (OutputStream os = buildContext.newFileOutputStream(javaFile)) {
          var content = """
              public class %s {
                String now = "%s";
              }
                """.formatted(dataClassName, new Date().toString());
          os.write(content.getBytes(Charset.forName("UTF-8")));
        } catch (Exception ex) {
          throw new RuntimeException(ex);
        }
      }
    }
  }

  private File createAndGetSourceDirectory() {
    try {
      var sourceDirectory = new File(project.getBasedir(),"src_dataClasses");
      Files.createDirectories(sourceDirectory.toPath());
      return sourceDirectory;
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }
}
