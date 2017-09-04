package ch.ivyteam.ivy.maven.util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class UrlRedirectionResolver {

	public static URL followRedirections(URL url) throws IOException
	{
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		while (isRedirected(connection.getResponseCode())) {
			String newUrl = connection.getHeaderField("Location");
			url = new URL(newUrl);
			closeHttpUrlConnectionSilently(connection);
			connection = (HttpURLConnection) url.openConnection();
		}
		return url;
	}

	private static boolean isRedirected(int httpStatusCode)
	{
		return httpStatusCode == HttpURLConnection.HTTP_MOVED_TEMP 
				|| httpStatusCode == HttpURLConnection.HTTP_MOVED_PERM
				|| httpStatusCode == HttpURLConnection.HTTP_SEE_OTHER
				|| httpStatusCode == 307
				|| httpStatusCode == 308;
	}

	private static void closeHttpUrlConnectionSilently(HttpURLConnection connection)
	{
		try
		{
			if (connection != null && connection.getInputStream() != null)
			{
				connection.getInputStream().close();
			}
		}
		catch (IOException e)
		{
			// silently
		}
	}

}
