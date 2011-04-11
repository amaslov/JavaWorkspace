/**
 * (c) Melexis Telecom and/or Remote Operating Services B.V.
 * 
 * Distributable under LGPL license
 * See terms of license at gnu.org
 */

/**
 * TFTPServer.java
 * @version 0.1 - March 2002
 * @author Marco Dubbeld
 *
 * This is the tftp server in remote operating services. An instance
 * of this class should be constructed. Then configure the server, and then
 * start/stop.
 */
package com.globalros.tftp.server;

import org.apache.log4j.Logger;

import com.globalros.tftp.common.VirtualFileSystem;

public class TFTPServer
{
	static int DEFAULT_PORT = 69;
	// fields for the attributes
	private int poolSize = 5;
	private int port = DEFAULT_PORT;
	private VirtualFileSystem vfs;

	private Thread server;
	private TFTPServerSocket ss;

	private EventListener listener = null;
	/**
	 * This constructor needs a VirtualFileSystem to get in or 
	 * output streams for a file.
	 * @param vfs An implementing virtual file system
	 */
	public TFTPServer(VirtualFileSystem vfs, EventListener listener)
	{
		this.vfs = vfs;
		this.listener = listener;
	}

	/**
	 * Returns the port.
	 * @return int
	 */
	public int getPort()
	{
		return port;
	}

	/**
	 * Sets the port.
	 * @param port The port to set
	 */
	public void setPort(int port)
	{
		this.port = port;
	}

	/**
	 * Sets the size of the workers pool
	 * @param size The size to set
	 */
	public void setPoolSize(int poolSize)
	{
		this.poolSize = poolSize;
		if (ss == null)
			return;
		ss.setPoolSize(poolSize);
	}

	/**
	 * Returns the size of the workers pool
	 * @return int
	 */
	public int getPoolSize()
	{
		return poolSize;
	}

	/**
	 * This method starts an actual server socket and listens
	 */
	public void start()
	{
		if (port == 0)
			port = DEFAULT_PORT;
		if (tftpLog.isDebugEnabled())
			tftpLog.debug("Starting new TFTP server socket on port: " + port);
         
		ss = new TFTPServerSocket(port, poolSize, vfs, listener);
		server = new Thread(ss);
		server.start();
	}

	public void stop()
	{
		if (ss == null)
		{
			tftpLog.warn(
				"ServerSocket is already null so, is tftpServer closed???");
			return;
		}

      if (tftpLog.isDebugEnabled())
         tftpLog.debug("Shutting down TFTP server socket.");
		ss.stop();

		if (server == null)
		{
			tftpLog.warn("FIXME: ServerSocket was not null but tftpServer is!");
			return;
		}

		try
		{
			server.join(6000);
		}
		catch (InterruptedException e)
		{
			tftpLog.warn("Could not close all TFTPServer thread!");
		}
		server = null;
		ss = null;
	}

	static Logger tftpLog = Logger.getLogger(TFTPServer.class);
}
