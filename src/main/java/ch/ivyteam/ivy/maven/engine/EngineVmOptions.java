package ch.ivyteam.ivy.maven.engine;

public class EngineVmOptions
{
  public final String maxmem;
  public final String additionalClasspath;
  public final String additionalVmOptions;
  
  public EngineVmOptions(String maxmem, String additionalClasspath, String additionalVmOptions)
  {
    this.maxmem = maxmem;
    this.additionalClasspath = additionalClasspath;
    this.additionalVmOptions = additionalVmOptions;
  }
}
