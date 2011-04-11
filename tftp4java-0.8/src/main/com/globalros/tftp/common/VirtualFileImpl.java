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
public class VirtualFileImpl implements VirtualFile
{
   protected String fileName;

   // This timeout determines the amount of time a read is blockedin milisecs,
   // when zero the read is blocked until data becomes available
   protected int timeout;

   // stores the file size
   protected long fileSize;
   
	/**
	 * Constructor for VirtualFile.
	 */
   public VirtualFileImpl(String fileName)
   {
      this.fileName = fileName;
   }
   
	/**
	 * Returns the fileName.
	 * @return String
	 */
	public String getFileName()
	{
		return fileName;
	}

	/**
	 * Returns the fileSize.
	 * @return long
	 */
	public long getFileSize()
	{
		return fileSize;
	}

	/**
	 * Returns the timeout.
	 * @return int
	 */
	public int getTimeout()
	{
		return timeout;
	}

	/**
	 * Sets the fileName.
	 * @param fileName The fileName to set
	 */
	public void setFileName(String fileName)
	{
		this.fileName = fileName;
	}

	/**
	 * Sets the fileSize.
	 * @param fileSize The fileSize to set
	 */
	public void setFileSize(long fileSize)
	{
		this.fileSize = fileSize;
	}

	/**
	 * Sets the timeout.
	 * @param timeout The timeout to set
	 */
	public void setTimeout(int timeout)
	{
		this.timeout = timeout;
	}

}
