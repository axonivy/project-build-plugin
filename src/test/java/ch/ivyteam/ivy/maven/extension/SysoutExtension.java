package ch.ivyteam.ivy.maven.extension;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

public class SysoutExtension implements BeforeEachCallback, BeforeAllCallback, AfterAllCallback, ParameterResolver {

  private PrintStream original;
  private ByteArrayOutputStream memory;

  public interface Sysout {
    String content();
    void reset();
  }

  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
    return Sysout.class.isAssignableFrom(parameterContext.getParameter().getType());
  }

  @Override
  public Sysout resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
    return new SysoutImpl();
  }

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    this.memory.reset();
  }

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    this.original = System.out;
    this.memory = new ByteArrayOutputStream();
    memorySysout();
  }

  private void memorySysout() {
    var memoryOut = new PrintStream(memory);
    System.setOut(memoryOut);
  }

  @Override
  public void afterAll(ExtensionContext context) throws Exception {
    restoreSysout();
  }

  private void restoreSysout() {
    System.setOut(original);
  }

  private class SysoutImpl implements Sysout {

    @Override
    public String content() {
      return memory.toString();
    }

    @Override
    public void reset() {
      memory.reset();
    }

    @Override
    public String toString() {
      return content();
    }

  }

}
