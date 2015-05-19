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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

import uk.org.freedonia.jfreewhois.exceptions.HostNameValidationException;
import uk.org.freedonia.jfreewhois.exceptions.WhoisException;
import uk.org.freedonia.jfreewhois.list.WhoisServerDefinition;
import uk.org.freedonia.jfreewhois.list.WhoisServerList;

/**
 * ServerDefinitionFinder is used to find a WhoisServerDefinition for a specified host name.
 * @author Joe Beeton
 *
 */
public class ServerDefinitionFinder {

	
	/**
	 * finds the list of WhoisServerDefinitions that can resolve the specified host name.
	 * @param hostName the domain name to be resolved.
	 * @return a list of WhoisServerDefinitions that can resolve the specified host name.
	 * @throws WhoisException if the XML list of server definitions cannot be read for some reason.
	 * @throws HostNameValidationException if the host name is null or if the hostname is not valid.
	 */
	public List<WhoisServerDefinition> getServerDefinitionsForHostName( final String hostName ) throws WhoisException, HostNameValidationException {
		WhoisServerList list = getServerList();
		List<WhoisServerDefinition> possibleServerList = list.getWhoisServerDefinitions();
		String[] hostNameArray = reverseHostNameArray( getHostNameAsStringArray( hostName ) );
		List<WhoisServerDefinition> matchedList =  getServerDefinitionsForHostName( hostNameArray,  possibleServerList );
		Hashtable<Integer, List<WhoisServerDefinition>> defsBySuitablity = new Hashtable<Integer, List<WhoisServerDefinition>>();
		for ( WhoisServerDefinition def : matchedList ) {
			int level = 0;
			for ( String tld : def.getNameTld() ) {
				int tldLevel = getSuitablity( hostNameArray, reverseHostNameArray( getHostNameAsStringArray( tld ) ) );
				if ( tldLevel > level ) {
					level = tldLevel;
				}
			}
			if ( defsBySuitablity.containsKey( level ) ) {
				defsBySuitablity.get( level ).add( def );
			} else {
				List<WhoisServerDefinition> lst = new ArrayList<WhoisServerDefinition>();
				lst.add(def);
				defsBySuitablity.put( level, lst );
			}
		}
		List<WhoisServerDefinition> sortedList = new ArrayList<WhoisServerDefinition>();
		Integer[] sortedKeys = defsBySuitablity.keySet().toArray( new Integer[]{} );
		Arrays.sort( sortedKeys, Collections.reverseOrder() );
		for ( Integer key : sortedKeys ) {
			sortedList.addAll( defsBySuitablity.get( key ) );
		}
		return sortedList;
	}
	
	
	

	/**
	 * returns the suitability of the specified hostname.
	 * @param hostNameArray
	 * @param reverseHostNameArray
	 * @return
	 */
	private int getSuitablity( String[] hostNameArray,
			String[] reverseHostNameArray ) {
		int level = 0;
		String[] first;
		String[] second;
		if( hostNameArray.length <= reverseHostNameArray.length ) {
			first = hostNameArray;
			second = reverseHostNameArray;
		} else {
			first = reverseHostNameArray;
			second = hostNameArray;
		}
		for( int i = 0 ; i < first.length; i++ ) {
			if( first[i].equals(second [i] ) ) {
				level++;
			} else {
				break;
			}
		}
		return level;
	}


	/**
	 * takes the reveresed host name and list of possible servers and returns a sub list containing only those 
	 * whois servers that match.
	 * @param reversedHostName
	 * @param possibleServerList
	 * @return
	 * @throws HostNameValidationException
	 */
	protected List<WhoisServerDefinition> getServerDefinitionsForHostName( final String[] reversedHostName, 
			final List<WhoisServerDefinition> possibleServerList ) throws HostNameValidationException {
		List<WhoisServerDefinition> matchedServers = new ArrayList<WhoisServerDefinition>();
		for( WhoisServerDefinition def : possibleServerList ) {
			for( String tld : def.getNameTld() ) {
				String[] tldHostName = reverseHostNameArray( getHostNameAsStringArray( tld ) );
				if( isTLDsMatch( tldHostName, reversedHostName ) ) {
					matchedServers.add( def );
					break;
				}
			}
		}
		return matchedServers;
	}
	
	/**
	 * takes the two reveresed host nanes and returns true if they match each other. Else returns false.
	 * @param tld1
	 * @param tld2
	 * @return
	 */
	protected boolean isTLDsMatch( String[] tld1, String[] tld2 ) {
		String[] first;
		String[] second;
		if( tld1.length <= tld2.length ) {
			first = tld1;
			second = tld2;
		} else {
			first = tld2;
			second = tld1;
		}
		boolean matches = true;
		for( int i = 0; i < first.length; i++ ) {
			if( !first[i].equals(second[i] ) ) {
				matches = false;
				break;
			}
		}
		return matches;
		
	}
	
	/**
	 * Returns the list of possible whois servers.
	 * @return
	 * @throws WhoisException
	 */
	private WhoisServerList getServerList() throws WhoisException {
		return ServerLister.getServerList();
	}
	
	/**
	 * Takes the specified host name and returns a array containing the parts of the hostname in reverse order e.g www.google.com would
	 * become com.google.www
	 * @param hostNames the host name to be reversed
	 * @return the reversed host name.
	 */
	protected String[] reverseHostNameArray( final String[] hostNames ) {
		String[] reversedArray = new String[hostNames.length];
		int reversedIndex = hostNames.length -1;
		for( int i = 0 ; i < hostNames.length; i++ ) {
			reversedArray[reversedIndex] = hostNames[i];
			reversedIndex--;
		}
		return reversedArray;
	}
	
	/**
	 * Takes the specified host name and converts it into a string array.
	 * @param hostName
	 * @return
	 * @throws HostNameValidationException if the host name is null.
	 */
	protected String[] getHostNameAsStringArray( final String hostName ) throws HostNameValidationException {
	    if( hostName == null ) {
	    	throw new HostNameValidationException("Hostname cannot be null." );
	    } else if( !hostName.contains(".") ) {
			return new String[]{hostName};
		} else {
			return hostName.split("\\.");
		}
	}
	
	
}
