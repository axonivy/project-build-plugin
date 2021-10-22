package ch.ivyteam.ivy.webservice.exec.cxf.codegen.binding;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.cxf.Bus;

public class IvyGeneratorBindings {
  private static final Collection<String> BINDING_RESOURCES = Arrays.asList("globalJaxbBindings.xml", "noWrappersJaxWsBinding.xml");

  public static String[] getBindings() {
    List<String> list = BINDING_RESOURCES
        .stream()
        .map(bindingResource -> IvyGeneratorBindings.class.getResource(bindingResource))
        .map(bindingResourceUrl -> bindingResourceUrl.toExternalForm())
        .collect(Collectors.toList());
    list.add(Bus.class.getResource("/schemas/wsdl/XMLSchema.xsd").toExternalForm());
    return list.toArray(String[]::new);
  }

}
