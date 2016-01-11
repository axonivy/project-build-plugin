/*
 * Copyright (C) 2016 AXON IVY AG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.ivyteam.ivy.maven.util;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang3.StringUtils;

/**
 * Jar with only a Manifest.MF that defines the classpath.
 * 
 * @author Reguel Wermelinger
 * @since 6.0.2
 */
public class ClasspathJar
{
  private static final String MANIFEST_MF = "META-INF/MANIFEST.MF";
  private final File jar;

  public ClasspathJar(File jar)
  {
    this.jar = jar;
  }
  
  public void create(List<String> classpathEntries) throws IOException
  {
    jar.getParentFile().mkdir();
    try(ZipOutputStream zipStream = new ZipOutputStream(new FileOutputStream(jar)))
    {
      String name = StringUtils.substringBeforeLast(jar.getName(), ".");
      writeManifest(name, zipStream, classpathEntries);
    }
  }
  
  public void createFileEntries(Collection<File> classpathEntries) throws IOException
  {
    create(getClassPathUris(classpathEntries));
  }
  
  private void writeManifest(String name, ZipOutputStream jarStream, List<String> classpathEntries) throws IOException
  {
    Manifest manifest = new Manifest();
    Attributes attributes = new Attributes();
    attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
    if (!classpathEntries.isEmpty())
    {
      attributes.put(Attributes.Name.CLASS_PATH, StringUtils.join(classpathEntries, " "));
    }
    manifest.getEntries().put(name, attributes);
    jarStream.putNextEntry(new ZipEntry(MANIFEST_MF));
    manifest.write(jarStream);
  }
  
  private static List<String> getClassPathUris(Collection<File> classpathEntries)
  {
    return classpathEntries.stream()
              .map(file -> file.toURI().toASCIIString())
              .collect(Collectors.toList());
  }
  
  public String getClasspathFiles()
  {
    String urlClasspath = getClasspathUrlEntries();
    if (!urlClasspath.contains(" "))
    {
      return uriToAbsoluteFilePath(urlClasspath);
    }
    
    List<String> cp = new ArrayList<>();
    for(String entry : urlClasspath.split(" "))
    {
      cp.add(uriToAbsoluteFilePath(entry));
    }
    return StringUtils.join(cp, ",");
  }

  private static String uriToAbsoluteFilePath(String entry)
  {
    try
    {
      return new URI(entry).getSchemeSpecificPart();
    }
    catch (URISyntaxException ex)
    {
      return entry;
    }
  }
  
  public String getClasspathUrlEntries()
  {
    try(ZipInputStream is = new ZipInputStream(new FileInputStream(jar)))
    {
      Manifest manifest = new Manifest(getInputStream(is, MANIFEST_MF));
      return manifest.getEntries().values().iterator().next().getValue(Attributes.Name.CLASS_PATH);
    }
    catch (IOException ex)
    {
      return null;
    }
  }
  
  private static InputStream getInputStream(ZipInputStream zin, String entry) throws IOException {
    for (ZipEntry e; (e = zin.getNextEntry()) != null;) {
        if (e.getName().equals(entry)) {
            return zin;
        }
    }
    throw new EOFException("Cannot find " + entry);
  }
}
