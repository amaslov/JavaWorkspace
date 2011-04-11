/**
 * (c) Melexis Telecom and/or Remote Operating Services B.V.
 * 
 * Distributable under LGPL license
 * See terms of license at gnu.org
 */
package com.globalros.tftp.common;

import java.net.InetAddress;

abstract public class TFTPPacket
{
   private InetAddress destAddr;
   private int destPort;

   // build new TFTPPacket 
   public TFTPPacket()
   {
      // OPCODE is already set in subclass
   }

   /**
    * This method helps to read byte for byte through a byte array
    * until zero byte found starting from index and puts the characters
    * in a stringbuffer
    */
   public int readString(int index, byte[] b, StringBuffer sb)
   {
      // We start reading from index in byte array and break until end of byte array
      // or zero byte is found. Characters are appended to stringbuffer
      // In the same time we read the string and check that we do not go out 
      // of bounds on a broken packet
      int i;
      for (i = index; i < b.length; i++)
      {
         // zero terminates the string
         if (b[i] == 0)
            break;
         // until we find zero append the characters of the filename in buffer
         sb.append((char) b[i]);
      }
      return ++i;
   }

   // construct tftppacket from udp data	
   public TFTPPacket(byte[] tftpP) throws InstantiationException
   {
      // use getMinPacketSize and getOpcode to use possibly overriden values from subclasses!
      if ((tftpP == null) || (tftpP.length < getMinPacketSize()))
      {
         throw new InstantiationException(
            "Argument passed to constructor is not a valid "
               + getClass().getName()
               + " packet!");
      }

      if (makeword(tftpP[IX_OPCODE], tftpP[IX_OPCODE + 1]) != getOpCode())
      {
         throw new InstantiationException(
            "Argument passed to constructor is not a valid "
               + getClass().getName()
               + " packet!");
      }
   }

   abstract public byte[] getBytes();

   //--------------- indices of the tftp elements in packets --------------------
   final static int IX_OPCODE = 0;

   //-------------- static initialization before construcor chain ---------------
   //----------------- overwritten appropriate in subclasses --------------------
   static public final int MIN_PACKET_SIZE = 4;
   public int getMinPacketSize()
   {
      return MIN_PACKET_SIZE;
   }
   static public final int OPCODE = 0;
   abstract public int getOpCode(); /* { return OPCODE; } */

   static public int fetchOpCode(byte[] tftpP)
   {
      if (tftpP == null)
         return 0;
      if (tftpP.length < IX_OPCODE + 2)
         return 0;
      return makeword(tftpP[IX_OPCODE], tftpP[IX_OPCODE + 1]);
   }

   // helper function to create integer from two bytes
   static public int makeword(byte Hi, byte Lo)
   {
      return (((int) ((int) Hi << 8) & 0xff00) | ((int) Lo & 0xff));
   }

   static public byte getMsb(int i)
   {
      return (byte) ((i >> 8) & 0xff);
   }

   static public byte getLsb(int i)
   {
      return (byte) (i & 0xff);
   }

   public void setAddress(InetAddress addr)
   {
      destAddr = addr;
   }

   public void setPort(int port)
   {
      destPort = port;
   }

   public InetAddress getAddress()
   {
      return destAddr;
   }

   public int getPort()
   {
      return destPort;
   }

}
