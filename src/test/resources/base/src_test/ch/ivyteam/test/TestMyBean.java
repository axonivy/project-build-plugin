package ch.ivyteam.test;

import org.junit.Test;

import ch.ivyteam.test.MyBean;

public class TestMyBean {

  @Test
  public void testEcho()
  {
    new MyBean().echo("hi");
  }
  
}
