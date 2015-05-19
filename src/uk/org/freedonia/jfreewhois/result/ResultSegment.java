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
package uk.org.freedonia.jfreewhois.result;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * ResultSegment contains information about a segment of the whois response. The ResultSegment contains both a 
 * a title and value. E.g Domain Name:BBC.MOBI .
 * In this case the heading would be Domain Name and the value would be BBC.MOBI
 * @author Joe Beeton
 *
 */
public class ResultSegment {

	private String heading = null;
	private List<String> values = new ArrayList<String>();
	private ResultSegment segment = null;
	private int indent = 0;
	
	/**
	 * Constructs a new ResultSegment.
	 * @param heading the name of the heading.
	 * @param indent The level of indentation.
	 * @param value the value(s).
	 */
	public ResultSegment( final String heading, final int indent, final String ...value ) {
		this.setHeading(heading);
		this.setIndent(indent);
		this.values.addAll( Arrays.asList( value ) );
	}
	
	/**
	 * Constructs a new ResultSegment.
	 * @param heading the name of the heading.
	 * @param indent The level of indentation.
	 */
	public ResultSegment( final String heading, final int indent ) {
		this.setHeading(heading);
		this.setIndent(indent);
	}

	/**
	 * Constructs a new ResultSegment.
	 * @param heading the name of the heading.
	 * @param indent The level of indentation.
	 * @param segment the sub ResultSegment.
	 */
	public ResultSegment( final String heading, final int indent, final ResultSegment segment ) {
		this.setHeading(heading);
		this.setIndent(indent);
		this.setSegment(segment);
	}
	
	/**
	 * Constructs a new ResultSegment.
	 * @param heading the name of the heading.
	 * @param indent The level of indentation.
	 * @param segment the sub ResultSegment
	 * @param value the value(s).
	 */
	public ResultSegment( final String heading,  final int indent, final ResultSegment segment,   final String ...value ) {
		this.setHeading(heading);
		this.values.addAll( Arrays.asList( value ) );
		this.setSegment(segment);
		this.setIndent(indent);
	}

	/**
	 * Returns the Heading.
	 * @return the Heading.
	 */
	public String getHeading() {
		if( heading != null ) {
			return heading.trim();
		} else {
			return heading;
		}
	}

	/**
	 * Sets the Heading.
	 * @param heading the value the heading will be set to.
	 */
	public void setHeading( final String heading ) {
		if( heading != null ) {
			this.heading = heading.trim();
		} else {
			this.heading = heading;
		}
	}

	/**
	 * Returns the values.
	 * @return the values.
	 */
	public List<String> getValue() {
		return values;
	}

	/**
	 * Adds a line to the List of Values.
	 * @param value the value to be added to the List of values.
	 */
	public void addLine( final String value ) {
		if( value != null ) {
			this.values.add( value.trim() );
		} else {
			this.values.add( value );
		}
	}

	/**
	 * Returns the child ResultSegment.
	 * @return the child ResultSegment.
	 */
	public ResultSegment getSegment() {
		return segment;
	}

	/**
	 * Sets the child ResultSegment
	 * @param segment the value the child ResultSegment to be set to.
	 */
	public void setSegment( final ResultSegment segment ) {
		this.segment = segment;
	}

	/**
	 * Returns the Indent.
	 * @return the level of indentation.
	 */
	public int getIndent() {
		return indent;
	}

	/**
	 * Sets the level of indentation
	 * @param indent the value the indent to be set to.
	 */
	public void setIndent( final int indent ) {
		this.indent = indent;
	}
	
	@Override
	public String toString() {
		StringBuilder msg = new StringBuilder();
		msg.append(getHeading()+" ");
		for( String line : getValue() ) {
			msg.append(line+"\r");
		}
		return msg.toString();
	}
	
	
}
