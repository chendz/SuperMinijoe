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

/**
 * A Exception that is thrown if the hostname is not valid. 
 * @author Joe Beeton
 *
 */
public class HostNameValidationException extends Exception {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1030967352620716902L;
	
	/**
	 * Constructs a HostNameValidationException with the specified 
	 * message.
	 * @param msg the message to be included in the Exception.
	 */
	public HostNameValidationException( final String msg ) {
		super( msg );
	}
	
	/**
	 * Constructs a HostNameValidationException with the specified 
	 * Throwable.
	 * @param t the throwable to be included in the Exception.
	 */
	public HostNameValidationException( final Throwable t ) {
		super( t ); 
	}
	
	/**
	 * Constructs a HostNameValidationException with the specified 
	 * message and throwable.
	 * @param msg the message to be included in the Exception.
	 * @param t the throwable to be included in the Exception.
	 */
	public HostNameValidationException( final String msg, final Throwable t ) {
		super( msg, t );
	}


}
