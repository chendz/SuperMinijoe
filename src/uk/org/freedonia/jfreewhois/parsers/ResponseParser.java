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
package uk.org.freedonia.jfreewhois.parsers;

/**
 * ResponseParsers read the response to the whois query one line at a time and parse that line for 
 * information.
 * @author Joe Beeton
 *
 */
public interface ResponseParser {

	/**
	 * The Whois result is pushed into the implementations of parseLine to extract information.
	 * @param line
	 */
	public void parseLine( final String line );
	
}
