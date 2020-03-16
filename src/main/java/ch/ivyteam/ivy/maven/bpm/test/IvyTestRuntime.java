package ch.ivyteam.ivy.maven.bpm.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

public class IvyTestRuntime
{
  public static final String RUNTIME_PROPS_RESOURCE="ivyTestRuntime.properties";
  
  public static interface Key
  {
    String PRODUCT_DIR = "product.dir";
  }
  
  private final Properties props = new Properties();
  
  public void setProductDir(File engineDir)
  {
    props.put(Key.PRODUCT_DIR, engineDir.getAbsolutePath());
  }
  
  public File store(File dir) throws IOException
  {
    File propsFile = new File(dir, RUNTIME_PROPS_RESOURCE);
    try(OutputStream os = new FileOutputStream(propsFile))
    {
      props.store(os, "ivy test vm runtime properties");
    }
    return propsFile;
  }
  
}
