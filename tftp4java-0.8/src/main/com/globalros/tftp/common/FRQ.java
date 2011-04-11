/**
 * (c) Melexis Telecom and/or Remote Operating Services B.V.
 * 
 * Distributable under LGPL license
 * See terms of license at gnu.org
 */
package com.globalros.tftp.common;

import org.apache.log4j.Logger;




/**
 * This class is an abstract class that implements the shared functionality
 * for tftp write and read requests. It only supports the tftp option extensions
 * for timeout and tsize. These are only implemented for server applications
 * and not for client applications
 */
public abstract class FRQ extends TFTPPacket
{
   private static Logger log = Logger.getLogger("FRQ.class");
   
	/**
	 * private field contains the tftp options when send in request
	 * otherwise null
	 */
	TFTPOptions tftpOptions = null;
    
	/**
	 * This is private helper for the constructor to read out the filename
	 * @param b byte array containing tftp request packet
	 * @return the index of the location in array after the zero terminated filename
	 * @throws InstantiationException indicates that tftp packet is incomplete
	 */
	private int readFileName(byte[] b) throws InstantiationException
	{
		//check if we do not go out of bounds when we look for filename
		if (IX_FILENAME >= b.length)
		{
			throw new InstantiationException(
				"TFTP request passed to constructor is not a complete "
					+ getClass().getName()
					+ " packet! It does not contain the filename!");
		}
		StringBuffer sb = new StringBuffer();
		// read the filename and the returned integer is index to mode
		int IX_MODE = readString(IX_FILENAME, b, sb);
		// At this point we broke out of the loop since we found zero byte
		// and so IX_MODE points to the location where the mode string is
		// expected and store the filename
		fileName = sb.toString();
		return IX_MODE;
	}

	/**
	 * This is a private helper for the constructor to read the tftp mode that
	 * @param IX_MODE index to the mode in the tftp packet
	 * @param b byte array containing the tftp packet
	 * @return the index of the location in array after the zero terminated mode
	 * @throws InstantiationException indicates that tftp packet is incomplete
	 */
	private int readMode(int IX_MODE, byte[] b) throws InstantiationException
	{
		// Check if de not go out of bound when we look for mode string
		if (IX_MODE >= b.length)
		{
			throw new InstantiationException(
				"TFTP packet passed to constructor is not a complete "
					+ getClass().getName()
					+ " packet! Mode could not be found");
		}
		// From here try to read the mode string from the packet
		StringBuffer sb = new StringBuffer();
		int IX_OPTION = readString(IX_MODE, b, sb);
		String modeString = sb.toString();
		// it should be one of ASCII, OCTET or MAIL
		for (int i = ASCII_MODE; i <= MAIL_MODE; i++)
		{
			if (modeString.compareToIgnoreCase(getModeString(i)) == 0)
			{
				mode = i;
				return IX_OPTION;
			}
		}
		throw new InstantiationException(
			"Unsupported mode found in the file request for mode: " + modeString);
	}

	/**
	 * This is a private helper for the constructor to read the tftp mode that
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
      log.debug("name of option: " + option);

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
      log.debug("value of option: " + value);
      
		// check if we did not have an option collected yet
		if (tftpOptions == null)
			tftpOptions = new TFTPOptions(3);

		tftpOptions.put(option, value);
		return IX_OPTION;
	}

   /**
    * 
    * FRQ default constructor
    */
   public FRQ() 
   {
      super();
   }
   
	/**
	 * This constructor is used to construct a WRQ or RRQ object
	 * from a byte array received from socket that contains the
	 * tftp packet.
	 */
	public FRQ(byte[] tftpP) throws InstantiationException
	{
		super(tftpP);

		// wrap all possible exception in InstantiationException
		try
		{
			int IX_MODE = readFileName(tftpP);
			int IX_OPTION = readMode(IX_MODE, tftpP);
			// check if we have extended options
			if (IX_OPTION >= tftpP.length)
				return;

			// keep reading the options until end of packet
			while (IX_OPTION < tftpP.length)
			{
				IX_OPTION = readOption(IX_OPTION, tftpP);
			}
		}
		catch (Throwable t)
		{
			if (t instanceof InstantiationException)
				throw (InstantiationException) t;
			throw new InstantiationException("CODING ERROR? " + t.getMessage());
		}
	}

	/**
	 * This method returns true if this request contained options as specified in RFC2349
	 * @return True when request contained options otherwise false
	 */
	public boolean hasOptions()
	{
		return (tftpOptions != null);
	}

	/**
	 * This method is used to return a byte array representing the RRQ or WRQ.
	 * The actual OPCODE is set correctly in the derived class.
	 * In the server implementation this method will never be called since, RRQ or
	 * WRQ will only be received and constructed from byte arrays.
	 * Never need this to be converted back to a byte array, except for pure
	 * client applications.
	 * 
	 * FIXME: implement the options extension in tftp for client usage
	 * 
	 * @return Byte array representing the WRQ or RRQ packet
	 */
	public byte[] getBytes()
	{
		String modeString = "";
		if (mode >= ASCII_MODE && mode <= MAIL_MODE)
		{
			modeString = modeStrings[mode];
		}

		int tftpLen = 2 + fileName.length() + 1 + modeString.length() + 1;
      if (hasOptions()) tftpLen += tftpOptions.getOptionsSize();
		byte[] tftpP = new byte[tftpLen];

		// Insert two byte opcode
		tftpP[IX_OPCODE] = (byte) ((this.getOpCode() >> 8) & 0xff);
		tftpP[IX_OPCODE + 1] = (byte) (this.getOpCode() & 0xff);

		// Insert filename
		System.arraycopy(
			fileName.getBytes(),
			0,
			tftpP,
			IX_FILENAME,
			fileName.length());

		// Insert modeString
		int IX_MODE = IX_FILENAME + fileName.length();
		tftpP[IX_MODE++] = 0; // terminating zero for fileName
		System.arraycopy(
			modeString.getBytes(),
			0,
			tftpP,
			IX_MODE,
			modeString.length());
		IX_MODE += modeString.length();
		tftpP[IX_MODE] = 0; // terminating zero for modeString
      
      int IX_OPTION = IX_MODE + 1;
      int optionLength = 0;
      String optionValue = null;
      if (hasOptions() && tftpOptions.hasOption(TFTPOptions.TIMEOUT))
      {
         optionLength = TFTPOptions.TIMEOUT.length();
         System.arraycopy(
            TFTPOptions.TIMEOUT.getBytes(),
            0,
            tftpP,
            IX_OPTION,
            optionLength);
         IX_OPTION += optionLength;                 
         tftpP[IX_OPTION++] = 0;
         
         optionValue = "" + tftpOptions.getTimeout();
         optionLength = optionValue.length();
         System.arraycopy(
            optionValue.getBytes(),
            0,
            tftpP,
            IX_OPTION,
            optionLength);
         IX_OPTION += optionLength;
         tftpP[IX_OPTION++] = 0;                 
      }
      
      if (hasOptions() && tftpOptions.hasOption(TFTPOptions.TSIZE))
      {
         optionLength = TFTPOptions.TSIZE.length();
         System.arraycopy(
            TFTPOptions.TSIZE.getBytes(),
            0,
            tftpP,
            IX_OPTION,
            optionLength);
         IX_OPTION += optionLength;                 
         tftpP[IX_OPTION++] = 0;
         
         optionValue = "" + tftpOptions.getTransferSize();
         optionLength = optionValue.length();
         System.arraycopy(
            optionValue.getBytes(),
            0,
            tftpP,
            IX_OPTION,
            optionLength);
         IX_OPTION += optionLength;
         tftpP[IX_OPTION++] = 0;                 
      }
		return tftpP;
	}

	//------------------- accessors and modifiers ---------------------
	private String fileName = "";
   
   public void setFileName(String fileName)
   {
      this.fileName = fileName;
   }
   
	public String getFileName()
	{
		return fileName;
	}

	private int mode = UNKNOWN_MODE;

   public void setMode(int mode)
   {
      this.mode = mode;      
   }
   
	public int getMode()
	{
		return mode;
	}

	public int getTimeout()
	{
		if (hasOptions())
			return tftpOptions.getTimeout();
		return -1;
	}
   
   public void setTimeout(int timeout)
   {
      if (tftpOptions == null) tftpOptions = new TFTPOptions();
      tftpOptions.setTimeout(timeout);
   }

	// default maximum file transfer size
	private int tsize = 2048;
	public int getTransferSize()
	{
		if (hasOptions())
			return tftpOptions.getTransferSize();
		return -1;
	}
  
   public void setTransferSize(int transferSize)
   {
      if (tftpOptions == null) tftpOptions = new TFTPOptions();
      tftpOptions.setTransferSize(transferSize);
   }

	//--------------- indices of the tftp elements in packets --------------------
	final static int IX_FILENAME = 2;

	//--------------------- Transfer modes and strings ----------------
	final static int UNKNOWN_MODE = 0;
	final static int ASCII_MODE = 1;
	public final static int OCTET_MODE = 2;
	final static int MAIL_MODE = 3;

	final static String[] modeStrings =
		{ "unknown", "netascii", "octet", "mail" };

	static String getModeString(int mode)
	{
		if (mode > UNKNOWN_MODE && mode <= MAIL_MODE)
			return modeStrings[mode];
		else
			return modeStrings[UNKNOWN_MODE];
	}
}
