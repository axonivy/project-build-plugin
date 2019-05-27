package ch.ivyteam.ivy.maven.engine.deploy.http;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.settings.Server;

public class HttpDeployer
{
  private String serverUrl;
  private String targetApplication;
  private File deployFile;
  private File deploymentOptions;
  private Server server;

  public HttpDeployer(Server server, String serverUrl, String targetApplication, File deployFile, File deploymentOptions)
  {
    this.server = server;
    this.serverUrl = serverUrl;
    this.targetApplication = targetApplication;
    this.deployFile = deployFile;
    this.deploymentOptions = deploymentOptions;
  }

  public void deploy(Log log)
  {
    String url = serverUrl + "/api/system/apps/" + targetApplication;
    
    HttpPost httpPost = new HttpPost(url);
    httpPost.addHeader("X-Requested-By", "maven-build-plugin");
    HttpEntity resEntity = null;
    
    try (CloseableHttpClient client = HttpClientBuilder.create().build())
    {
      httpPost.setEntity(getRequestData(deploymentOptions));
      CloseableHttpResponse response = client.execute(httpPost, getRequestContext(url));
      
      resEntity = response.getEntity();
      if (resEntity != null) {
        log.info(EntityUtils.toString(resEntity));
      }
      int status = response.getStatusLine().getStatusCode();
      if (status != HttpStatus.SC_OK) {
        log.error("Upload error! (" + status + ")");
      }
      EntityUtils.consume(resEntity);
    }
    catch (IOException | URISyntaxException ex)
    {
      log.error(ex.getMessage());
    }
    finally 
    {
      removeTemporaryDeploymentOptionsFile();
    }
  }
  
  private void removeTemporaryDeploymentOptionsFile()
  {
    FileUtils.deleteQuietly(deploymentOptions);
  }
  
  private HttpEntity getRequestData(File resolvedOptionsFile) throws IOException
  {
    MultipartEntityBuilder builder = MultipartEntityBuilder.create();
    builder.addPart("fileToDeploy", new FileBody(deployFile));
    if (resolvedOptionsFile != null)
    {
      builder.addTextBody("deploymentOptions", FileUtils.readFileToString(resolvedOptionsFile, StandardCharsets.UTF_8));
    }
    return builder.build();
  }

  private HttpClientContext getRequestContext(String url) throws URISyntaxException
  {
    String username = "admin";
    String password = "admin";
    if (server != null)
    {
      username = server.getUsername();
      password = server.getPassword();
    }
    HttpHost httpHost = URIUtils.extractHost(new URI(url));
    CredentialsProvider credsProvider = new BasicCredentialsProvider();
    credsProvider.setCredentials(AuthScope.ANY,
        new UsernamePasswordCredentials(username, password));
    AuthCache authCache = new BasicAuthCache();
    authCache.put(httpHost, new BasicScheme());
    HttpClientContext context = HttpClientContext.create();
    context.setCredentialsProvider(credsProvider);
    context.setAuthCache(authCache);
    return context;
  }
}
