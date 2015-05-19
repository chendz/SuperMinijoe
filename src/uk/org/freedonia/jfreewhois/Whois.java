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

import java.util.HashMap;
import java.util.List;

import uk.org.freedonia.jfreewhois.exceptions.HostNameValidationException;
import uk.org.freedonia.jfreewhois.exceptions.WhoisException;
import uk.org.freedonia.jfreewhois.list.WhoisServerDefinition;
import uk.org.freedonia.jfreewhois.parsers.RawResponseParser;
import uk.org.freedonia.jfreewhois.parsers.ResponseParser;
import uk.org.freedonia.jfreewhois.parsers.ResultSegmentParser;
import uk.org.freedonia.jfreewhois.parsers.VersionSegmentParser;
import uk.org.freedonia.jfreewhois.result.ResultSegment;

/**
 * Whois is a utility class used to give easy access to the JFreeWhois Library.
 * @author Joe Beeton
 *
 */
public class Whois {
	
	/**
	 * Makes a whois request and returns a List of ResultSegments representing the results of the whois query, using the default
	 * whois server for domain the hostname belongs to.
	 * @param hostName the hostname to be queried.
	 * @return a List of ResultSegments representing the results of the query.
	 * @throws WhoisException if a error occurs while running the query.
	 * @throws HostNameValidationException if the hostname is invalid.
	 */
	public static List<ResultSegment> getWhoisResults( final String hostName ) throws WhoisException, HostNameValidationException {
		ResultSegmentParser parser = new ResultSegmentParser();		
		WhoisRunner runner = new WhoisRunner( parser );
		runner.runWhoisQuery(hostName);
		return parser.getResults();
	}
	
	/**
	 * Makes a whois request and returns a List of ResultSegments representing the results of the whois query.
	 * @param hostName the hostname to be queried.
	 * @param serverDefinition the definition of the whois server to be queried.
	 * @return a List of ResultSegments representing the results of the query.
	 * @throws WhoisException if a error occurs while running the query.
	 * @throws HostNameValidationException if the hostname is invalid.
	 */
	public static List<ResultSegment> getWhoisResults( final String hostName, final WhoisServerDefinition serverDefinition ) throws WhoisException, HostNameValidationException {
		ResultSegmentParser parser = new ResultSegmentParser();		
		WhoisRunner runner = new WhoisRunner( parser );
		runner.runWhoisQuery( hostName, serverDefinition );
		return parser.getResults();
	}
	
	/**
	 * Makes a whois request and pushes the results of that query through the specified ResponseParsers.
	 * @param hostName the hostname to be queried.
	 * @param parsers the parsers which will be used to read the results of the whois query.
	 * @throws WhoisException if a error occurs while running the query.
	 * @throws HostNameValidationException if the hostname is invalid.
	 */
	public static void getWhoisResults( final String hostName, ResponseParser... parsers ) throws WhoisException, HostNameValidationException {
		new WhoisRunner( parsers ).runWhoisQuery( hostName );
	}
	
	/**
	 * Makes a whois request and pushes the results of that query through the specified ResponseParsers.
	 * @param hostName the hostname to be queried.
	 * @param parsers the parsers which will be used to read the results of the whois query.
 	 * @param serverDefinition the definition of the whois server to be queried.
	 * @throws WhoisException if a error occurs while running the query.
	 * @throws HostNameValidationException if the hostname is invalid.
	 */
	public static void getWhoisResults( final String hostName, final WhoisServerDefinition serverDefinition, final ResponseParser... parsers ) throws WhoisException, HostNameValidationException {
		new WhoisRunner( parsers ).runWhoisQuery( hostName, serverDefinition );
	}
	
	/**
	 * Makes a whois request and returns the raw whois response.
	 * @param hostName the hostname to be queried.
	 * @return the raw whois response.
	 * @throws WhoisException if a error occurs while running the query.
	 * @throws HostNameValidationException if the hostname is invalid.
	 */
	public static String getRawWhoisResults( final String hostName ) throws WhoisException, HostNameValidationException {
		RawResponseParser parser = new RawResponseParser();		
		WhoisRunner runner = new WhoisRunner( parser );
		runner.runWhoisQuery(hostName);
		return parser.getRawResponse();
	}
	
	/**
	 * Makes a whois request and returns the raw whois response.
	 * @param hostName the hostname to be queried.
	 * @param serverDefinition the definition of the whois server to be queried.
	 * @return the raw whois response.
	 * @throws WhoisException if a error occurs while running the query.
	 * @throws HostNameValidationException if the hostname is invalid.
	 */
	public static String getRawWhoisResults( final String hostName, final WhoisServerDefinition serverDefinition ) throws WhoisException, HostNameValidationException {
		RawResponseParser parser = new RawResponseParser();		
		WhoisRunner runner = new WhoisRunner( parser );
		runner.runWhoisQuery( hostName, serverDefinition );
		return parser.getRawResponse();
	}
	
	
	/**
	 * Makes a whois request and returns a result segment containing the version of the whois server.
	 * @param hostName the hostname to be queried.
	 * @return the version of the whois server.
	 * @throws WhoisException if a error occurs while running the query.
	 * @throws HostNameValidationException if the hostname is invalid.
	 */
	public static ResultSegment getServerVersion( final String hostName ) throws WhoisException, HostNameValidationException {
		VersionSegmentParser parser = new VersionSegmentParser();		
		WhoisRunner runner = new WhoisRunner( parser );
		runner.runWhoisQuery( hostName );
		return parser.getServerVersion();
	}

	/**
	 * Makes a whois request and returns a result segment containing the version of the whois server.
	 * @param hostName the hostname to be queried.
	 * @param serverDefinition the definition of the whois server to be queried.
	 * @return the version of the whois server.
	 * @throws WhoisException if a error occurs while running the query.
	 * @throws HostNameValidationException if the hostname is invalid.
	 */
	public static ResultSegment getServerVersion( final String hostName, final WhoisServerDefinition serverDefinition ) throws WhoisException, HostNameValidationException {
		VersionSegmentParser parser = new VersionSegmentParser();		
		WhoisRunner runner = new WhoisRunner( parser );
		runner.runWhoisQuery( hostName, serverDefinition );
		return parser.getServerVersion();
	}
	
	/**
	 * Makes a whois request and returns a HashMap containing all of the default result parsers.
	 * @param hostName the hostname to be queried.
	 * @return a hashmap containing the default ResposeParsers.
	 * @throws WhoisException if a error occurs while running the query.
	 * @throws HostNameValidationException if the hostname is invalid.
	 */
	public static HashMap<Class<? extends ResponseParser>, ResponseParser> getWhoisResultsWithDefaultParsers( final String hostName ) throws WhoisException, HostNameValidationException {
		WhoisRunner runner = new WhoisRunner();
		runner.runWhoisQuery( hostName );
		return runner.getParsers();
	}
	
	/**
	 * Makes a whois request and returns a HashMap containing all of the default result parsers.
	 * @param hostName the hostname to be queried.
	 * @param serverDefinition the definition of the whois server to be queried.
	 * @return a hashmap containing the default ResposeParsers.
	 * @throws WhoisException if a error occurs while running the query.
	 * @throws HostNameValidationException if the hostname is invalid.
	 */
	public static HashMap<Class<? extends ResponseParser>, ResponseParser> getWhoisResultsWithDefaultParsers( final String hostName, final WhoisServerDefinition serverDefinition ) throws WhoisException, HostNameValidationException {
		WhoisRunner runner = new WhoisRunner();
		runner.runWhoisQuery( hostName, serverDefinition );
		return runner.getParsers();
	}

}
