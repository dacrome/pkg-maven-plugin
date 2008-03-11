/*
 * Maven Packaging Plugin,
 * Maven plugin to package a Project (deb, ipk, izpack)
 * Copyright (C) 2000-2008 tarent GmbH
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License,version 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 *
 * tarent GmbH., hereby disclaims all copyright
 * interest in the program 'Maven Packaging Plugin'
 * Signature of Elmar Geese, 11 March 2008
 * Elmar Geese, CEO tarent GmbH.
 */

package de.tarent.maven.plugins.pkg.map;

import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import de.tarent.maven.plugins.pkg.Utils;

/**
 * A <code>PackageMap</code> instance denotes the mapping between a Maven2
 * dependency and a package in a distribution.
 * 
 * <p>The mapping mechanism is weaker than what Maven provides: A dependency
 * in Maven consists of an artifactId, a groupId and a version number. In its
 * current form the groupId and version number is not regarded.
 * 
 * <p>The group id is left out because there is a small propability that
 * artifact ids conflict.</p>
 * 
 * <p>The version number is left out because distributions usually only package
 * one specific version of a package.</p>
 * 
 * <p>If this will pose problems in the future the package maps format can be beefed up 
 * to support those features as well. For now it keeps the format half-way simple.</p>
 * 
 * <p>Apart from the package name mapping a bunch of distribution-specific properties
 * can be defined. For instance the location of JNI libraries or the jars, the default
 * dependency line for Java applications and whether a distribution follows a Debian-like
 * naming scheme (java library foo gets packaged as libfoo-java).</p>
 * 
 * <p>Since package maps evolve as the distribution is developed further one can define
 * inheritance relations between the package maps. <code>PackageMap</code> will only
 * provide access to the data that has been merged from the child to all its ancestors.</p> 
 * 
 * <p>The package map is using at least one default XML document and can optionally read
 * in another document to supplement the first. The latter is done to provide your own
 * mappings without having to recompile the packaging plugin or wait for another
 * release.</p>
 * 
 * <p>Since <code>PackageMap</code> acts as a facade to all the package map related
 * functions the name of the targeted distribution has to be provided upon
 * instantiation.</p>
 * 
 * <p><em>Important:</em>IzPack packaging is a bit different from the other packaging
 * variants in that it does not have a special target distribution. IzPack packaging
 * is provided through the 'izpack' distribution.</p>
 * 
 * @author Robert Schuster (robert.schuster@tarent.de)
 *
 */
public class PackageMap
{
  private Mapping mapping;
  
  private Set bundleOverrides;
  
  /**
   * Creates a <code>PackageMap</code< instance which can then be used to query the mapping
   * between Maven artifacts and package names in the target distribution. 
   * 
   * @param packageMapURL Location of the default package map XML document (must not be null or invalid).
   * @param auxPackageMapURL Location of the supplemental package map XML document (optional, can be null)
   * @param distribution Name of the targeted distribution.
   * @param bundleOverrides Set of artifact ids which should be treated as bundled, regardless of what the package maps say.
   * @throws MojoExecutionException If the document parsing fails.
   */
  public PackageMap(URL packageMapURL,
                    URL auxPackageMapURL,
                    String distribution,
                    Set bundleOverrides)
    throws MojoExecutionException
  {
    this.bundleOverrides = bundleOverrides;
    
    if (packageMapURL == null)
      packageMapURL = PackageMap.class.getResource("default-package-maps.xml");
    
    try
    {
      mapping = new Parser(packageMapURL,
                           auxPackageMapURL).getMapping(distribution);
    }
    catch (Parser.Exception pe)
    {
      throw new MojoExecutionException("Package map creation failed", pe);
    }
    
  }
  
  /**
   * Returns the packaging variant used by the distribution. Supported variants
   * are "ipk", "deb" and "izpack".
   * 
   * <p><a href="http://mvn-pkg-plugin.evolvis.org">Implement} rpm and make some people happy!</a></p> 
   * 
   * @return
   */
  public String getPackaging()
  {
    return mapping.packaging;
  }

  /**
   * Returns the dependency line that is neccessary for Java application in the target
   * distribution. 
   * 
   * <p>In case this has not been set in the XML document the value is "java2-runtime".</p>
   *  
   * @return
   */
  public String getDefaultDependencyLine()
  {
    return (mapping.defaultDependencyLine != null ? mapping.defaultDependencyLine : "java2-runtime");
  }
  
  /**
   * Returns the default location for Jar files.
   * 
   * <p>In case this has not been set in the XML document the value is "/usr/share/java" which is
   * the default for Debian and derivatives.</p> 
   * 
   * @return
   */
  public String getDefaultJarPath()
  {
    return (mapping.defaultJarPath != null ? mapping.defaultJarPath : "/usr/share/java");
  }

  /**
   * Returns the default location for JNI libraries.
   * 
   * <p>In case this has not been set in the XML document the value is "/usr/lib/jni" which is
   * the default for Debian and derivatives.</p> 
   * 
   * @return
   */
  public String getDefaultJNIPath()
  {
    return (mapping.defaultJNIPath != null ? mapping.defaultJNIPath : "/usr/lib/jni");
  }
  
  public String getDistroLabel()
  {
    return mapping.label;
  }
  
  /**
   * Returns the default location for executable (scripts).
   * 
   * <p>In case this has not been set in the XML document the value is "/usr/bin".</p> 
   * 
   * @return
   */
  public String getDefaultBinPath()
  {
    return (mapping.defaultBinPath != null ? mapping.defaultBinPath : "/usr/bin");
  }
  
  /**
   * Returns whether the package names generally follow a Debian-style renaming.
   * 
   * <p>In case this has not been set in the XML document the value is <code>true</code>.</p>
   *  
   * @return
   */
  public boolean isDebianNaming()
  {
    return (mapping.debianNaming != null ? mapping.debianNaming.booleanValue() : true);
  }
  
  public boolean hasNoPackages()
  {
    return mapping.hasNoPackages;
  }

  public void iterateDependencyArtifacts(Log l, Collection deps, Visitor v, boolean bundleNonExisting)
  {
   for (Iterator ite = deps.iterator(); ite.hasNext(); )
     {
       Artifact a = (Artifact) ite.next();
       String aid = a.getArtifactId();
       
       // Bundle dependencies which have been explicitly
       // marked as such.
       if (bundleOverrides.contains(aid))
         {
           v.bundle(a);
           continue;
         }
       
       Entry e = (Entry) mapping.getEntry(a.getGroupId(), aid);
       // If a distro is explicitly declared to have no packages everything
       // will be bundled (without warning).
       if (mapping.hasNoPackages)
         v.bundle(a);
       else if (e == Entry.BUNDLE_ENTRY)
         // If a package is explicitly said to be bundled this will be done
         // without warning.
         v.bundle(a);
       else if (e == null)
         {
           // If a package as not been declared a warning reminds to fix the
           // package map.
           l.warn(mapping.distro + " has no entry for: " + a);
           
           if (bundleNonExisting)
             v.bundle(a);
         }
       else if (e != Entry.IGNORE_ENTRY)
         // If a package is explicitly said to be bundled this will be done
         // without warning.
         v.visit(a, e);
       
     }
     
  }
  
  public Entry getEntry(String groupId, String artifactId, String section)
  {
    Entry e = (Entry) mapping.getEntry(groupId, artifactId);
    
    // If an entry does not exist create one based on the artifact id.
    // This is needed if the project is a library and is going to be packaged.
    if (e == null)
      {
        HashSet hs = new HashSet();
        hs.add(getDefaultJarPath() + "/" + artifactId + ".jar");
        
        e = new Entry(artifactId,
                      Utils.createPackageName(artifactId, section, isDebianNaming()),
                      hs,
                      false);
      }
    
    return e;
  }

}
