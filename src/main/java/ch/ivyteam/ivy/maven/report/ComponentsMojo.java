package ch.ivyteam.ivy.maven.report;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;

import ch.ivyteam.ivy.maven.project.ProjectContextFactory;
import ch.ivyteam.ivy.process.model.Process;
import ch.ivyteam.ivy.process.model.ProcessKind;
import ch.ivyteam.ivy.process.model.element.event.start.CallSubStart;

/**
 * Reports re-usable components of a project to JSON.
 * @since 14.0.2
 */
@Mojo(name = ComponentsMojo.GOAL, requiresProject = true, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE)
public class ComponentsMojo extends AbstractMojo {
  public static final String GOAL = "report-components";

  @Parameter(property = "project", required = true, readonly = true)
  MavenProject mavenProject;

  @Parameter(defaultValue = "${session}", readonly = true)
  MavenSession session;

  @Inject
  ProjectBuilder projectBuilder;

  @Parameter(property = "ivy.report.skipComponents", defaultValue = "true")
  Boolean skipComponentsReport;

  @Parameter(property = "ivy.report.componentReportDir", defaultValue = "${project.build.directory}/components")
  Path componentReportDir;

  @Parameter(property = "ivy.report.includeTags", defaultValue = "CONNECTOR")
  List<String> includeTags;

  Function<String, Type> loader = it -> {
    var projectContext = new ProjectContextFactory(mavenProject, session, projectBuilder, getLog());
    return new ProjectTypeLoader(projectContext).loadType(it);
  };

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (skipComponentsReport) {
      getLog().info("Skipping components report generation");
      return;
    }

    report();
  }

  private void report() {
    var start = System.currentTimeMillis();
    var processes = new ProcessReader(mavenProject.getBasedir().toPath()).processes();
    var callables = callSubStarts(processes);

    JsonReporter processReport = new JsonReporter(loader);
    processReport.callables(callables);
    var json = processReport.finish();
    try {
      Files.createDirectories(componentReportDir);
      Path process = componentReportDir.resolve("process.json");
      Files.writeString(process, json.toPrettyString());
      var end = System.currentTimeMillis();
      getLog().info("Components reported at: " + process +" in " + (end - start) + " ms");
    } catch (IOException ex) {
      throw new RuntimeException("Failed to write component report", ex);
    }
  }

  private Stream<CallSubStart> callSubStarts(List<Process> processes) {
    return processes.stream()
        .filter(p -> p.getKind() == ProcessKind.CALLABLE_SUB)
        .flatMap(sub -> sub.search().type(CallSubStart.class)
            .and(start -> hasTag(start.getTags().toList(), includeTags))
            .findDeep().stream());
  }

  private static boolean hasTag(List<String> tags, List<String> includeTags) {
    return tags.stream().anyMatch(includeTags::contains);
  }

}
