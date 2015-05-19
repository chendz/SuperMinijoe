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

import java.util.ArrayList;
import java.util.List;

/**
 * The RawResponseParser is a ResponseParser implementation which is used to store the entire whois response.
 * @author Joe Beeton
 *
 */
public class RawResponseParser implements ResponseParser {

	/**
	 * The List which contains the lines of the whois response.
	 */
	private List<String> data = new ArrayList<String>();
	
	@Override
	public void parseLine(String line) {
		data.add( line );
	}
	
	/**
	 * Returns the raw whois response.
	 * @return the whois response.
	 */
	public String getRawResponse() {
		return formatResponse();
	}
	
	/**
	 * Formats and returns the whois response.
	 * @return the whois response.
	 */
	private String formatResponse() {
		StringBuilder msg = new StringBuilder();
		for( String line : data ) {
			msg.append(line + "\r" );
		}
		return msg.toString();
	}

}
