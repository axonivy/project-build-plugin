package ch.ivyteam.ivy.webservice.exec.cxf.codegen;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.util.Compiler;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.wsdlto.WSDLToJava;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.HTTPConduitConfigurer;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ivyteam.ivy.webservice.exec.cxf.codegen.binding.IvyGeneratorBindings;
import ch.ivyteam.ivy.webservice.exec.cxf.codegen.fix.FixCXFSchemaLocation;

/**
 * Plain CXF client jar generator without any Eclipse or Ivy API involved.
 * @author rew
 * @since 7.1.0
 */
public class CxfClientGenerator {

  private static final Logger LOGGER = LoggerFactory.getLogger(CxfClientGenerator.class);

  private final ToolContext cxfContext;

  public CxfClientGenerator() {
    this.cxfContext = new ToolContext();
    cxfContext.put(ToolConstants.CFG_BINDING, IvyGeneratorBindings.getBindings());
  }

  public CxfClientGenerator compiler(Compiler compiler) {
    cxfContext.put(ToolConstants.COMPILER, compiler);
    return this;
  }

  public CxfClientGenerator namespaceMapping(Map<String, String> mappings) {
    mappings.forEach((k, v) -> cxfContext.addNamespacePackageMap(k, v));
    return this;
  }

  public ToolContext generate(String wsdlUri, Consumer<File> clientJarUser) throws Exception {
    File tmpGenDir = Files.createTempDirectory("cxfClient").toFile();
    try {
      File tmpClientJar = new File(tmpGenDir, "client.jar");

      List<String> args = Arrays.asList(
          "-d", tmpClientJar.getParentFile().getAbsolutePath(), // outputDir
          "-clientjar", tmpClientJar.getName(),
          "-autoNameResolution", // solve conflicts
          "-mark-generated", // @Generated annotation
          "-xjc-Xsetters", // JAXB2_basics plugin. Generates setter methods for collections
          "-xjc-Xcommons-lang", //JAXB2_commons_lang plugin. Generates toString, equals, hashCode methods
          "-xjc-Xcommons-lang:ToStringStyle=SHORT_PREFIX_STYLE",
          wsdlUri);

      Callable<ToolContext> generate = () -> {
        //should be fixed with cxf version 3.2.5
        //JAXWSBindingSerializer.register(); // Bug fix for CXF-7695
        WSDLToJava cxfGenerator = new WSDLToJava(args.toArray(new String[args.size()]));

        Compiler compiler = (Compiler) cxfContext.get(ToolConstants.COMPILER);
        compiler.setOutputDir(tmpClientJar);
        cxfGenerator.run(cxfContext);

        appendSources(tmpClientJar);
        FixCXFSchemaLocation.fixLocalWsdlIfNecessary(tmpClientJar); // Bug fix for CXF-7706
        moveLocalWsdlToService(tmpClientJar, cxfContext);

        clientJarUser.accept(tmpClientJar);
        return cxfContext;
      };

      return withRedirectConfigurer(
               withSchemaAccProperty(
                 withHttpPortFactory(
                   withTcl(generate)
                 )
               )
             ).call();
    } finally {
      FileUtils.deleteDirectory(tmpGenDir);
    }
  }


  public static ToolContext generate(String wsdlUri, Consumer<File> clientJarUser, Map<String, String> namespaceToPackageNames, MavenCxfCompiler compiler) throws Exception {
    return new CxfClientGenerator()
      .compiler(compiler)
      .namespaceMapping(namespaceToPackageNames)
      .generate(wsdlUri, clientJarUser);
  }

  private static void moveLocalWsdlToService(File tmpClientJar, ToolContext cxfContext) throws IOException {
    URI uri = URI.create("jar:" + tmpClientJar.toURI());
    try (FileSystem zipFs = FileSystems.newFileSystem(uri, new HashMap<>())) {
      List<String> serviceNs = cxfContext.getJavaModel().getServiceClasses().values().stream()
        .map(service -> service.getPackageName())
        .collect(Collectors.toList());
      if (serviceNs.size() != 1) {
        // keep wsdl in root of jar
        return;
      }

      String path = StringUtils.replace(serviceNs.get(0), ".", "/");
      Path serviceDirZip = zipFs.getPath(path);

      try (Stream<Path> walker = Files.walk(zipFs.getPath("/"), 1)) {
        walker.filter(CxfClientGenerator::isSchemaFile)
          .forEach(schemaPath -> {
            try {
              Files.move(schemaPath, serviceDirZip.resolve(schemaPath.getFileName()));
            } catch (IOException ex) {
              LOGGER.warn("Failed to move service definition files", ex);
            }
          });
      }
    }
  }

  private static boolean isSchemaFile(Path zipPath) {
    String entry = zipPath.toString().toLowerCase();
    return entry.endsWith(".wsdl") || entry.endsWith(".xsd");
  }

  /**
   * embedded java sources next to its binaries in the JAR
   */
  private static void appendSources(File tmpClientJar) throws  IOException {
    URI uri = URI.create("jar:" + tmpClientJar.toURI());
    Path tmpPath = tmpClientJar.getParentFile().toPath();
    try (FileSystem zipFs = FileSystems.newFileSystem(uri, new HashMap<>());
         Stream<Path> walker = Files.walk(tmpPath)) {
        walker.filter(path -> path.toFile().getName().endsWith(".java"))
        .forEach(entry -> {
          Path relative = tmpPath.relativize(entry);
          try {
            Files.copy(entry, zipFs.getPath(relative.toString()));
          } catch (IOException ex) {
            LOGGER.warn("Failed to copy java source into CXF client jar", ex);
          }
        });
    }
  }

  private static <T> Callable<T> withHttpPortFactory(Callable<T> callable) {
    return ()-> {
      IvyHTTPTransportFactory httpTransportFactory = new IvyHTTPTransportFactory();
      Map<String, DestinationFactory> original = new HashMap<>();
      try {
        original.putAll(httpTransportFactory.register());
        return callable.call();
      } finally {
        httpTransportFactory.reset(original);
      }
    };
  }

  private static <T> Callable<T> withTcl(Callable<T> callable) {
    Thread thread = Thread.currentThread();
    Callable<T> withWsdl2Java = () -> {
      ClassLoader origTcl = thread.getContextClassLoader();
      try {
        thread.setContextClassLoader(WSDLToJava.class.getClassLoader());
        return callable.call();
      } finally {
        thread.setContextClassLoader(origTcl);
      }
    };
    return withWsdl2Java;
  }

  private static Callable<ToolContext> withRedirectConfigurer(Callable<ToolContext> callable) {
    return () -> {
      Properties originalProps = (Properties) System.getProperties().clone();
      System.setProperty("http.autoredirect", Boolean.TRUE.toString());
      Bus myBus = BusFactory.getDefaultBus();
      HTTPConduitConfigurer configurer = myBus.getExtension(HTTPConduitConfigurer.class);
      try {
        myBus.setExtension(new RedirectConfigurer(), HTTPConduitConfigurer.class);
        return callable.call();
      } finally {
        myBus.setExtension(configurer, HTTPConduitConfigurer.class);
        System.setProperties(originalProps);
      }
    };
  }

  private static <T> Callable<T> withSchemaAccProperty(Callable<T> callable) {
    return () -> {
      String externalSchemaAccessKey = "javax.xml.accessExternalSchema";
      String oldSchemaAccess = System.getProperty(externalSchemaAccessKey);
      try {
        System.setProperty(externalSchemaAccessKey, "all");
        return callable.call();
      } finally {
        resetProperty(externalSchemaAccessKey, oldSchemaAccess);
      }
    };
  }

  private static void resetProperty(String externalSchemaAccessKey, String oldSchemaAccess) {
    if (oldSchemaAccess == null) {
      System.clearProperty(externalSchemaAccessKey);
    } else {
      System.setProperty(externalSchemaAccessKey, oldSchemaAccess);
    }
  }

  private static class RedirectConfigurer implements HTTPConduitConfigurer {
    @Override
    public void configure(String name, String address, HTTPConduit c) {
      HTTPClientPolicy client = c.getClient();
      if (client == null) {
        client = new HTTPClientPolicy();
      }

      TLSClientParameters tlsClientParameters = c.getTlsClientParameters();
      if (tlsClientParameters==null) {
        tlsClientParameters = new TLSClientParameters();
        tlsClientParameters.setUseHttpsURLConnectionDefaultHostnameVerifier(true);
        tlsClientParameters.setUseHttpsURLConnectionDefaultSslSocketFactory(true);
      }
      c.setTlsClientParameters(tlsClientParameters);

      client.setAutoRedirect(true);
      c.setClient(client);
    }
  }
}