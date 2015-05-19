/*******************************************************************************
 * Copyright (c) 2013 Joe Beeton.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Joe Beeton - initial API and implementation
 ******************************************************************************/
package uk.org.freedonia.jfreewhois;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import uk.org.freedonia.jfreewhois.exceptions.WhoisException;
import uk.org.freedonia.jfreewhois.list.WhoisServerList;

/**
 * 
 * WhoisLister is used to return the full list of Whois Servers from the serverlist.xml
 * @author Joe Beeton
 *
 */
public class ServerLister {

	
	private static WhoisServerList serverList = null;
	
	private static final String serverListPath = "/uk/org/freedonia/jfreewhois/etc/serverlist.xml";
	
	/**
	 * The System property to set if you wish to use a non default server list.
	 */
	public static final String SERVER_PATH_KEY = "uk.org.freedonia.jfreewhois.serverlist";
	
	/** 
	 * Returns the full list of whois servers. If this is the first time this is called then the server list is loaded from
	 * the xml and stored in memory. All subsequent calls use the stored list.
	 * @return the full list of whois servers.
	 * @throws WhoisException if an error occurs while reading the server list from xml.
	 */
	public static WhoisServerList getServerList() throws WhoisException {
		if( serverList == null ) {
			loadServerList();
		}
		return serverList;
	}
	
	/**
	 * Loads the server list.
	 * @throws WhoisException if the xml server list could not be read.
	 */
	private static void loadServerList() throws WhoisException {
		InputStream stream = null;
		try {
			stream = getServerListAsStream();
			serverList = getServerList( stream );
		} catch ( FileNotFoundException e ) {
			throw new WhoisException( e );
		} finally {
			if ( stream != null ) {
				try {
					stream.close();
				} catch (IOException e) {
				}
			}
		}
	}
	
	/**
	 * Takes the path to the server list xml file and returns it as a InputStream.
	 * @return a InputStream to the xml server list.
	 * @throws FileNotFoundException if the server list xml cannot be found.
	 */
	private static InputStream getServerListAsStream() throws FileNotFoundException {
		String listFilePath = System.getProperty( SERVER_PATH_KEY, serverListPath );
		if( new File( listFilePath ).exists() ) {
			return new FileInputStream( new File( listFilePath ) );
		} else {
			return ClassLoader.class.getResourceAsStream( listFilePath );
		}
	}
	
	/**
	 * Reads the XML Server List from the specified InputStream and unmarshals it into a WhoisServerList object.
	 * @param inputStream the inputstream pointing to the server list xml.
	 * @return a WhoisList unmarshalled from the server xml.
	 * @throws WhoisException if a error occurs while reading the xml.
	 */
	private static WhoisServerList getServerList( final InputStream inputStream ) throws WhoisException {
		
		Serializer serializer = new Persister();
		try {
			return serializer.read( WhoisServerList.class, inputStream );
		} catch (Exception e) {
			e.printStackTrace();
			throw new WhoisException( e );
		}
		
	}
	
	
}
