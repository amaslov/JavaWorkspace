/**
 * (c) Melexis Telecom and/or Remote Operating Services B.V.
 * 
 * Distributable under LGPL license
 * See terms of license at gnu.org
 */
package com.globalros.tftp.common;

public class DATA extends ACK {
  /**
   * required since TFTPClient must create empty TFTPClient object 
   */ 
  public DATA() throws InstantiationException
  {
     super();
  } 
  
  public DATA (int blockNr, byte[] data) throws InstantiationException {
     this(blockNr, data, 0, data.length);
  }
  
  public DATA (int blockNr, byte [] data, int offset, int len) throws InstantiationException {
    super(blockNr);
    try {
      this.data = new byte [len];
      System.arraycopy(data, offset, this.data, 0, len);
    } catch (Exception e) {
      throw new InstantiationException("Data passed to constructor of DATA is invalid! " + e.toString());
    }
  }
	
  // construct tftp packet from udp data
  public DATA (byte[] tftpP, int tftpPLength) throws InstantiationException {
    super(tftpP);
    
    try {
      if (tftpPLength > tftpP.length) tftpPLength = tftpP.length;
      int len = tftpPLength - IX_DATA;
      data = new byte[len];
      System.arraycopy(tftpP, 4, data, 0, len);
    } catch (Exception e) {
      throw new InstantiationException("CODING ERROR? " + e.toString());
    }
  }

  public byte [] getBytes() {
    int tftpLen = MIN_PACKET_SIZE;
    if (data != null) tftpLen += data.length;
    byte [] tftpP = new byte[tftpLen];
    
    // Insert two byte opcode
    tftpP[IX_OPCODE] = (byte)((OPCODE >> 8) & 0xff);
    tftpP[IX_OPCODE+1] = (byte)(OPCODE & 0xff);
    // Insert two byte blocknumber
    tftpP[IX_BLOCKNR] = (byte)((blockNr >> 8) & 0xff);
    tftpP[IX_BLOCKNR+1] = (byte)(blockNr & 0xff);
    // Insert data 
    System.arraycopy(data, 0, tftpP, IX_DATA, data.length);
    return tftpP;
  }
  
  //--------- accessors and modifiers ----------------
  byte [] data = {};
  public byte[] getData() { return data; }
  
//--------------- indices of the tftp elements in packets --------------------
  final static int IX_DATA  = 4;

  static public final int OPCODE  = 3;
  public int getOpCode() { return OPCODE; }
}
