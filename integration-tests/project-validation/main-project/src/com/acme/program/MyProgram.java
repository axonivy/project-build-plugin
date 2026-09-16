package com.acme.program;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.program.activity.AbortableExecution;
import ch.ivyteam.ivy.process.program.activity.ProgramExecutor;

public class MyProgram implements ProgramExecutor {

  @Override
  public AbortableExecution newExecution() {
    return ctxt -> {
      Ivy.log().info("Custom program on: " + ctxt.element().name());
    };
  }

}
