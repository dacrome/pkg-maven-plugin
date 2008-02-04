package de.tarent.maven.plugins.pkg.packager;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import de.tarent.maven.plugins.pkg.DistroConfiguration;
import de.tarent.maven.plugins.pkg.PackageMap;

public abstract class Packager
{
  
  public abstract void execute(Log l,
                               PackagerHelper ph,
                               DistroConfiguration distroConfig,
                               PackageMap packageMap) throws MojoExecutionException;
  
  public abstract void checkEnvironment(Log l, DistroConfiguration dc) throws MojoExecutionException;
                      
}