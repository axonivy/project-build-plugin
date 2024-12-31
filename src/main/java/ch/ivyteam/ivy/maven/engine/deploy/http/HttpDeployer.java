package ch.ivyteam.ivy.maven.engine.deploy.http;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;

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
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.settings.Server;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcherException;

public class HttpDeployer {
  private static final String DEPLOY_URI = "/system/api/apps/";
  private final String serverUrl;
  private final String targetApplication;
  private final Path deployFile;
  private final Path deploymentOptions;
  private final Server server;
  private final SecDispatcher secDispatcher;

  public HttpDeployer(SecDispatcher secDispatcher, Server server, String serverUrl, String targetApplication,
      Path deployFile, Path deploymentOptions) {
    this.secDispatcher = secDispatcher;
    this.server = server;
    this.serverUrl = serverUrl;
    this.targetApplication = targetApplication;
    this.deployFile = deployFile;
    this.deploymentOptions = deploymentOptions;
  }

  public void deploy(Log log) throws MojoExecutionException {
    try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
      executeRequest(log, client);
    } catch (IOException ex) {
      throw new MojoExecutionException("Failed to build request body", ex);
    } catch (URISyntaxException ex) {
      throw new MojoExecutionException("Failed to build http credentials context", ex);
    }
  }

  private void executeRequest(Log log, CloseableHttpClient client)
      throws IOException, URISyntaxException, MojoExecutionException {
    String url = serverUrl + DEPLOY_URI + targetApplication;
    HttpPost httpPost = new HttpPost(url);
    httpPost.addHeader("X-Requested-By", "maven-build-plugin");
    httpPost.setEntity(getRequestData(deploymentOptions));

    HttpEntity resultEntity = null;
    log.info("Uploading file " + deployFile + " to " + url);
    try (CloseableHttpResponse response = client.execute(httpPost, getRequestContext(url))) {
      resultEntity = response.getEntity();
      String deploymentLog = readDeploymentLog(resultEntity);
      int status = response.getStatusLine().getStatusCode();
      if (status != HttpStatus.SC_OK) {
        log.error(deploymentLog);
        throw new MojoExecutionException("Deployment of file '" + deployFile.getFileName() + "' to engine failed (Status: " + status + ")");
      }
      log.debug(deploymentLog);
      log.info("Deployment finished");
    } finally {
      EntityUtils.consume(resultEntity);
    }
  }

  private String readDeploymentLog(HttpEntity resultEntity) throws IOException {
    if (resultEntity == null) {
      return "";
    }
    return EntityUtils.toString(resultEntity);
  }

  private HttpEntity getRequestData(Path resolvedOptionsFile) {
    MultipartEntityBuilder builder = MultipartEntityBuilder.create();
    builder.addPart("fileToDeploy", new FileBody(deployFile.toFile()));
    if (resolvedOptionsFile != null) {
      builder.addPart("deploymentOptions", new FileBody(resolvedOptionsFile.toFile(), ContentType.TEXT_PLAIN));
    }
    return builder.build();
  }

  private HttpClientContext getRequestContext(String url) throws URISyntaxException, MojoExecutionException {
    String username = "admin";
    String password = "admin";
    if (server != null) {
      username = server.getUsername();
      try {
        password = secDispatcher.decrypt(server.getPassword());
      } catch (SecDispatcherException ex) {
        throw new MojoExecutionException("Could not decrypt maven password", ex);
      }
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
