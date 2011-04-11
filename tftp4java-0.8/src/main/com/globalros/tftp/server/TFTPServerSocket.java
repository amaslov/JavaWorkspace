/**
 * (c) Melexis Telecom and/or Remote Operating Services B.V.
 * 
 * Distributable under LGPL license
 * See terms of license at gnu.org
 */
package com.globalros.tftp.server; 

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Hashtable;

import org.apache.log4j.Logger;

import com.globalros.tftp.common.FRQ;
import com.globalros.tftp.common.RRQ;
import com.globalros.tftp.common.TFTPPacket;
import com.globalros.tftp.common.VirtualFileSystem;
import com.globalros.tftp.common.WRQ;

/**
 *
 * @author  marco
 */
public class TFTPServerSocket implements Runnable {
//    private VirtualFileSystem vfs = null;
      private EventListener listener = null;    

    /** Creates a new instance of TFTPServerSocket */
    public TFTPServerSocket(int serverPort, int poolSize, VirtualFileSystem vfs, EventListener listener) {
  //    this.vfs = vfs;
         this.listener = listener;
		   this.serverPort = serverPort;
			this.poolSize = poolSize;
			/** Creates a new queue for receiving packets to wait for handling by a worker thread
			 *  Creates a new pool and fill with new workerthreads */
			newConnects = new Hashtable();
         if (tftpLog.isDebugEnabled())
            tftpLog.debug("Creating new pool of " + poolSize + " worker threads");
			workers = new TFTPPool(poolSize, newConnects, vfs, listener);
		}

    public void setPoolSize(int poolSize) {
      this.poolSize = poolSize;
		workers.resize(poolSize);
    }
    
    public int getPoolSize() {
      return poolSize;
    }
    
  /* This internal function helps to verify the fileContent on the request.
   * If the WRQ or RRQ packet does not contain valid fileContent, the request
   * is denied. It would be possible to delegate a check to rosmcp too.*/
//  private boolean fileContentOK () {
    /* filename should not be empty, else send ERROR and say goodbye */
//    if (fileContent == null || Array.getLength(fileContent) == 0) {
//      sendErrPacket (ERR_NOT_DEFINED, "Invalid ROS-MCP content, GO AWAY!" + fileName);
//      tftpLog.debug(destAddrStr + ":" + destPort + 
//        " Client sent RRQ/WRQ without valid content!");
//      return false;
//    }
//    return true;
//  }
  
  public void run() {
    int bufSize = 528;    
    InetAddress clientAddress;
    int clientPort;
		try {
		  serverSocket = new DatagramSocket(serverPort);
		  serverSocket.setSoTimeout(1000);
		} catch (SocketException e) {
         tftpLog.warn("Could not create socket on port: " + serverPort + ", shutting down!");
         // OK here we should inform the main thread that is is of no use to continue
         stop();
		  return;
		}
		
    tftpLog.info("TFTPServerSocket is started");
    
    for (;abort != true;) {
      byte[] buffer = new byte[bufSize];
      DatagramPacket packet = new DatagramPacket(buffer, bufSize);
      try {
        serverSocket.receive (packet);
      } catch (IOException ioe) {
        /** Did not receive any package, it is silent
					*  humm, I do not earn enough money! */
        continue;
      }
      clientAddress = packet.getAddress();
      clientPort = packet.getPort();
      tftpLog.debug("server received request for file from " + clientAddress.toString());

      // copy the data from udp packet      
      byte[] tftpP = new byte[packet.getLength()];
      System.arraycopy(packet.getData(), packet.getOffset(), tftpP, 0, packet.getLength());
      
      FRQ frq;
      int opcode = TFTPPacket.fetchOpCode(tftpP);
      try {
        switch (opcode) {
          case RRQ.OPCODE: frq = new RRQ(tftpP); break;
          case WRQ.OPCODE: frq = new WRQ(tftpP); break;
          default: continue;
        }
      } catch (InstantiationException e) {
            tftpLog.info("InstantiationException: " + e.getMessage());
				continue;
      }
      
      /* This internal function helps to verify the filename on the request.
       * If the WRQ or RRQ packet contains not a valid filename, the request
       * is ignored. It would be possible to delegate a check to rosmcp too.*/
      if (frq.getFileName() == null || frq.getFileName().length() == 0) continue;
      workers.performWork(frq, clientAddress, clientPort); 
			
    }
    tftpLog.info("TFTPServerSocket is stopped");
  }

  public void stop() {
      workers.resize(0);
      abort = true;
		if (serverSocket != null) serverSocket.close();
  }
    
    /* poolSize stores the number of workers with sockets to handle of incoming
     * requests on master TFTP socket, for now this size is only affected when
     * set before the socket is connected */
    private int poolSize;
    
    private TFTPPool workers;
    private Hashtable newConnects;

    DatagramSocket serverSocket;
    int serverPort;
    boolean abort;
    
  static Logger tftpLog = Logger.getLogger(TFTPServerSocket.class);
}
