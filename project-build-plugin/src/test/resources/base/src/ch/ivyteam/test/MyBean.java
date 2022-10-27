package ch.ivyteam.test;

import ch.ivyteam.ivy.process.call.SubProcessSearchFilter;
import ch.ivyteam.ivy.process.call.SubProcessSearchFilter.Builder;

public class MyBean {

  public String echo(String input)
  {
    Builder ivyFilterBuilder = SubProcessSearchFilter.create();
    ivyFilterBuilder.toFilter();
    return input;
  }
  
}
