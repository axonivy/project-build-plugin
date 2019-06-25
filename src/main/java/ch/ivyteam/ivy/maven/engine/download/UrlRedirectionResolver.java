package ch.ivyteam.ivy.maven.engine.download;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class UrlRedirectionResolver
{
  public List<URL> openedUrls = new ArrayList<>();

  public InputStream followRedirections(URL url) throws IOException
  {
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    openedUrls.add(url);
    while (isRedirected(connection.getResponseCode()))
    {
      String newUrl = connection.getHeaderField("Location");
      url = new URL(newUrl);
      closeHttpUrlConnectionSilently(connection);
      connection = (HttpURLConnection) url.openConnection();
      openedUrls.add(url);
    }
    return connection.getInputStream();
  }

  public List<URL> getOpenedUrls()
  {
    return openedUrls;
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
