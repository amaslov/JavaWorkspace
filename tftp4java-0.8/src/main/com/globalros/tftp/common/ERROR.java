/**
 * (c) Melexis Telecom and/or Remote Operating Services B.V.
 * 
 * Distributable under LGPL license
 * See terms of license at gnu.org
 */
package com.globalros.tftp.common;

public class ERROR extends TFTPPacket {
  public ERROR(int code, String message) throws InstantiationException {
    super();
    errorCode = code;
    errorMessage = message;
  }
	
  // construct tftp packet from udp data
  public ERROR(byte [] tftpP) throws InstantiationException {
    super(tftpP);
    
    errorCode = makeword(tftpP[IX_ERRCODE], tftpP[IX_ERRCODE+1]);
    
    if (IX_ERRMSG >= tftpP.length) {
      throw new InstantiationException("Argument passed to constructor is not a complete " + getClass().getName() + " packet!");
    }
    
    try {
      StringBuffer sb = new StringBuffer();
      for (int i = IX_ERRMSG; i < tftpP.length; i++) {
        if (tftpP[i] == 0) break;
        sb.append ((char)tftpP[i]);
      }
      errorMessage = sb.toString();
    } catch (Exception e) {
      throw new InstantiationException("CODING ERROR? " + e.toString());
    }
  }
  
  public byte [] getBytes() {
    int tftpLen = MIN_PACKET_SIZE + errorMessage.length() + 1;
    byte [] tftpP = new byte[tftpLen];
    
    // Insert two byte opcode
    tftpP[IX_OPCODE] = (byte)((OPCODE >> 8) & 0xff);
    tftpP[IX_OPCODE+1] = (byte)(OPCODE & 0xff);
  
    // Insert two byte errorcode
    tftpP[IX_ERRCODE] = (byte)((errorCode >> 8) & 0xff);
    tftpP[IX_ERRCODE+1] = (byte)(errorCode & 0xff);

    // Insert error message
    System.arraycopy(errorMessage.getBytes(), 0, tftpP, IX_ERRMSG, errorMessage.length());
    tftpP[tftpLen-1] = 0; // zero terminate on error message
    return tftpP;
  }
  
  //--------- accessors and modifiers ----------------
  private int errorCode = ERR_NO_ERROR;
  public int getErrorCode() { return errorCode; }
  
  private String errorMessage = "";
  public String getErrorMessage() { return errorMessage; }
  
//--------------------  Error codes and messages -----------------------------
  public final static int ERR_NO_ERROR         = -1;
  public final static int ERR_NOT_DEFINED      = 0;
  public final static int ERR_FILE_NOT_FOUND   = 1;
  public final static int ERR_ACCESS_VIOLATION = 2;
  public final static int ERR_DISK_FULL        = 3;
  public final static int ERR_ILLEGAL_OP       = 4;
  public final static int ERR_UNKNOWN_TRANS_ID = 5;
  public final static int ERR_FILE_EXISTS      = 6;
  public final static int ERR_NO_SUCH_USER     = 7;

  final static String[] errStrings = {"Unknown error",
				"File not found",
				"Access violation",
				"Disk full or allocation exceeded",
				"Illegal TFTP operation",
				"Unknown transfer ID",
				"File already exists",
				"No such user"};

  static String getErrorMessage(int messageID) {
    if (messageID > ERR_NOT_DEFINED && messageID <= ERR_NO_SUCH_USER) return errStrings [messageID];
    else return errStrings[ERR_NOT_DEFINED];
  }

//--------------- indices of the tftp elements in packets --------------------
  final static int IX_ERRCODE  = 2;
  final static int IX_ERRMSG   = 4;

  static public final int OPCODE  = 5;
  public int getOpCode() { return OPCODE; }
  static public final int MIN_PACKET_SIZE  = 5;
  public int getMinPacketSize() { return MIN_PACKET_SIZE; }
}
