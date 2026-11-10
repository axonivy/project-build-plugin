package ch.ivyteam.ivy.maven.report;

import java.lang.reflect.Type;

import ch.ivyteam.ivy.maven.project.ProjectContextFactory;

class ProjectTypeLoader {

  private final ProjectContextFactory projectContext;
  private ClassLoader classLoader;

  ProjectTypeLoader(ProjectContextFactory projectContext) {
    this.projectContext = projectContext;
  }

  Type loadType(String type) {
    try {
      return lazyLoader().loadClass(type);
    } catch (ClassNotFoundException ex) {
      throw new RuntimeException("Failed to load " + type + " using " + lazyLoader(), ex);
    }
  }

  private ClassLoader lazyLoader() {
    if (classLoader != null) {
      return classLoader;
    }
    var ctx = projectContext.create();
    this.classLoader = ctx.classLoader();
    return classLoader;
  }
}
