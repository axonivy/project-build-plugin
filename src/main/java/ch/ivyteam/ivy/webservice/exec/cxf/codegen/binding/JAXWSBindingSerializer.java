package ch.ivyteam.ivy.webservice.exec.cxf.codegen.binding;

import java.io.PrintWriter;

import javax.wsdl.Binding;
import javax.wsdl.BindingOperation;
import javax.wsdl.Definition;
import javax.wsdl.Fault;
import javax.wsdl.Operation;
import javax.wsdl.Port;
import javax.wsdl.PortType;
import javax.wsdl.Service;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.extensions.ExtensionSerializer;
import javax.xml.namespace.QName;

import org.apache.cxf.BusFactory;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.wsdl.WSDLManager;

/**
 * This class fixes CXF-7695.
 * It can be deleted if the bug is fixed in CXF.
 * @author rwei
 */
public class JAXWSBindingSerializer implements ExtensionSerializer {

  @Override
  public void marshall(@SuppressWarnings("rawtypes") Class parentType, QName elementType, ExtensibilityElement extension, PrintWriter pw,
          Definition def, ExtensionRegistry extReg) throws WSDLException {
  }

  public static void register() {
    WSDLManager mgr = BusFactory.getDefaultBus().getExtension(WSDLManager.class);
    ExtensionRegistry registry = mgr.getExtensionRegistry();
    register(registry, Definition.class);
    register(registry, Service.class);
    register(registry, Fault.class);
    register(registry, PortType.class);
    register(registry, Port.class);
    register(registry, Operation.class);
    register(registry, Binding.class);
    register(registry, BindingOperation.class);
  }

  private static void register(ExtensionRegistry registry, Class<?> clz) {
    registry.registerSerializer(clz, ToolConstants.JAXWS_BINDINGS, new JAXWSBindingSerializer());
  }
}