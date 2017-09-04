package ch.ivyteam.ivy.maven.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URL;

import org.junit.Test;

public class TestUrlRedirectionResolver {

	/**
	 * Url: developer.axonivy.com/download/maven.html must be available for http and
	 * https without any redirections. Old versions of project-build-plugin relies
	 * on that behavior.
	 */
	@Test
	public void no_redirections() throws IOException
	{
		assertRedirection("http://developer.axonivy.com/download/maven.html", "http://developer.axonivy.com/download/maven.html");
		assertRedirection("https://developer.axonivy.com/download/maven.html", "https://developer.axonivy.com/download/maven.html");
	}

	@Test
	public void redirections() throws IOException
	{
		assertRedirection("http://developer.axonivy.com/download/", "https://developer.axonivy.com/download/");
		assertRedirection("https://developer.axonivy.com/download/", "https://developer.axonivy.com/download/");
	}

	private static void assertRedirection(String initUrl, String redirectUrl) throws IOException
	{
		URL url = new URL(initUrl);
		URL redirectedUrl = UrlRedirectionResolver.followRedirections(url);
		assertThat(redirectedUrl.toExternalForm()).isEqualTo(redirectUrl);
	}

}
