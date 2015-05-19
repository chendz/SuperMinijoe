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
package uk.org.freedonia.jfreewhois.exceptions;

public class WhoisException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -798120777500139941L;

	
	/**
	 * Constructs a WhoisException with the specified 
	 * message.
	 * @param msg the message to be included in the Exception.
	 */
	public WhoisException( final String msg ) {
		super( msg );
	}
	
	/**
	 * Constructs a WhoisException with the specified 
	 * Throwable.
	 * @param t the throwable to be included in the Exception.
	 */
	public WhoisException( final Throwable t ) {
		super( t ); 
	}
	
	/**
	 * Constructs a WhoisException with the specified 
	 * message and throwable.
	 * @param msg the message to be included in the Exception.
	 * @param t the throwable to be included in the Exception.
	 */
	public WhoisException( final String msg, final Throwable t ) {
		super( msg, t );
	}
}
