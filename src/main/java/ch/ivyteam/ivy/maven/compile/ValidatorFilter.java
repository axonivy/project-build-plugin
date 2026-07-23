package ch.ivyteam.ivy.maven.compile;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.logging.Log;

import ch.ivyteam.ivy.project.validation.ProjectValidator;

class ValidatorFilter {

  private final List<String> excludeKeywords;
  private final List<String> skipped = new ArrayList<>();
  private final Log log;

  ValidatorFilter(List<String> excludeKeywords, Log log) {
    this.excludeKeywords = excludeKeywords == null ? List.of() : excludeKeywords;
    this.log = log;
  }

  @SuppressWarnings("rawtypes")
  List<ProjectValidator> apply(List<ProjectValidator> all) {
    if (excludeKeywords.isEmpty()) {
      return all;
    }
    return all.stream()
        .filter(validator -> !isExcluded(validator))
        .toList();
  }

  List<String> skipped() {
    return List.copyOf(skipped);
  }

  @SuppressWarnings("rawtypes")
  private boolean isExcluded(ProjectValidator validator) {
    var id = validator.id();
    var excluded = excludeKeywords.stream()
        .filter(keyword -> keyword != null && !keyword.isBlank())
        .anyMatch(keyword -> id.equalsIgnoreCase(keyword));
    if (excluded) {
      skipped.add(id);
      log.info("Skipping validator '" + id + "' (excluded by configuration)");
    }
    return excluded;
  }
}
