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

import uk.org.freedonia.jfreewhois.result.ResultSegment;



/**
 * ResultSegmentParser parses the whois result line by line and builds up a list of result segments containing the information 
 * returned from the whois query.
 * @author Joe Beeton
 *
 */
public class ResultSegmentParser implements ResponseParser {

	private List<ResultSegment> results = new ArrayList<ResultSegment>();
	
	private boolean isSegmentActive = false;
	private ResultSegment parentSegment = null;
	
	public static final String TOKEN_CHAR = ":";
	
	
	@Override
	public void parseLine( final String line ) {
		if( !isSegmentActive && isLineAHeader( line ) ) {
			if( isValueOnSameLineAsHeading( line ) ) {
				activateSegment( getOneLineSegment( line ) );
			} else {
				activateSegment( getOnlyHeadingSegment( line ) );
			}
		} else if( isSegmentActive && !isLineAHeader( line ) ) {
			if( !line.trim().isEmpty() )  {
				parentSegment.addLine(line);
			} else {
				flushSegment();
			}
		} else if( isSegmentActive && isLineAHeader( line ) ) {
			flushSegment();
			if( isValueOnSameLineAsHeading( line ) ) {
				activateSegment( getOneLineSegment( line ) );
			} else {
				activateSegment( getOnlyHeadingSegment( line ) );
			}
		}
	}
	
	/**
	 * Sets the specified ResultSegment to be the parent segment and sets the isSegmentActive switch to true.
	 * @param resultSegment the segment to be made active
	 */
	private void activateSegment( final ResultSegment resultSegment ) {
		parentSegment = resultSegment;
		isSegmentActive = true;
	}
	
	/**
	 * Sets teh isSegmentActive switch to false and flushes the current parent segment to the results list.
	 */
	private void flushSegment() {
		isSegmentActive = false;
		results.add(parentSegment);
	}
	
	
	/**
	 * Returns the list of result segments.
	 * @return the list of result segments.
	 */
	public List<ResultSegment> getResults() {
		if( isSegmentActive ) {
			results.add(parentSegment);
			isSegmentActive = false;
		}
		return results;
	}
	
	/**
	 * Returns true if the specified line contains a Result Segment header.
	 * @param line the line to be checked.
	 * @return true if the line is a header.
	 */
	private boolean isLineAHeader( final String line ) {
		if( line.contains( TOKEN_CHAR ) && countTokenChars( line ) == 1 && !line.startsWith("%") && 
				isLineStartingWithUpperCase( line ) ) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Returns the amount of TOKEN CHARS used to delimit the header from the text.
	 * @param line the line to be counted.
	 * @return the amount of tokens in the line.
	 */
	private int countTokenChars( final String line ) {
		int tokenCount = 0;
		int i = 0;
		for( char c : line.toCharArray() ) {
			if( TOKEN_CHAR.contains( String.valueOf(c) ) ) {
				if( !isNextAndPreviousCharDigit( line, i )
						) {
					if( !isNextTwoCharsForwardSlash( line, i ) ) {
						tokenCount++;
					}
				}
			}
			i++;
		}
		return tokenCount;
	}
	
	/**
	 * Returns true if the next two characters to the character at the specified index is two forward slashes '//'.
	 * This is done to differentiate between the ':' used to split the header from the text to a URL containing a protocol e.g 
	 * Referral URL: http://domainhelp.opensrs.net .
	 * @param line the line of text to check
	 * @param index the location of the token char 
	 * @return true if the next two characters along from the character at the specified index are forward slashes.
	 */
	private boolean isNextTwoCharsForwardSlash( final String line, final int index ) {
		return ( new Character('/').equals( getNextCharInLine( line, index ) ) && new Character('/').equals( getNextCharInLine( line, index+1 ) ) );
	}
	
	/**
	 * Returns true if the previous and next characters to the character at the specified index are numbers.
	 * This is done so that the ':' within the time stamps are not confused with the ':' used to split the header from the text. For example the line
	 * Created On:12-Jun-2006 14:01:31 UTC.
	 * @param line the line of text to check
	 * @param index the location of the token char 
	 * @return true if the characters before and after the character at the specified index are digits 
	 */
	private boolean isNextAndPreviousCharDigit( final String line, final int index ) {
		return ( Character.isDigit( getPreviousCharInLine( line, index ) ) && Character.isDigit( getNextCharInLine( line, index ) ) );
	}
	
	
	/**
	 * Returns the previous character in the line to the character at the specified index.
	 * @param line
	 * @param index
	 * @return
	 */
	private Character getPreviousCharInLine( final String line, int index ) {
		int location = index -1;
		if(line.length()-1 < location) {
			return null;
		} else {
			return line.charAt(location);
		}
	}
	
	/**
	 * Returhns the next character in the line to the character at the specified index.
	 * @param line
	 * @param index
	 * @return
	 */
	private Character getNextCharInLine( final String line, int index ) {
		int location = index +1;
		if(line.length()-1 < location) {
			return null;
		} else {
			return line.charAt(location);
		}
	}
	
	/**
	 * Returns true if the specified line starts with (not including whitespace) a uppercase character.
	 * @param line
	 * @return
	 */
	private boolean isLineStartingWithUpperCase( final String line ) {
		if( line.trim().isEmpty() ) {
			return false;
		} else if( Character.isUpperCase(line.trim().charAt(0) ) ) {
			 return true;
		} else if( line.trim().startsWith(">>>") && line.trim().endsWith("<<<") ) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Returns true if the value is on the same line as the header.
	 * @param line
	 * @return
	 */
	private boolean isValueOnSameLineAsHeading( final String line ) {
		if(line.trim().endsWith( TOKEN_CHAR ) ) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Returns the amount of whitespace upto the first non whitespace character.
	 * @param line
	 * @return
	 */
	private int countWhiteSpaceUptoFirstLetter( final String line ) {
		int i = 0;
		for( char c : line.toCharArray() ) {
			if( Character.isWhitespace(c) ) {
				i++;
			} else {
				break;
			}
		}
		return i;
	}

	/**
	 * Returns a ResultSegment where the text is on the same line as the value.
	 * @param line
	 * @return
	 */
	private ResultSegment getOneLineSegment( final String line) {
		return new ResultSegment( getHeading( line ), countWhiteSpaceUptoFirstLetter(line), getValueOnSameLineAsHeading( line ) );
	}
	
	/**
	 * Returns the text value found on the specified line. This is the text after the header name is removed.
	 * @param line
	 * @return
	 */
	private String getValueOnSameLineAsHeading(String line) {
		return line.substring( line.indexOf( TOKEN_CHAR ) , line.length() );
	}

	/**
	 * Returne the header from the specified line.
	 * @param line
	 * @return
	 */
	private String getHeading( final String line ) {
		return line.substring( 0, line.indexOf( TOKEN_CHAR ) );
	}
	
	/**
	 * Returns a result segment which contains only the Header 
	 * @param line
	 * @return
	 */
	private ResultSegment getOnlyHeadingSegment( final String line ) {
		return new ResultSegment( getHeading( line ), countWhiteSpaceUptoFirstLetter(line) ); 
	}

}
