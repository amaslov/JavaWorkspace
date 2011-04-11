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
public interface VirtualFile
{
	/**
	 * Returns the fileName.
	 * @return String
	 */
	public String getFileName();

	/**
	 * Returns the fileSize.
	 * @return long
	 */
	public long getFileSize();

	/**
	 * Returns the timeout.
	 * @return int
	 */
	public int getTimeout();

	/**
	 * Sets the fileName.
	 * @param fileName The fileName to set
	 */
	public void setFileName(String fileName);

	/**
	 * Sets the fileSize.
	 * @param fileSize The fileSize to set
	 */
	public void setFileSize(long fileSize);

	/**
	 * Sets the timeout.
	 * @param timeout The timeout to set
	 */
	public void setTimeout(int timeout);

}
