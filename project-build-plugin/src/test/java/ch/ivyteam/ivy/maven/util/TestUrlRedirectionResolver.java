package ch.ivyteam.ivy.maven.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.junit.Test;

import ch.ivyteam.ivy.maven.engine.download.UrlRedirectionResolver;

/**
 * Url: developer.axonivy.com/download/maven.html must be available for http and
 * https without any redirections. Old versions of project-build-plugin relies
 * on that behavior.
 */
public class TestUrlRedirectionResolver {
  @Test
  public void no_redirections() throws IOException {
    assertRedirection("http://developer.axonivy.com/download/maven.html",
            "http://developer.axonivy.com/download/maven.html");
    assertRedirection("https://developer.axonivy.com/download/maven.html",
            "https://developer.axonivy.com/download/maven.html");
    assertRedirection("https://developer.axonivy.com/download", "https://developer.axonivy.com/download");
  }

  @Test
  public void redirections() throws IOException {
    assertRedirection("http://answers.axonivy.com/", "http://answers.axonivy.com/",
            "https://answers.axonivy.com/");
  }

  private static void assertRedirection(String initUrl, String... openendUrls) throws IOException {
    URL url = new URL(initUrl);
    UrlRedirectionResolver resolver = new UrlRedirectionResolver();
    InputStream stream = resolver.followRedirections(url);
    stream.close();

    for (int i = 0; i < openendUrls.length; i++) {
      assertThat(resolver.getOpenedUrls().get(i).toExternalForm()).isEqualTo(openendUrls[i]);
    }
  }

}
