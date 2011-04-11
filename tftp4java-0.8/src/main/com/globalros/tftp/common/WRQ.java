/**
 * (c) Melexis Telecom and/or Remote Operating Services B.V.
 * 
 * Distributable under LGPL license
 * See terms of license at gnu.org
 */
package com.globalros.tftp.common;

public class WRQ extends FRQ {
   
  /**
   * 
   * WRQ default constructor
   * @param tftpP
   * @throws InstantiationException
   */ 
  public WRQ() throws InstantiationException{
     super();
  }
  
  public WRQ(byte [] tftpP) throws InstantiationException {
    super(tftpP);
  }

  static public final int OPCODE  = 2;
  public int getOpCode() { return OPCODE; }
}

