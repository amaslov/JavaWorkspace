/**
 * (c) Melexis Telecom and/or Remote Operating Services B.V.
 * 
 * Distributable under LGPL license
 * See terms of license at gnu.org
 */
package com.globalros.tftp.common;

/**
 * @author marco
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class TFTPException extends Exception
{

	/**
	 * Constructor for TFTPException.
	 */
	public TFTPException()
	{
		super();
	}

	/**
	 * Constructor for TFTPException.
	 * @param message
	 */
	public TFTPException(String message)
	{
		super(message);
	}

	/**
	 * Constructor for TFTPException.
	 * @param message
	 * @param cause
	 */
	public TFTPException(String message, Throwable cause)
	{
		super(message, cause);
	}

	/**
	 * Constructor for TFTPException.
	 * @param cause
	 */
	public TFTPException(Throwable cause)
	{
		super(cause);
	}   
}
