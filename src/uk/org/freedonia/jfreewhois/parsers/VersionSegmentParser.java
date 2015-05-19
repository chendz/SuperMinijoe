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

import uk.org.freedonia.jfreewhois.result.ResultSegment;

/**
 * The VersionSegmentParser is used to find the version of the whois server that is being queried.
 * @author Joe Beeton
 *
 */
public class VersionSegmentParser implements ResponseParser {

	private boolean isVersion2 = false;
	
	/**
	 * Returns the Server Version as a ResultSegment. The version will be either version 1.0 or 2.0
	 * @return
	 */
	public ResultSegment getServerVersion() {
		ResultSegment segment = new ResultSegment( "Whois Server Version:", 0 );
		if( isVersion2 ) {
			segment.addLine( "2.0" );
		} else {
			segment.addLine( "1.0" );
		}
		return segment;
	}
	
	@Override
	public void parseLine(String line) {
		if( line.trim().equals("Whois Server Version 2.0") ) {
			isVersion2 = true;
		}
		
	}

}
