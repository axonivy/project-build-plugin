package ch.ivyteam.ivy.webservice.exec.cxf.codegen;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.BusFactory;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.transport.http.HTTPTransportFactory;
import org.apache.cxf.wsdl11.WSDLEndpointFactory;

public class IvyHTTPTransportFactory extends HTTPTransportFactory implements WSDLEndpointFactory {
  public IvyHTTPTransportFactory() {
    super(DEFAULT_NAMESPACES, null);
  }

  @Override
  public EndpointInfo createEndpointInfo(Bus bus, ServiceInfo serviceInfo, BindingInfo b, List<?> extensions) {
    return super.createEndpointInfo(serviceInfo, b, extensions);
  }

  @Override
  public void createPortExtensors(Bus bus, EndpointInfo ei, Service service) {
    super.createPortExtensors(ei, service);
  }

  Map<String, DestinationFactory> register() {
    DestinationFactoryManager dfm = getDestinationFactoryManager();
    Map<String, org.apache.cxf.transport.DestinationFactory> nsFactories = new HashMap<>();
    DEFAULT_NAMESPACES.forEach(ns -> {
      nsFactories.put(ns, getRegisteredFactory(dfm, ns));
      dfm.deregisterDestinationFactory(ns);
      dfm.registerDestinationFactory(ns, this);
    });
    return nsFactories;
  }

  private DestinationFactoryManager getDestinationFactoryManager() {
    return BusFactory.getDefaultBus().getExtension(DestinationFactoryManager.class);
  }

  void reset(Map<String, DestinationFactory> nsFactories) {
    DestinationFactoryManager dfm = getDestinationFactoryManager();
    nsFactories.keySet().stream().forEach(ns -> {
      dfm.deregisterDestinationFactory(ns);
      DestinationFactory factory = nsFactories.get(ns);
      if (factory != null) {
        dfm.registerDestinationFactory(ns, factory);
      }
    });
  }

  private static DestinationFactory getRegisteredFactory(DestinationFactoryManager dfm, String ns) {
    try {
      return dfm.getDestinationFactory(ns);
    } catch (BusException ex) { // nothing registered
      return null;
    }
  }
}
