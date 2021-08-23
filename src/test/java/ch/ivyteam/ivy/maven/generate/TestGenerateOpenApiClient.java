package ch.ivyteam.ivy.maven.generate;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.Rule;
import org.junit.Test;

import ch.ivyteam.ivy.maven.BaseEngineProjectMojoTest;
import ch.ivyteam.ivy.maven.compile.CompileMojoRule;

public class TestGenerateOpenApiClient  extends BaseEngineProjectMojoTest {

  @Rule
  public CompileMojoRule<OpenApiClientGeneratorMojo> generate = new CompileMojoRule<>(OpenApiClientGeneratorMojo.GOAL);

  @Test
  public void clientFromOpenApi() throws MojoExecutionException, MojoFailureException, IOException {
    simpleRestYaml();
    generate.getMojo().execute();
  }

  private void simpleRestYaml() throws IOException {
    try(InputStream is = TestGenerateOpenApiClient.class.getResourceAsStream("rest-clients.yaml")){
      File project = generate.getMojo().project.getBasedir();
      Path restClients = project.toPath().resolve("config").resolve("rest-clients.yaml");
      Files.copy(is, restClients, StandardCopyOption.REPLACE_EXISTING);
    }
  }

}
