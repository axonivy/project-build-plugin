package ch.ivyteam.ivy.maven.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import ch.ivyteam.ivy.maven.log.LogCollector;
import ch.ivyteam.ivy.process.model.Process;
import ch.ivyteam.ivy.process.model.element.event.start.CallSubStart;
import ch.ivyteam.ivy.process.model.value.ProcessName;
import ch.ivyteam.ivy.process.model.value.scripting.VariableDesc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

class TestJsonReporter {

  @Test
  void process_subProcess() {
    var process = loadProcess("/base/process/myFunction.p.json", "myFunction");
    var json = reportCallables(process);

    var elements = json.get("elements");
    assertThat(elements).hasSize(1);
    var caller = elements.get(0);
    assertThat(caller.get("type").asString())
        .isEqualTo("SubProcessCall");
    assertThat(caller.get("name").asString())
        .isEqualTo("userInfo()");
    assertThat(caller.get("description").asString())
        .isEqualTo("lookup user information");
    assertThat(caller.get("tags"))
        .isInstanceOf(ArrayNode.class)
        .hasSize(1);
    assertThat(caller.get("tags").get(0).asString())
        .isEqualTo("connector");
    assertThat(caller.get("config").get("processCall").asString())
        .isEqualTo("myFunction:call(Integer)");

    var params = (ObjectNode) caller.get("param").get("properties");
    assertThat(params.propertyNames())
        .containsExactly("userId");

    var result = (ObjectNode) caller.get("result").get("properties");
    assertThat(result.propertyNames())
        .containsExactly("firstName", "lastName");
  }

  @Test
  void process_typeRefs() {
    var process = loadProcess("/base/process/myFunction.p.json", "myFunction");
    var start = process.search().type(CallSubStart.class).findOne();
    var updated = start.getSignature().setInputParameters(List.of(new VariableDesc("user", MyPerson.class.getName())));
    start.setSignature(updated);

    var json = reportCallables(process);
    var caller = json.get("elements").get(0);
    var params = caller.get("param").get("properties");
    assertThat(params.propertyNames())
        .containsExactly("user");
    assertThat(params.get("user").get("$ref").asString())
        .as("complex type trees are referenced in $defs")
        .isEqualTo("#/$defs/MyPerson");
    var person = json.get("$defs").get("MyPerson");
    assertThat(person.get("type").asString())
        .isEqualTo("object");
    assertThat(person.get("properties").propertyNames())
        .containsExactly("firstName", "lastName");
  }

  @Test
  void process_typeRefs_unresolvable() {
    var process = loadProcess("/base/process/myFunction.p.json", "myFunction");
    var start = process.search().type(CallSubStart.class).findOne();
    var updated = start.getSignature()
        .setInputParameters(List.of(new VariableDesc("user", "com.acme.oblivion.Person")))
        .setOutputParameters(List.of());
    start.setSignature(updated);

    LogCollector log = new LogCollector();
    var reporter = new JsonReporter(TestComponentsMojo.testLoader(), log);
    reporter.callables(Stream.of(start));
    var json = reporter.finish();
    assertThat(json.get("elements").get(0).get("param").get("properties").get("user").get("x-java-type").asString())
        .as("type-hint for java is present; json writing succeeds even if types are partially unresolvable")
        .isEqualTo("com.acme.oblivion.Person");

    assertThat(log.getErrors()).hasSize(1);
    assertThat(log.getErrors().get(0).toString())
        .contains("Skipping schema for unresolvable type com.acme.oblivion.Person");
  }

  private static JsonNode reportCallables(Process process) {
    var reporter = new JsonReporter(TestComponentsMojo.testLoader(), new LogCollector());
    var callables = process.search().type(CallSubStart.class).findDeep();
    reporter.callables(callables.stream());
    return reporter.finish();
  }

  private static Process loadProcess(String res, String name) {
    try (var in = TestJsonReporter.class.getResourceAsStream(res)) {
      return ProcessReader.readProcess(in, new ProcessName(name, ""));
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public static class MyPerson {
    public String firstName;
    public String lastName;
  }

}
