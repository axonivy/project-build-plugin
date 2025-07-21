package com.axonivy.test.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.environment.IvyTest;

/**
 * This sample UnitTest runs java code in an environment as it would exists when
 * being executed in Ivy Process. Popular projects API facades, such as {@link Ivy#persistence()}
 * are setup and ready to be used.
 * 
 * <p>The test can either be run<ul>
 * <li>in the Designer IDE ( <code>right click > run as > JUnit Test </code> )</li>
 * <li>or in a Maven continuous integration build pipeline ( <code>mvn clean verify</code> )</li>
 * </ul></p>
 * 
 * <p>Detailed guidance on writing these kind of tests can be found in our
 * <a href="https://developer.axonivy.com/doc/13.2/concepts/testing/unit-testing.html">Unit Testing docs</a>
 * </p>
 */
@IvyTest
public class SampleIvyTest{
  
  @Test
  public void useIvy(){
    Ivy.log().info("hi from JUnit");
    assertThat(true).as("I can use Ivy API facade in tests").isEqualTo(true);
  }
  
}
