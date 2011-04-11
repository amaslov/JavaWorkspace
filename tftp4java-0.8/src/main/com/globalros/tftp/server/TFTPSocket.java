/**
 * (c) Melexis Telecom and/or Remote Operating Services B.V.
 * 
 * Distributable under LGPL license
 * See terms of license at gnu.org
 */
package com.globalros.tftp.server;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import org.apache.log4j.Logger;

import com.globalros.tftp.common.ACK;
import com.globalros.tftp.common.DATA;
import com.globalros.tftp.common.ERROR;
import com.globalros.tftp.common.OACK;
import com.globalros.tftp.common.RRQ;
import com.globalros.tftp.common.TFTPPacket;
import com.globalros.tftp.common.WRQ;

/**
 * @author  marco
 */

/*
 * This class handles off one TFTP write or read request
 * in future time this could be improved to handle more than one
 * request on same socket.
 * Thus the maximum of 65534 ports could be overcome
 * if capacity on NIC and processor would allow. */
public class TFTPSocket
{
   // timeout in msecs
   private int timeout;
   
   byte[] buffer;

   InetAddress destAddr;
   int destPort;

   static int handlePort;
   DatagramSocket udpSocket;

   static Logger tftpLog = Logger.getLogger(TFTPSocket.class);
   public static final int BLOCK_SIZE = 512;

   /** Creates a new instance of TFTPSocket */
   public TFTPSocket(int timeout) throws SocketException
   {
      udpSocket = getFreeSocket();
      this.setSocketTimeOut(timeout);

      buffer = new byte[BLOCK_SIZE + 16];
   }

   /**
    * This method is used to set the tftp socket timeout in seconds
    * @param secs Number of seconds a read should block. Use zero to make read blocking
    */
   public void setSocketTimeOut(int secs) throws SocketException
   {
      this.timeout = secs * 1000;
      udpSocket.setSoTimeout(secs * 1000);
   }

   public void setSockTimeoutMSec(int msecs) throws SocketException
   {
      this.timeout = msecs;
      udpSocket.setSoTimeout(msecs);
   }
   
   public int getSocketTimeOut()
   {
      return timeout / 1000;
   }


   /** static method to provide port number to */
   private static DatagramSocket getFreeSocket() throws SocketException
   {
      int loopPort = handlePort - 1;
      while (loopPort != handlePort)
      {
         if ((handlePort < 29001) || (++handlePort > 65000))
         {
            handlePort = 29001;
         }
         try
         {
            DatagramSocket freeSocket = new DatagramSocket(handlePort);
            return freeSocket;
         } catch (SocketException e)
         {
            /* continue to find free port */
            continue;
         }
      }
      /* should already be returned with free Socket! */
      tftpLog.warn("Could not find a free port!");
      throw new SocketException();
   }

   public TFTPPacket read() throws IOException
   {
      DatagramPacket udpPacket = new DatagramPacket(buffer, BLOCK_SIZE + 16);
      try
      {
         udpSocket.receive(udpPacket);
         
      } catch (InterruptedIOException e)
      {
         // timeout occured, no packet received!
         return null;
      }

      byte[] udpData = udpPacket.getData();
      int udpLength = udpPacket.getLength();
      tftpLog.debug("udpPacket.length in receive: " + udpLength);

      // copy tftpdata
      byte[] tftpPB = new byte[udpPacket.getLength()];
      System.arraycopy(udpData, udpPacket.getOffset(), tftpPB, 0, udpLength);

      TFTPPacket tftpP = null;
      try
      {
         int opcode = TFTPPacket.fetchOpCode(tftpPB);
         switch (opcode)
         {
            case ACK.OPCODE :
               tftpP = new ACK(tftpPB);
               break;
            case DATA.OPCODE :
               tftpP = new DATA(tftpPB, udpLength);
               break;
            case RRQ.OPCODE :
               tftpP = new RRQ(tftpPB);
               break;
            case WRQ.OPCODE :
               tftpP = new WRQ(tftpPB);
               break;
            case OACK.OPCODE :
               tftpP = new OACK(tftpPB);
               break;               
            case ERROR.OPCODE :
               tftpP = new ERROR(tftpPB);
               break;            
            default:
               tftpLog.error("Unknown opcode: "+opcode);
               break;   
               
         }
      } catch (InstantiationException e)
      {
         throw new IOException("Could not discover tftp packet in recieved data!"+e.getMessage());
      }                  
      tftpP.setPort(udpPacket.getPort());
      tftpP.setAddress(udpPacket.getAddress());
      
      return tftpP;
   }

   public void write(TFTPPacket tftpP) throws IOException
   {           
      byte[] data = tftpP.getBytes();
      InetAddress address = tftpP.getAddress();
      
      int port = tftpP.getPort();      
      
      if(udpSocket.isConnected())
      {
         address = destAddr;
         port = destPort; 
      }
           
      DatagramPacket udpPacket =
         new DatagramPacket(data, data.length, address, port);        
      udpSocket.send(udpPacket);
   }

   public void connect(InetAddress addr, int port)
   {
      /* if (udpSocket.isConnected()) */
      udpSocket.disconnect();
      udpSocket.connect(addr, port);
      destAddr = addr;
      destPort = port;
   }

   public void clear() 
   {
      try
      {
         udpSocket.setSoTimeout(10);
      } catch (SocketException e1)
      {
         tftpLog.warn(e1);
         e1.printStackTrace();
      }
      byte[] data = new byte[516];
      DatagramPacket udpPacket = new DatagramPacket(data, data.length);
      while (true)
      {
         try
         {
            udpSocket.receive(udpPacket);
         }
         catch (SocketTimeoutException ste)
         {
            //tftpLog.debug("[clear]: "+ste.getMessage());
            try
            {
               udpSocket.setSoTimeout(timeout);
            } catch (SocketException e1)
            {
               tftpLog.warn(e1);
               e1.printStackTrace();
            }
            return;
         }
         catch (Exception e)
         {
            tftpLog.debug("[clear] :" +e.getMessage());
            try
            {
               udpSocket.setSoTimeout(timeout);
            } catch (SocketException e1)
            {
               tftpLog.warn(e1);
               e1.printStackTrace();
            }
            return;
         }
      }
      
//      int tempPort = udpSocket.getPort();
//      InetAddress tempAddress = udpSocket.getInetAddress();
//      boolean connectedAlready = udpSocket.isConnected();
//      
//      try {  
//         udpSocket.disconnect();
//         udpSocket.close();         
//         udpSocket = new DatagramSocket(tempPort);
//         this.setSocketTimeOut(timeout);
//                         
//         if(connectedAlready) 
//         //if(udpSocket.isBound())
//         {
//            // then drop the connection and reconnect...
//            tftpLog.debug("drop the connection and reconnect...");
//            
//            this.connect(tempAddress, tempPort);                                            
//         }         
//      } catch(SocketException se)
//      {
//         tftpLog.debug("Socket is freaking useless...:"+se.getMessage()); 
//      }

   }
   
   public void disconnect()
   {
      udpSocket.disconnect();
   }   

   public int getPort()
   {
      return destPort;
   }
   
   public InetAddress getAddress()
   {
      return destAddr;
   }
   
   /*
       public static String getIP(InetAddress in)
       {
         String ret;
         byte[] buf = in.getAddress();
         StringBuffer rv = new StringBuffer();
         for (int i = 0; i < buf.length; i++) {
     rv.append (buf[i] + ".");
         }
         ret = rv.toString();
         return ret.substring (0, ret.length() - 1);
       }
   */
}
