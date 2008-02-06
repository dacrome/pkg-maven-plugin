/*
 * Maven Packaging Plugin,
 * Maven plugin to package a Project (deb and izpack)
 * Copyright (C) 2000-2007 tarent GmbH
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
 * Signature of Elmar Geese, 14 June 2007
 * Elmar Geese, CEO tarent GmbH.
 */

package de.tarent.maven.plugins.pkg.packager;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import de.tarent.maven.plugins.pkg.AotCompileUtils;
import de.tarent.maven.plugins.pkg.DistroConfiguration;
import de.tarent.maven.plugins.pkg.Packaging;
import de.tarent.maven.plugins.pkg.Utils;
import de.tarent.maven.plugins.pkg.generator.ControlFileGenerator;
import de.tarent.maven.plugins.pkg.map.PackageMap;

/**
 * Creates a Debian package file (.deb)
 * 
 * TODO: Description needs to formatted in a Debian-specific way 
 */
public class DebPackager extends Packager
{

  public void execute(Log l,
                      Packaging.Helper ph,
                      DistroConfiguration distroConfig,
                      PackageMap packageMap) throws MojoExecutionException
  {
    String packageName = ph.getPackageName();
    String packageVersion = ph.getPackageVersion();

    File basePkgDir = ph.getBasePkgDir();
    
    // The Debian control file (package name, dependencies etc).
    File controlFile = new File(basePkgDir, "DEBIAN/control");

    File srcArtifactFile = ph.getSrcArtifactFile();

    String gcjPackageName = ph.getAotPackageName();
    
    File aotPkgDir = ph.getAotPkgDir();
    
    // The file extension for aot-compiled binaries.
    String aotExtension = ".jar.so";
    
    // The destination directory for all aot-compiled binaries.
    File aotDstDir = new File(aotPkgDir, packageMap.getDefaultJarPath());
    
    // The file name of the aot-compiled binary of the project's own artifact.
    File aotCompiledBinaryFile = new File(aotDstDir, ph.getArtifactId() + aotExtension);
    
    // The destination directory for all classmap files. 
    File aotDstClassmapDir = new File(aotPkgDir, "usr/share/gcj-4.1/classmap.d");
    
    // The file name of the classmap of the project's own artifact. 
    File aotClassmapFile = new File(aotDstClassmapDir, ph.getArtifactId() + ".db");
    
    // The destination file for the 'postinst' script.
    File aotPostinstFile = new File(aotPkgDir, "DEBIAN/postinst");

    // The destination file for the 'control' file.
    File aotControlFile = new File(aotPkgDir, "DEBIAN/control");
    
    // A set which will be filled with the artifacts which need to be bundled with the
    // application.
    Set bundledArtifacts = new HashSet();
    
    long byteAmount = srcArtifactFile.length();

    try
      {
    	// The following section does the coarse-grained steps
    	// to build the package(s). It is meant to be kept clean
    	// from simple Java statements. Put additional functionality
    	// into the existing methods or create a new one and add it
    	// here.
    	
        ph.prepareInitialDirectories();

        ph.copyArtifact();
        
        ph.copyJNILibraries();
        
        byteAmount += Utils.copyAuxFiles(l,
                                         ph.getAuxFileSrcDir(),
                                         ph.getBasePkgDir(),
                                         distroConfig.getAuxFiles());

        // Create classpath line, copy bundled jars and generate wrapper
        // start script only if the project is an application.
        if (distroConfig.getMainClass() != null)
          {
            // TODO: Handle native library artifacts properly.
            StringBuilder bcp = new StringBuilder();
            StringBuilder cp = new StringBuilder();
            ph.createClasspathLine(bundledArtifacts, bcp, cp);

            ph.generateWrapperScript(bundledArtifacts, bcp.toString(), cp.toString());
            

            byteAmount += ph.copyArtifacts(bundledArtifacts);
          }
        
        generateControlFile(l,
                            ph,
                            distroConfig,
        		            controlFile,
        		            packageName,
        		            packageVersion,
        		            ph.createDependencyLine(),
        		            byteAmount);

        createPackage(l, ph, basePkgDir);
        
        if (distroConfig.isAotCompile())
          {
            ph.prepareAotDirectories();
            // At this point anything created in the basePkgDir cannot be used
            // any more as it was removed by the above method.
            
            byteAmount = aotCompiledBinaryFile.length() + aotClassmapFile.length();
            
            AotCompileUtils.compile(l, srcArtifactFile, aotCompiledBinaryFile);
            
            AotCompileUtils.generateClassmap(l,
                                             aotClassmapFile,
                                             srcArtifactFile,
                                             aotCompiledBinaryFile,
                                             packageMap.getDefaultJarPath());

            // AOT-compile and classmap generation for bundled Jar libraries
            // are only needed for applications.
            if (distroConfig.getMainClass() != null)
              byteAmount += AotCompileUtils.compileAndMap(l,
            		                bundledArtifacts,
            		                aotDstDir,
            		                aotExtension,
            		                aotDstClassmapDir,
                                    packageMap.getDefaultJarPath());
            
/*            gen.setShortDescription(gen.getShortDescription() + " (GCJ version)");
            gen.setDescription("This is the ahead-of-time compiled version of "
                               + "the package for use with GIJ.\n"
                               + gen.getDescription());
 */          
            // The dependencies of a "-gcj" package are always java-gcj-compat
            // and the corresponding 'bytecode' version of the package.
            // GCJ can only compile for one architecture.
            distroConfig.setArchitecture(System.getProperty("os.arch"));
            generateControlFile(l,
                                ph,
                                distroConfig,
                                aotControlFile,
            		            gcjPackageName,
            		            packageVersion,
            		            "java-gcj-compat",
            		            byteAmount);
            
            AotCompileUtils.depositPostinstFile(l, aotPostinstFile);
            
            createPackage(l, ph, aotPkgDir);
          }
        
      }
    catch (MojoExecutionException badMojo)
      {
        throw badMojo;
      }
    
    /* When the Mojo fails to complete its task the work directory will be left
     * in an unclean state to make it easier to debug problems.
     * 
     * However the work dir will be cleaned up when the task is run next time.
     */
  }

  /** Validates arguments and test tools.
   * 
   * @throws MojoExecutionException
   */
  public void checkEnvironment(Log l, DistroConfiguration dc) throws MojoExecutionException
  {
    // No specifics to show or test.
  }

  /**
   * Creates a control file whose dependency line can be provided as an argument.
   * 
   * @param l
   * @param controlFile
   * @param packageName
   * @param packageVersion
   * @param installedSize
   * @throws MojoExecutionException
   */
  private void generateControlFile(Log l,
                                   Packaging.Helper ph,
                                   DistroConfiguration dc,
                                   File controlFile,
                                   String packageName,
                                   String packageVersion,
                                   String dependencyLine,
                                   long byteAmount)
      throws MojoExecutionException
  {
	ControlFileGenerator cgen = new ControlFileGenerator();
	cgen.setPackageName(packageName);
	cgen.setVersion(packageVersion);
	cgen.setSection(dc.getSection());
	cgen.setDependencies(dependencyLine);
	cgen.setMaintainer(dc.getMaintainer());
	cgen.setShortDescription(ph.getProjectDescription());
	cgen.setDescription(ph.getProjectDescription());
	cgen.setArchitecture(dc.getArchitecture());
	cgen.setInstalledSize(getInstalledSize(byteAmount));
	    
    l.info("creating control file: " + controlFile.getAbsolutePath());
    Utils.createFile(controlFile, "control");
    
    try
      {
        cgen.generate(controlFile);
      }
    catch (IOException ioe)
      {
        throw new MojoExecutionException("IOException while creating control file.",
                                         ioe);
      }

  }

  private void createPackage(Log l, Packaging.Helper ph, File base) throws MojoExecutionException
  {
    l.info("calling dpkg-deb to create binary package");
    
    Utils.exec(new String[] {"dpkg-deb",
                             "--build",
                             base.getName(),
                             ph.getOutputDirectory().getAbsolutePath() },
                ph.getTempRoot(),
                "'dpkg --build' failed.",
                "Error creating the .deb file.");
  }
 
 /**
   * Convert the artifactId into a Debian package name which contains
   * gcj precompiled binaries.
   * 
   * @param artifactId
   * @return
   */
  private String gcjise(String artifactId, String section)
  {
    return section.equals("libs") ? "lib" + artifactId + "-gcj"
                                        : artifactId + "-gcj";
    
  }

  /** Converts a byte amount to the unit used by the Debian control file
   * (usually KiB). That value can then be used in a ControlFileGenerator
   * instance.
   * 
   * @param byteAmount
   * @return
   */
  private long getInstalledSize(long byteAmount)
  {
    return byteAmount / 1024L;
  }


}
