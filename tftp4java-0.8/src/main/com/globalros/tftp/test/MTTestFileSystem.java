/**
 * Title:        MTTestFileSystem.java<p>
 * Description:  <p>
 * Copyright:    (c) Roses B.V. 2003<p>
 * Company:      Roses B.V.<p>
 *
 * @author      Kinshuk
 * Created:      23-jul-2003
 */
package com.globalros.tftp.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import org.apache.log4j.Logger;

import com.globalros.tftp.common.VirtualFile;
import com.globalros.tftp.common.VirtualFileSystem;

/**
 * class:   MTTestFileSystem
 * package: com.globalros.tftp.test
 * project: tftp4java
 * 
 * This class is a modified version off the FileSystem class that Multi-Threaded
 * test units for TFTP uses.
 * 
 * The reason for this file is so that the MT unit tests are able to work against
 * byte array output streams so that for test purposes, files so not have to be 
 * transferred from/to physical locations.
 * 
 * Instead, under the hood we are use byte arrays and can check whether 
 * the byte arrays are the same before/after transfer. 
 *  
 */
public class MTTestFileSystem implements VirtualFileSystem
{
   /**
    * logger
    */
   private static Logger log = Logger.getLogger(MTTestFileSystem.class.getName());

   /**
    * Constructor for FileSystem.
    */
   public MTTestFileSystem()
   {      
   }
   
   private static ByteArrayOutputStream outputStream;
    
   /**
    * 
    * @see com.globalros.tftp.common.VirtualFileSystem#getInputStream(VirtualFile)
    * 
    */
   public InputStream getInputStream(VirtualFile file) throws FileNotFoundException
   {            
      Map myMap = MTDownloadTest.getMap();                
      return new ByteArrayInputStream (((String)myMap.get(new Integer(file.getFileName()))).getBytes());
   }

   /**
    * @see com.globalros.tftp.common.VirtualFileSystem#getOutputStream(VirtualFile)
    */
   public OutputStream getOutputStream(VirtualFile file) throws FileNotFoundException
   {      
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      
      this.setOutputStream(outputStream);
      return outputStream;
   }     
   
   /**
    * These are internal methods to set the output so it can be retrieved from the unit test and 
    * compared against!! 
    */
   public static ByteArrayOutputStream getOutputStream() 
   {
      return outputStream;
   }
   
   public void setOutputStream(ByteArrayOutputStream outputStream)
   {
      this.outputStream = outputStream;
   }
}
