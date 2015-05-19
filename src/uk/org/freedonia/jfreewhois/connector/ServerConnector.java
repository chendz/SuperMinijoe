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
package uk.org.freedonia.jfreewhois.connector;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Collection;

import uk.org.freedonia.jfreewhois.list.WhoisServerDefinition;
import uk.org.freedonia.jfreewhois.parsers.ResponseParser;

/**
 * ServerConnector is used to connect to the whois server send the query and return a WhoisReponse.
 * @author Joe Beeton
 *
 */

public class ServerConnector {

	/** 
	 * The TCP port that the whois server listens on.
	 */
	public static final int WHOIS_PORT = 43;
	
	/**
	 * The Line separator.
	 */
	public static final String CRLF = System.getProperty("line.separator");
	
	
	
	
	public void queryServer( final WhoisServerDefinition serverDef, final Collection<ResponseParser> responseParsers, final String hostName ) throws UnknownHostException, IOException {
		Socket sock = null;
		OutputStream outStream = null;
		try {
			sock = ServerConnectorUtils.getConnectionToServer( serverDef, getWhoisPort() );
			InputStream inStream = ServerConnectorUtils.getInputStreamFromSocket( sock );
			InputStreamReader streamReader = new InputStreamReader( inStream );
			BufferedReader buffReader = new BufferedReader( streamReader );
			outStream = ServerConnectorUtils.getOutputStreamFromSocket( sock );
			sendQuery( outStream, hostName );
			String line = null;
			while( ( line = buffReader.readLine()  ) != null ) {
				for( ResponseParser responseParser : responseParsers ) {
					responseParser.parseLine( line );
				}
			}
		} finally {
			if( sock != null ) {
				sock.close();
			} if( outStream != null ) {
				outStream.close();
			}
		}
	}
	
	
	
	/**
	 * Returns the port that the whois server runs on. ( 43 ) 
	 * @return the port that the whois server runs on.
	 */
	protected int getWhoisPort() {
		return WHOIS_PORT;
	}
	

	
	/**
	 * Sends the query to the whois server.
	 * @param outStream the output stream of the whois server.
	 * @param hostName the hostname to be resolved.
	 * @throws IOException if a error occurs while querying the server.
	 */
	private void sendQuery( final OutputStream outStream, final String hostName ) throws IOException {
		OutputStreamWriter writer = null;
		BufferedWriter buffWriter = null;
		writer = new OutputStreamWriter( outStream );
		buffWriter = new BufferedWriter( writer );
		buffWriter.write( hostName+CRLF );
		buffWriter.flush();
	}
	
	
	
}
