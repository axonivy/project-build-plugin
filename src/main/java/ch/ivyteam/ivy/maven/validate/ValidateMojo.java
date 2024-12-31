package ch.ivyteam.ivy.maven.validate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Validates that only one version of the project-build-plugin is active in one maven reactor.
 *
 * @since 12.0.1
 */
@Mojo(name = ValidateMojo.GOAL, requiresProject = true)
public class ValidateMojo extends AbstractMojo {

  public static final String GOAL = "validate";
  protected static final String PLUGIN_GROUPID = "com.axonivy.ivy.ci";
  protected static final String PLUGIN_ARTIFACTID = "project-build-plugin";

  @Parameter(defaultValue = "${session}", readonly = true, required = true)
  private MavenSession session;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    validateConsistentPluginVersion(session.getAllProjects());
  }

  void validateConsistentPluginVersion(List<MavenProject> projects) throws MojoExecutionException {
    var versionToProjectsMap = new HashMap<String, Set<MavenProject>>();
    for (var project : projects) {
      findProjectBuildPlugin(project.getBuild().getPlugins()).ifPresent(plugin -> {
        var version = plugin.getVersion();
        getLog().debug(PLUGIN_GROUPID + ":" + plugin.getArtifactId() + ":" + version + " configured in " + project);
        var projectSet = versionToProjectsMap.computeIfAbsent(version, v -> new LinkedHashSet<>());
        projectSet.add(project);
      });
    }
    if (versionToProjectsMap.size() > 1) {
      var versions = new ArrayList<>(versionToProjectsMap.keySet());
      Collections.sort(versions);
      var error = "Several versions of project-build-plugins are configured " + versions + ":\n";
      error += versions.stream().map(v -> versionProjects(versionToProjectsMap, v)).collect(Collectors.joining("\n"));
      getLog().error(error);
      throw new MojoExecutionException("All project-build-plugins configured in one reactor must use the same version");
    }
  }

  private Optional<Plugin> findProjectBuildPlugin(List<Plugin> plugins) {
    return plugins.stream()
        .filter(p -> PLUGIN_GROUPID.equals(p.getGroupId()) && PLUGIN_ARTIFACTID.equals(p.getArtifactId()))
        .filter(p -> p.getVersion() != null) // Skip plug-ins that do not have a version
        .findFirst();
  }

  private String versionProjects(Map<String, Set<MavenProject>> versionToProjectsMap, String version) {
    return version + " -> [" +
        versionToProjectsMap.get(version).stream()
            .map(MavenProject::getArtifactId)
            .collect(Collectors.joining(", "))
        +
        "]";
  }
}
