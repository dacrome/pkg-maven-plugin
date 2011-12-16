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

/**
 * 
 */
package de.tarent.maven.plugins.pkg.upload;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

import de.tarent.maven.plugins.pkg.AbstractPackagingMojo;
import de.tarent.maven.plugins.pkg.Utils;
import de.tarent.maven.plugins.pkg.WorkspaceSession;
import de.tarent.maven.plugins.pkg.map.PackageMap;

/**
 * 
 * Uploads packages generated in the package-phase to an APT-repository.
 * 
 * @author Fabian K&ouml;ster (f.koester@tarent.de) tarent GmbH Bonn
 * 
 * @goal deploy
 */
public class APTUploader extends AbstractPackagingMojo {
	
	/**
	 * Defines the Repository-ID to use for dupload. For use on the command-line. 
	 * 
	 * @parameter expression="${repo}"
	 */
	protected String repo;
	
	protected PackageMap packageMap;
	protected String packagingType;
	
	/**
	 * The command to use for uploading packages
	 */
	protected final String uploadCmd = "dupload";
	

	/**
	 * @see org.apache.maven.plugin.Mojo#execute()
	 */
	public void execute() throws MojoExecutionException, MojoFailureException {
		if(!getPackagingType().equals("deb")) {
			throw new MojoFailureException("Uploading packaging-type '"+ getPackagingType() + "' is currently not supported.");
		}
		checkEnvironment(getLog());
		
		uploadPackage(getLog(), buildDir);
	}
	
	protected void checkEnvironment(Log l) throws MojoExecutionException {
	    Utils.checkProgramAvailability("dupload");
	}
	
	/**
	 * Calls the command defined in the <code>uploadCmd</code>-variable to upload the package to
	 * a APT-Repository. The APT-repository is defined in the dupload-configuration (Probably located in /etc/dupload.conf).
	 * 
	 * If you do not want to use the standard-repository which is configured int dupload's $default_host-varible, you can specify
	 * an other repository using the 'repo'-variable.
	 * 
	 * @param l
	 * @param base
	 * @throws MojoExecutionException
	 */
	protected void uploadPackage(Log l, File base) throws MojoExecutionException  {
		l.info("calling " + uploadCmd + " to upload package");
		
		String[] command;
		
		if(repo != null && repo.length() > 0) {
			command = new String[] { uploadCmd, "--to", repo };
		} else {
			command = new String[] { uploadCmd };
		}
		Utils.exec(command ,
				base,
				"Uploading package failed.",
		"Error while uploading package.");
		
		l.info("package uploaded sucessfully.");
	}

	/**
	 * TODO same functionality as in DebianSigner.getPackageMap(). Refactoring needed.
	 * @return
	 */
	public PackageMap getPackageMap() {
		if(packageMap == null) {
			packageMap = ((PackageMap)getPluginContext().get("pm"));
		}
		return packageMap;
	}
	
	/**
	 * TODO same functionality as in DebianSigner.getPackagingType(). Refactoring needed.
	 * @return
	 */
	public String getPackagingType() {
		if(packagingType == null) {
			packagingType = getPackageMap().getPackaging();
		}
		return packagingType;
	}

	@Override
	protected void executeTargetConfiguration(
			WorkspaceSession workspaceSession, String distro)
			throws MojoExecutionException, MojoFailureException {
		// TODO: Sign- and Upload is being unser rework!
		throw new UnsupportedOperationException("does not work!");
	}
}
