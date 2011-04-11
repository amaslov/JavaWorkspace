/**
 * Title:        MTTestServer.java<p>
 * Description:  <p>
 * Copyright:    (c) Roses B.V. 2003<p>
 * Company:      Roses B.V.<p>
 *
 * @author      Kinshuk
 * Created:      23-jul-2003
 */
package com.globalros.tftp.test;

import java.net.InetAddress;

import org.apache.log4j.Logger;

import com.globalros.tftp.common.VirtualFileSystem;
import com.globalros.tftp.server.EventListener;
import com.globalros.tftp.server.TFTPServer;

/**
 * class:   MTTestServer
 * package: com.globalros.tftp.test
 * project: tftp4java
 */
public class TFTPTestServer implements EventListener
{
   /**
    * logger
    */
   private Logger log = Logger.getLogger(TFTPTestServer.class.getName());


   private TFTPServer tftpServer;

   /**
    * Constructor for Server.
    * 
    * @param singleThreaded the file system created depends on whether the test is a Multi-Threaded / Single-Threaded 
    * test
    */
   public TFTPTestServer(boolean singleThreaded)
   {
      VirtualFileSystem vfs = null;
      // For in-memory testing purposes, the File System has been written to provide a byte array layer.
      if(!singleThreaded)  
          vfs = new MTTestFileSystem();
      else    
          vfs = new STTestFileSystem();
         
      tftpServer = new TFTPServer(vfs, this);
      tftpServer.setPoolSize(2);
      tftpServer.setPort(1069);
   }

   public void connect() throws Exception
   {
      if (tftpServer == null) return;
      tftpServer.start();
   }

   public void disconnect()
   {
      if (tftpServer == null) return;
      tftpServer.stop();
   }
   
   public void onAfterDownload(InetAddress a, int p, String fileName, boolean ok)
   {
      if (ok) log.debug("Send " + fileName + " sucessfully to client: " + a.getHostAddress() + " port: " +p);
      else log.debug("Send " + fileName + " file not sucessfully to client: " + a.getHostAddress() + " port: " +p);     
   }
   
   public void onAfterUpload(InetAddress a, int p, String fileName, boolean ok)
   {
      if (ok) log.debug("received " + fileName + " sucessfully from client: " + a.getHostAddress() + " port: " +p);
      else log.debug("received " + fileName + " file not sucessfully from client: " + a.getHostAddress() + " port: " +p);     
   }
}
