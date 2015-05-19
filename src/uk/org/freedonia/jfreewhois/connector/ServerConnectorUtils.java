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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import uk.org.freedonia.jfreewhois.list.WhoisServerDefinition;


/**
 * 
 * This is a utility class containing methods for the ServerConnector. This class mostly exists to make it easy to mock
 * connections to whois servers.
 * @author Joe Beeton
 *
 */
public class ServerConnectorUtils {

	/**
	 * Constructs and returns a Socket connection to the server.
	 * @param serverDef the Server Definition used to connect to the server.
	 * @param the TCP port the connection should be made to.
	 * @return a Socket connection to the whois server.
	 * @throws UnknownHostException if the host cannot be found.
	 * @throws IOException if a error occurs while connecting to the server.
	 */
	public static Socket getConnectionToServer( final WhoisServerDefinition serverDef, int port ) throws UnknownHostException, IOException {
		return new Socket( serverDef.getServerAddress(), port );
	}
	
	/**
	 * Returns a InputStream from the specified socket
	 * @param socket the socket the inputstream is connected to.
	 * @return the input stream connected to the specified socket.
	 * @throws IOException if a error occurs while connecting to the server.
	 */
	public static InputStream getInputStreamFromSocket(  final Socket socket) throws IOException {
		return socket.getInputStream();
	}
	
	/**
	 * Returns a OutputStream from the specified socket
	 * @param socket the socket the OutputStream is connected to.
	 * @return the OutputStream connected to the specified socket.
	 * @throws IOException if a error occurs while connecting to the server.
	 */
	public static OutputStream getOutputStreamFromSocket(  final Socket socket) throws IOException {
		return socket.getOutputStream();
	}
	
	
}
