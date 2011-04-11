/**
 * (c) Melexis Telecom and/or Remote Operating Services B.V.
 * 
 * Distributable under LGPL license
 * See terms of license at gnu.org
 */
package com.globalros.tftp.common;

import java.util.Hashtable;
import java.util.Map;

/**
 * @author marco
 * @since ROS 0.6
 *
 * This class stores the tftp options that are set in the write or read request
 */
public class TFTPOptions extends Hashtable
{
   /** This string is place holder for option name tsize **/
   public final static String TSIZE = "tsize";

   /** This string is place holder for option name timeout **/
   public final static String TIMEOUT = "timeout";

   /** This string is place holder for option name blksize **/
   public final static String BLKSIZE = "blksize";
   
	/**
	 * Constructor for TFTPOptions.
	 * @param initialCapacity
	 */
	public TFTPOptions(int initialCapacity)
	{
		super(initialCapacity);
	}

	/**
	 * Constructor for TFTPOptions.
	 */
	public TFTPOptions()
	{
		super();
	}

	/**
	 * Constructor for TFTPOptions.
	 * @param t
	 */
	public TFTPOptions(Map t)
	{
		super(t);
	}

   /**
    * Helper function that retrieves an integer option that is
    * stored as decimal ascii string.
    * @param option name of the integer option to retrieve
    * @return integer value of the option or -1
    */
   public int getIntegerOption(String option)
   {
      String strValue = (String) get(option);
      if (strValue == null) return -1;
      try
      {
         Integer value = Integer.decode(strValue);
         return value.intValue();
      }
      catch (NumberFormatException e)
      {
         return -1;
      }
   }

   /**
    * Helper function that stores an integer option.
    * @param option name of the integer option to retrieve
    * @param value value of the option
    */
   public void setIntegerOption(String option, int value)
   {
      put(option, new Integer(value).toString());
   }
   
   /**
    * Gets the timeout option as specified in RFC2349
    * @return timeout in seconds or -1 if not specified
    */
   public int getTimeout()
   {
      return getIntegerOption(TIMEOUT);
   }
   
   /**
    * Sets the timeout option as specified in RFC2349
    * @param timeout timeout in seconds
    */
   public void setTimeout(int to)
   {
      setIntegerOption(TIMEOUT, to);
   }
   
   /**
    * Gets the tsize option as specified in RFC2349
    * @return tsize in bytes or -1 if not specified
    */
   public int getTransferSize()
   {
      return getIntegerOption(TSIZE);
   }
   
   /**
    * Sets the tsize option as specified in RFC2349
    * @param tsize tsize in bytes
    */
   public void setTransferSize(int tsize)
   {
      setIntegerOption(TSIZE, tsize);
   }
   
   /**
    * Gets the blksize option as specified in RFC2348
    * @return the blksize option as specified in RFC2348
    */
   public int getBlockSize()
   {
      return getIntegerOption(BLKSIZE);
   }
   
   /**
    * Sets the blksize option as specified in RFC2348
    * @param blksize blksize in bytes
    */
   public void setBlockSize(int blksize)
   {
      setIntegerOption(BLKSIZE, blksize);
   }
   
   
   /**
    * Checks if an option with name ... is stored
    */
   public boolean hasOption(String name)
   {
      return (this.containsKey(name));
   }
   
   /**
    * Returns the size of the options in a TFTPPacket
    */
   public int getOptionsSize()
   {
      int size = 0;
      // | timeout | 0 | #secs | 0 |
      if (hasOption(TIMEOUT)) size += TIMEOUT.length() + 1 + ("" + getTimeout()).length() + 1;
      if (hasOption(TSIZE)) size += TSIZE.length() + 1 + ("" + getTransferSize()).length() + 1;
      return size;
   }
   
   /**
    * Put ensures that every key is the name of an option (so lowercase)
    * @param option 
    */
   public Object put(Object option, Object value)
   {
      if (!(option instanceof String)) return null;
      if (!(value instanceof String)) return null;
      ((String)option).toLowerCase();
     return  super.put(option, value);
   }
   
}
