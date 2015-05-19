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

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;

import uk.org.freedonia.jfreewhois.connector.ServerConnector;
import uk.org.freedonia.jfreewhois.exceptions.HostNameValidationException;
import uk.org.freedonia.jfreewhois.exceptions.WhoisException;
import uk.org.freedonia.jfreewhois.list.WhoisServerDefinition;
import uk.org.freedonia.jfreewhois.parsers.RawResponseParser;
import uk.org.freedonia.jfreewhois.parsers.ResponseParser;
import uk.org.freedonia.jfreewhois.parsers.ResultSegmentParser;
import uk.org.freedonia.jfreewhois.parsers.VersionSegmentParser;

/**
 * WhoisRunner is used to run whois queries.
 * @author Joe Beeton
 *
 */
public class WhoisRunner {

	private HashMap<Class<? extends ResponseParser>,  ResponseParser> parsers = new  HashMap<Class<? extends ResponseParser>,  ResponseParser>();
	
	/**
	 * Construct a WhoisRunner with the default parsers.
	 * The default Parsers are :
	 *  ResultSegmentParser
	 *  VersionSegmentParser
	 *  RawResponseParser
	 */
	public WhoisRunner() {
		loadDefaultParsers();
	}
	
	/**
	 * Constructs a WhoisRunner with the specified ReponseParsers.
	 * @param responseParsers the ResponseParsers to use with the query.
	 */
	public WhoisRunner( final ResponseParser... responseParsers ) {
		for( ResponseParser parser : responseParsers ) {
			parsers.put( parser.getClass(), parser );
		}
	}

	/**
	 * Loads the default parsers.
	 */
	protected void loadDefaultParsers() {
		parsers.put( ResultSegmentParser.class, new ResultSegmentParser() );
		parsers.put( VersionSegmentParser.class, new VersionSegmentParser() );
		parsers.put( RawResponseParser.class, new RawResponseParser() );
	}
	
	/**
	 * Runs a Whois Query against the specified host name.
	 * @param hostName the hostname to be queried.
	 * @throws WhoisException if a error occurs while running the query.
	 * @throws HostNameValidationException if the hostname is invalid.
	 */
	public void runWhoisQuery( final String hostName ) throws WhoisException, HostNameValidationException {
			runWhoisQuery( hostName, getServerDefinition( hostName ) );
	}
	
	/**
	 * Runs a Whois Query against the specified host name.
	 * @param hostName the hostname to be queried.
	 * @param serverDefinition the WhoisServerDefinition to be used in the query.
	 * @throws WhoisException if a error occurs while running the query.
	 * @throws HostNameValidationException if the hostname is invalid.
	 */
	public void runWhoisQuery( final String hostName, final WhoisServerDefinition serverDefinition ) throws WhoisException, HostNameValidationException {
		try {
			ServerConnector connector = new ServerConnector();
			connector.queryServer (serverDefinition, parsers.values(), hostName );
		} catch( UnknownHostException e ) {
			throw new HostNameValidationException( e );
		} catch( IOException e ) {
			throw new WhoisException( e );
		}
	}
	
	/**
	 * Returns the default WhoisServerDefinition for the specified hostname.
	 * @param hostName the hostname to be queried.
	 * @return a WhoisServerDefinition that can be used 
	 * @throws WhoisException if the whois server list cannot be read.
	 * @throws HostNameValidationException if the hostname is not valid or no server definition can be found for that hostname.
	 */
	public WhoisServerDefinition getServerDefinition( final String hostName ) throws WhoisException, HostNameValidationException {
		ServerDefinitionFinder serverDefFinder = new ServerDefinitionFinder();
		List<WhoisServerDefinition> potentialServerDefinitions = serverDefFinder.getServerDefinitionsForHostName( hostName );
		if( potentialServerDefinitions.isEmpty() ) {
			throw new HostNameValidationException( "Cannot find any Whois Servers for hostname :  " + hostName );
		}
		return potentialServerDefinitions.get( 0 );
	}
	
	/**
	 * Returns the hash map of response parsers.
	 * @return the hash map of response parsers.
	 */
	public HashMap<Class<? extends ResponseParser>, ResponseParser> getParsers() {
		return parsers;
	}
	
}
