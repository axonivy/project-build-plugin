package ch.ivyteam.ivy.maven.report;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.maven.plugin.logging.Log;

import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaBuilder;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;

import ch.ivyteam.ivy.process.model.element.activity.SubProcessCall;
import ch.ivyteam.ivy.process.model.element.activity.value.ProcessCallSignature;
import ch.ivyteam.ivy.process.model.element.event.start.CallSubStart;
import ch.ivyteam.ivy.process.model.element.event.start.value.CallSignature;
import ch.ivyteam.ivy.process.model.value.scripting.VariableDesc;
import ch.ivyteam.ivy.process.schema.adapter.call.ProcessRefAdapter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.StringNode;

public class JsonReporter {

  private final Function<String, Type> typeLoader;
  private final SchemaBuilder schemaGenerator;
  private final ObjectNode components;
  private final Log log;

  public JsonReporter(Function<String, Type> typeLoader, Log log) {
    this.typeLoader = typeLoader;
    this.log = log;
    this.schemaGenerator = schemaGenerator();
    this.components = JsonNodeFactory.instance.objectNode();
  }

  public void callables(Stream<CallSubStart> callables) {
    var elements = components.putArray("elements");
    callables.forEach(callable -> elements.add(reportJson(callable)));
  }

  private ObjectNode reportJson(CallSubStart callable) {
    var element = JsonNodeFactory.instance.objectNode();
    element.put("type", SubProcessCall.class.getSimpleName());
    optional(callable.getName())
        .ifPresent(name -> element.put("name", name));
    optional(callable.getDescription())
        .ifPresent(description -> element.put("description", description));
    callable.getShape().getCustomSymbol()
        .ifPresent(symbol -> element.put("icon", symbol.toASCIIString()));
    var tags = callable.getTags().toList();
    if (!tags.isEmpty()) {
      var tagsArray = element.putArray("tags");
      tags.forEach(tagsArray::add);
    }

    CallSignature signature = callable.getSignature();
    var config = element.putObject("config");
    var ref = new ProcessCallSignature(callable.getRootProcess().getFullQualifiedName(), signature.toReference());
    config.put("processCall", new ProcessRefAdapter().toSchema(ref));
    var ins = signature.getInputParameters();
    if (!ins.isEmpty()) {
      element.set("param", schemaOf(ins));
    }
    var outs = signature.getOutputParameters();
    if (!outs.isEmpty()) {
      element.set("result", schemaOf(outs));
    }
    return element;
  }

  private Optional<String> optional(String name) {
    return Optional.ofNullable(name)
        .filter(Predicate.not(String::isBlank));
  }

  private JsonNode schemaOf(List<VariableDesc> variables) {
    var schema = object();

    var props = schema.putObject("properties");
    for (var variable : variables) {

      var fullType = variable.getType().fullQualifiedName();
      var varSchema = load(fullType)
          .map(this::typeRef)
          .orElseGet(JsonReporter::object);
      props.set(variable.getName(), varSchema);

      var info = variable.getInfo();
      optional(info.getDescription())
          .ifPresent(description -> varSchema.put("description", description));

      if (varSchema.get("type") instanceof StringNode jsonType && "object".equals(jsonType.stringValue())) {
        varSchema.put("x-java-type", variable.getType().getName());
      }
    }
    return schema;
  }

  private ObjectNode typeRef(Type type) {
    try {
      return schemaGenerator.createSchemaReference(type);
    } catch (Throwable ex) {
      log.error("Skipping schema for unresolvable type " + type, ex);
      return JsonReporter.object();
    }
  }

  public JsonNode finish() {
    var defs = schemaGenerator.collectDefinitions("$defs");
    if (!defs.isEmpty()) {
      components.set("$defs", defs);
    }
    return components;
  }

  private static SchemaBuilder schemaGenerator() {
    var config = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2019_09, OptionPreset.PLAIN_JSON)
        .with(Option.DEFINITIONS_FOR_ALL_OBJECTS)
        .build();
    return new SchemaGenerator(config).buildMultipleSchemaDefinitions();
  }

  private Optional<Type> load(String fullType) {
    try {
      var type = typeLoader.apply(fullType);
      return Optional.of(type);
    } catch (Throwable ex) {
      log.error("Skipping schema for unresolvable type " + fullType, ex);
      return Optional.empty();
    }
  }

  private static ObjectNode object() {
    return JsonNodeFactory.instance.objectNode().put("type", "object");
  }

}
