/**
 * (c) Melexis Telecom and/or Remote Operating Services B.V.
 * 
 * Distributable under LGPL license
 * See terms of license at gnu.org
 */
package com.globalros.tftp.common;

public class RRQ extends FRQ {
  /**
   * 
   * RRQ default constructor. 
   * Added because off the client requirement to create RRQ objects. 
   *
   */
  public RRQ() throws InstantiationException {
     super();
  }
  
  public RRQ(byte [] tftpP) throws InstantiationException {
    super(tftpP);
  }

  static public final int OPCODE  = 1;
  public int getOpCode() { return OPCODE; }
}
