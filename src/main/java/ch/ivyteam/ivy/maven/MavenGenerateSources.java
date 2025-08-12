package ch.ivyteam.ivy.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = MavenGenerateSources.GOAL)
public class MavenGenerateSources extends AbstractMojo {

  public static final String GOAL = "maven-generate-sources";

  @Override
  public void execute() {
    // placeholder to make vscode integration m2e integration work.
  }
}
