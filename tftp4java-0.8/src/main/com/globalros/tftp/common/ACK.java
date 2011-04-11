/**
 * (c) Melexis Telecom and/or Remote Operating Services B.V.
 * 
 * Distributable under LGPL license
 * See terms of license at gnu.org
 */
package com.globalros.tftp.common;

public class ACK extends TFTPPacket {
  /**
   * ACK default constructor - required since TFTPClient must create empty TFTPClient object 
   */
  public ACK()
  {
     this(0);     
  }
   
  // construct new ACK packet on block #
  public ACK(int blockNr) {
    super();
    this.blockNr = blockNr;
  }
  
  // construct tftp packet from udp data
  public ACK(byte[] tftpP) throws InstantiationException {
    super(tftpP);
    blockNr = makeword(tftpP[IX_BLOCKNR], tftpP[IX_BLOCKNR+1]);
  }

  public byte [] getBytes() {
    byte [] tftpP = new byte[MIN_PACKET_SIZE];
    // Insert two byte opcode
    tftpP[IX_OPCODE] = (byte)((OPCODE >> 8) & 0xff);
    tftpP[IX_OPCODE+1] = (byte)(OPCODE & 0xff);
    // Insert two byte blocknumber
    tftpP[IX_BLOCKNR] = (byte)((blockNr >> 8) & 0xff);
    tftpP[IX_BLOCKNR+1] = (byte)(blockNr & 0xff);
    return tftpP;
  }
  
  //--------- accessors and modifiers ----------------
  protected int blockNr = 0;
  public int getBlockNr() { return blockNr; }
  
//--------------- indices of the tftp elements in packets --------------------
  final static int IX_BLOCKNR  = 2;

  static public final int OPCODE  = 4;
  public int getOpCode() { return OPCODE; }
}
