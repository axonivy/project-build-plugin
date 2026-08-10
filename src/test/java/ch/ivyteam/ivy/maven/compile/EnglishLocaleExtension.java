package ch.ivyteam.ivy.maven.compile;

import java.util.Locale;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class EnglishLocaleExtension implements BeforeAllCallback {

  @Override
  public void beforeAll(ExtensionContext context) {
    Locale.setDefault(Locale.ENGLISH);
  }
}

