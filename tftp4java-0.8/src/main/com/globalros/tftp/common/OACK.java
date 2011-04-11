/**
 * (c) Melexis Telecom and/or Remote Operating Services B.V.
 * 
 * Distributable under LGPL license
 * See terms of license at gnu.org
 */
package com.globalros.tftp.common;



/**
 * @author marco
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class OACK extends ACK
{

   /**
    * This field is used to store the tftp options that go  in the acknowledgement
    */
   private TFTPOptions options = null;
   
	/**
	 * Constructor for OACK.
	 */
	public OACK(int blockNr, TFTPOptions options)
	{
      super(blockNr);
      this.options = options;
	}

   public boolean hasTimeout()
   {
      if (options != null) return options.hasOption(TFTPOptions.TIMEOUT);
      return false; 
   }
   
   public int getTimeout()
   {
      if (options != null) return options.getTimeout();
      return -1;
   }
   
   
   public boolean hasTransferSize()
   {
      if (options != null) return options.hasOption(TFTPOptions.TSIZE);
      return false;
   }
   
   public int getTransferSize()
   {
      if (options != null) return options.getTransferSize();
      return -1;
   }

   
   /**
    * This is a private helper for the constructor to read a tftp option that
    * @param IX_MODE index to the mode in the tftp packet
    * @param b byte array containing the tftp packet
    * @return the index of the location in array after the zero terminated option
    * @throws InstantiationException indicates that tftp packet is incomplete
    */
   private int readOption(int IX_OPTION, byte[] b)
      throws InstantiationException
   {
      // we cannot read option since we are at end of packet
      if (IX_OPTION >= b.length)
         return IX_OPTION;

      StringBuffer sb = new StringBuffer();
      int IX_VALUE = readString(IX_OPTION, b, sb);
      String option = sb.toString();
      sb = null;

      // check if we will not go out of bounds
      if (IX_VALUE >= b.length)
         throw new InstantiationException(
            "TFTP packet passed to constructor is not a complete "
               + getClass().getName()
               + " packet! Missing value for option: "
               + option);

      sb = new StringBuffer();
      IX_OPTION = readString(IX_VALUE, b, sb);
      String value = sb.toString();
      sb = null;

      // check if we did not have an option collected yet
      if (options == null)
         options = new TFTPOptions(3);

      options.put(option, value);
      return IX_OPTION;
   }

	/**
	 * Constructor for OACK.
	 * @param tftpP
	 * @throws InstantiationException
	 */
	public OACK(byte[] tftpP) throws InstantiationException
	{
    super(tftpP);
    int IX_OPTION = IX_OPCODE + 2;
    
    while (IX_OPTION < tftpP.length -2)
    {
       IX_OPTION = readOption(IX_OPTION, tftpP);
    }
   // over scope the super IX_BLOCKNR
//    int IX_BLOCKNR = IX_OPTION;
//    if (IX_BLOCKNR >= tftpP.length -2)
//      throw new InstantiationException("Could not find blocknr in OACK");
//    blockNr = makeword(tftpP[IX_BLOCKNR], tftpP[IX_BLOCKNR+1]);
      blockNr = 0;
	}

	/**
    * Helper function to calculate length of OACK in bytes
    */
   private int getLength()
   {
      int length = 4;
      if (options == null) return length;
      int timeout = options.getTimeout();
      if (timeout > 0) length += options.TIMEOUT.length() + ((String) options.get(options.TIMEOUT)).length() +2;
      int tsize = options.getTransferSize();
      if (tsize > 0) length += options.TSIZE.length() + ((String) options.get(options.TSIZE)).length() +2;
      return length;
   }
   
   /**
	 * @see com.globalros.tftp.common.TFTPPacket#getBytes()
	 */
	public byte[] getBytes()
	{
    byte [] tftpP = new byte[getLength()];
    // Insert two byte opcode
    tftpP[IX_OPCODE] = (byte)((OPCODE >> 8) & 0xff);
    tftpP[IX_OPCODE+1] = (byte)(OPCODE & 0xff);
   
   int IX_OPTION = IX_OPCODE +2;
    int timeout = options.getTimeout();
    if (timeout > 0) 
    {
      int length = options.TIMEOUT.length();
      System.arraycopy(options.TIMEOUT.getBytes(), 0, tftpP, IX_OPTION, length);
      IX_OPTION += length;
      tftpP[IX_OPTION++] = 0;
      
      String timeoutValue = (String) options.get(options.TIMEOUT);
      length = timeoutValue.length();
      System.arraycopy(timeoutValue.getBytes(), 0, tftpP, IX_OPTION, length);
      IX_OPTION += length;
      tftpP[IX_OPTION++] = 0;
    }
      
    int tsize = options.getTransferSize();
    if (tsize > 0) 
    {
      int length = options.TSIZE.length();
      System.arraycopy(options.TSIZE.getBytes(), 0, tftpP, IX_OPTION, length);
      IX_OPTION += length;
      tftpP[IX_OPTION++] = 0;
      
      String tsizeValue = (String) options.get(options.TSIZE);
      length = tsizeValue.length();
      System.arraycopy(tsizeValue.getBytes(), 0, tftpP, IX_OPTION, length);
      IX_OPTION += length;
      tftpP[IX_OPTION++] = 0;
    }
    int IX_BLOCKNR = IX_OPTION;
   
    // Insert two byte blocknumber
    tftpP[IX_BLOCKNR] = (byte)((blockNr >> 8) & 0xff);
    tftpP[IX_BLOCKNR+1] = (byte)(blockNr & 0xff);
    return tftpP;
	}

  static public final int OPCODE  = 6;
   /**
    * @see com.globalros.tftp.common.TFTPPacket#getOpCode()
    */
  public int getOpCode() { return OPCODE; }
}
