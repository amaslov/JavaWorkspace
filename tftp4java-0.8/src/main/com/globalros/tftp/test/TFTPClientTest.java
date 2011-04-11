/**
 * Title:        TFTPClientTest.java<p>
 * Description:  <p>
 * Copyright:    (c) Roses B.V. 2003<p>
 * Company:      Roses B.V.<p>
 *
 * @author      Kinshuk
 * Created:      3-jul-2003
 */
package com.globalros.tftp.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import com.globalros.tftp.client.TFTPClient;
import com.globalros.tftp.common.RRQ;
import com.globalros.tftp.common.WRQ;

import junit.framework.TestCase;

/**
 * class:   TFTPClientTest
 * package: com.globalros.tftp.test
 * project: tftp4java
 * 
 * This class is responsible for testing the download and upload functionality of
 * the TFTPClient.
 * 
 * The TFTPClient replicates device behaviour, initiating download/upload activity.
 * 
 * The tests in this class provide a single-threading paradigm and test download/upload for 
 * non-OACK and OACK.
 * 
 * The client test gives the use the options whether to download/upload using the local file 
 * system or  to perform the tests in memory (recommended). 
 * 
 */
public class TFTPClientTest extends TestCase 
{
   /**
    * logger
    */
    private static Logger log = null;
    
   
   /**
    * file created on the server during the upload from client to server. The server default dir is in /temp
    */
   private static final String DEFAULT_SERVER_DOWNLOAD_FILENAME = "server_input.txt";
   
   /**
    * file copied from the client to the server during the upload.
    */
   private static final String DEFAULT_CLIENT_DOWNLOAD_FILENAME = "/temp/client_input.txt";
   
   private TFTPTestServer server = null;

   /**
    * Used during in-memory transfers. Need a data structure that holds the indexed contents to be transferred, 
    */
   private static Map map;
   
   /**
    * The number off tests that are to be carried out. 
    */
   private int NUMBER_OF_TESTS = 4;

   /**
    * This is the setting that controls whether the tests are run against the local file system or in-memory.
    */
   private boolean fileSystem = false;
      
   /**
    * The tests to be performed by this test case.
    */   
   private static final int DOWNLOAD_TEST = 0;
   private static final int DOWNLOAD_OACK_TEST = 1;
   private static final int UPLOAD_TEST = 2;
   private static final int UPLOAD_OACK_TEST = 3;
      
   /**
    * this static initializer is required to set the BasicConfigurator so that the logging is set up correctly.
    */   
   static {
      BasicConfigurator.resetConfiguration();  // need this - find out why!!!
      BasicConfigurator.configure();
      
      log = Logger.getLogger(TFTPClientTest.class.getName());      
   }

   /**
    * This method starts up the server.
    */
   protected void setUp() 
   {    
      server = new TFTPTestServer(true); 
      this.createMap();
      
      try {      
         server.connect();
      } catch(Exception ex)
      {
         log.error("Problems starting the server!!");
      }
   }
   
   /**
    * This method shuts down the server.
    */
   protected void tearDown()
   {
      server.disconnect();
   }
        
   /**
    * This method provides the handle to test the download functionality of the TFTP module.
    * Tests the entire flow, the creation off the DATA object, the sending/receiving off the ACKS, 
    * data transfer itself.    
    *  
    */
   public void testDownload()
   {
      RRQ testRRQ = null;
      try {  
         long startTime = System.currentTimeMillis();
         if(getWriteToFileSystem())
         {
            
            File newFile = new File("/temp/testRRQ.txt");         
            FileOutputStream readFos = new FileOutputStream(newFile);
            
            TFTPClient client = new TFTPClient(TFTPClient.DEFAULT_HOSTNAME);
            
            testRRQ = client.initialiseDownload();
            
            boolean didWork = client.download(testRRQ, readFos);
   
            // remember to close the stream 
            readFos.close();                                                       
                               
            assertTrue(didWork); 
            
            // then need to check that the 2 files are the same....
            // find the size off file on server, and compare with that off the new file on the client,                 
            long newSize = newFile.length();
            File oldFile = new File("/temp/"+TFTPClient.DEFAULT_DOWNLOAD_FILENAME);         
            long oldSize = oldFile.length();
                     
            assertEquals(newSize, oldSize);
            
            FileInputStream newFis = new FileInputStream(newFile);
            FileInputStream oldFis = new FileInputStream(oldFile);
            
            int newValue,oldValue = 0;
            while (((newValue = newFis.read() )!= -1) && ((oldValue = oldFis.read() )!= -1)) 
            {
               assertEquals(newValue, oldValue);
            }                  
            
            // close streams
            newFis.close();
            oldFis.close();
         }
         else 
         {
            TFTPClient client = new TFTPClient(TFTPClient.DEFAULT_HOSTNAME);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            testRRQ = client.initialiseDownload(new Integer(DOWNLOAD_TEST).toString(), 0, 0);
            boolean didWork = client.download(testRRQ, baos);

            // remember to close the stream
            baos.close();

            assertTrue(didWork);

            // then need to work out whether the byte array that was transferred was the same length 
            // before and after.
            long newSize = new String(baos.toByteArray()).length();               
            long oldSize = ((String)map.get(new Integer(DOWNLOAD_TEST))).length();
         
            // log.debug("newSize = "+newSize);
            // log.debug("oldSize = "+oldSize);                             
            assertEquals(newSize, oldSize);                              

            // also compare the contents off the file. Lets never say I wasnt a thorough tester!!
            assertEquals(new String(baos.toByteArray()), ((String)map.get(new Integer(DOWNLOAD_TEST))));
            
            //log.debug("b4: "+new String(baos.toByteArray()));
            //log.debug("after: "+(String)map.get(new Integer(DOWNLOAD_TEST)));
                             
         }
         long duration = System.currentTimeMillis() - startTime;         
         log.info("Download complete: The time to run = "+duration);         
                                    
      } catch(IOException ioe) 
      {
         log.error("There is download error: "+ioe.getMessage());
         System.err.println("There is download error: "+ioe.getMessage());
      } catch(InstantiationException ie)
      {
         log.error("There is download error: "+ie.getMessage());
         System.err.println("There is download error: "+ie.getMessage());
      }
   }
   
   /**
    * This method provides the handle to test the download with Options functionality of the TFTP module.
    *
    * client                                    server
    * -------------------------------------------------
    * RRQ ------------------------------------->
    *                               <----------- OACK
    * ACK----------->
    *                               <----------- DATA
    * ACK----------->
    *                               <----------- DATA
    * ACK----------->
    *                               <----------- DATA
    * ACK----------->
    *    
    */
   public void testDownloadOACK()
   {
      RRQ testRRQ = null;
      try {  
         long startTime = System.currentTimeMillis();
 
         if(getWriteToFileSystem())
         {
            
            File newFile = new File("/temp/testRRQ.txt");         
            FileOutputStream readFos = new FileOutputStream(newFile);
         
            TFTPClient client = new TFTPClient(TFTPClient.DEFAULT_HOSTNAME);
            // method takes a filename, timeout and a transferSize.
            testRRQ = client.initialiseDownload(TFTPClient.DEFAULT_DOWNLOAD_FILENAME, 3, 2048);
            
            boolean didWork = client.download(testRRQ, readFos);
   
            // remember to close the stream 
            readFos.close();
                              
            assertTrue(didWork); 
         
            // then need to check that the 2 files are the same....
            // find the size off file on server, and compare with that off the new file on the client,         
            long newSize = newFile.length();
            File oldFile = new File("/temp/"+TFTPClient.DEFAULT_DOWNLOAD_FILENAME);
         
            long oldSize = oldFile.length();
                  
            assertEquals(newSize, oldSize);
            
            // TODO: 
            // need to test the options also: the timeout and the transferSize.
            log.debug("timeout = "+testRRQ.getTimeout());                    
         }
         else 
         {
            TFTPClient client = new TFTPClient(TFTPClient.DEFAULT_HOSTNAME);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            testRRQ = client.initialiseDownload(new Integer(DOWNLOAD_OACK_TEST).toString(), 10, 512);
            boolean didWork = client.download(testRRQ, baos);

            // remember to close the stream
            baos.close();

            assertTrue(didWork);

            // then need to work out whether the byte array that was transferred was the same length 
            // before and after.
            long newSize = new String(baos.toByteArray()).length();               
            long oldSize = ((String)map.get(new Integer(DOWNLOAD_OACK_TEST))).length();
            
            // log.debug("newSize = "+newSize);
            // log.debug("oldSize = "+oldSize);                             
            assertEquals(newSize, oldSize);                              

            // also compare the contents off the file. Lets never say I wasnt a thorough tester!!
            assertEquals(new String(baos.toByteArray()), ((String)map.get(new Integer(DOWNLOAD_OACK_TEST))));
            
            // need to test the options also: the timeout and the transferSize.
            log.debug("timeout = "+testRRQ.getTimeout()); 
            assertEquals(testRRQ.getTimeout(), client.getOptionTimeout());                          
            assertEquals(testRRQ.getTransferSize(), client.getOptionTransferSize());
         }
         long duration = System.currentTimeMillis() - startTime;         
         log.info("Download OACK complete: The time to run = "+duration);
                                 
      } catch(IOException ioe) 
      {
         log.error("There is download error: "+ioe.getMessage());
         System.err.println("There is download error: "+ioe.getMessage());
      } catch(InstantiationException ie)
      {
         log.error("There is download error: "+ie.getMessage());
         System.err.println("There is download error: "+ie.getMessage());
      }
   }
   
   
   /**
    * This method provides the handle to test the upload functionality of the TFTP module.
    * Tests the entire flow, the creation off the DATA objects, the sending/receiving off the ACKS, 
    * data transfer itself.    
    */
   public void testUpload()
   {
      WRQ testWRQ = null;
      try {
         long startTime = System.currentTimeMillis();
                     
         if (getWriteToFileSystem())
         {    
            File newFile = new File(DEFAULT_CLIENT_DOWNLOAD_FILENAME);         
            FileInputStream fis = new FileInputStream(newFile);
                                   
            TFTPClient client = new TFTPClient(TFTPClient.DEFAULT_HOSTNAME);
            
            testWRQ = client.initialiseUpload();
            boolean didWork = client.upload(testWRQ, fis);
            
            // remember to close the stream
            fis.close();
            
            assertTrue(didWork);
                                 
            FileInputStream newFis = new FileInputStream(newFile);
            FileInputStream oldFis = new FileInputStream("/temp/"+testWRQ.getFileName());
                        
            int newValue,oldValue = 0;
            while (((newValue = newFis.read() )!= -1) && ((oldValue = oldFis.read() )!= -1)) 
            {
               log.debug("new value = "+newValue+ " : oldValue = "+oldValue);
               assertEquals(newValue, oldValue);
            }                  
   
            // close streams
            newFis.close();
            oldFis.close();        
         }             
         else 
         {
                        
            TFTPClient client = new TFTPClient(TFTPClient.DEFAULT_HOSTNAME);
            testWRQ = client.initialiseUpload(new Integer(UPLOAD_TEST).toString(), 0, 0);               
                  
            ByteArrayInputStream bais = new ByteArrayInputStream(((String)map.get(new Integer(UPLOAD_TEST))).getBytes());
      
            boolean didWork = client.upload(testWRQ, bais);
            bais.close();   
   
            assertTrue(didWork);
   
            long sizeBefore = ((String)map.get(new Integer(UPLOAD_TEST))).getBytes().length;
            long sizeAfter = STTestFileSystem.getOutputStream().size();
   
            log.debug("Size before = "+((String)map.get(new Integer(UPLOAD_TEST))).getBytes().length);
            log.debug("Size after = "+ STTestFileSystem.getOutputStream().size());
            assertEquals(sizeBefore, sizeAfter);
   
            //now test the contents are the same...
            assertEquals(new String(STTestFileSystem.getOutputStream().toByteArray()) , (String)map.get(new Integer(UPLOAD_TEST)));
         }                            
         long duration = System.currentTimeMillis() - startTime;      
         log.info("Upload complete: The time to run = "+duration);      
         
      } catch(IOException ioe) 
      {
         log.error("There is an upload error: "+ioe.getMessage());
      } catch(InstantiationException ie)
      {
         log.error("There is an upload error: "+ie.getMessage());
      }
   }     
   
   /**
    * This method provides the handle to test the upload functionality of the TFTP module.
    *
    * client                                    server
    * -------------------------------------------------
    * WRQ ------------------------------------->
    *                               <----------- OACK
    * DATA----------->
    *                               <----------- ACK
    * DATA----------->
    *                               <----------- ACK
    * DATA----------->
    *                               <----------- ACK
    */
   public void testUploadOACK()
   {      
      try {
         long startTime = System.currentTimeMillis();
         TFTPClient client = new TFTPClient(TFTPClient.DEFAULT_HOSTNAME);
         WRQ testWRQ = null;
                             
         if(getWriteToFileSystem())
         {    
            File newFile = new File(DEFAULT_CLIENT_DOWNLOAD_FILENAME);         
            FileInputStream fis = new FileInputStream(newFile);
                                
            testWRQ = client.initialiseUpload(TFTPClient.DEFAULT_SERVER_UPLOAD_FILENAME, 5, 2048);
            
            boolean didWork = client.upload(testWRQ, fis);
         
            // remember to close the stream
            fis.close();
            
            assertTrue(didWork);
            
            long sizeBefore = newFile.length();
            long sizeAfter = testWRQ.getFileName().length();            
            assertEquals(sizeBefore, sizeAfter);
         }
         else 
         {
                                 
            testWRQ = client.initialiseUpload(new Integer(UPLOAD_OACK_TEST).toString(), 5, 2048);               
                           
            ByteArrayInputStream bais = new ByteArrayInputStream(((String)map.get(new Integer(UPLOAD_OACK_TEST))).getBytes());
               
            boolean didWork = client.upload(testWRQ, bais);
            bais.close();   
            
            assertTrue(didWork);
            
            long sizeBefore = ((String)map.get(new Integer(UPLOAD_OACK_TEST))).getBytes().length;
            long sizeAfter = STTestFileSystem.getOutputStream().size();
            
            //log.debug("Size before = "+((String)map.get(new Integer(UPLOAD_OACK_TEST))).getBytes().length);
            //log.debug("Size after = "+ STTestFileSystem.getOutputStream().size());
            assertEquals(sizeBefore, sizeAfter);
            
            //now test the contents are the same...
            assertEquals(new String(STTestFileSystem.getOutputStream().toByteArray()) , (String)map.get(new Integer(UPLOAD_OACK_TEST)));
            
         }
         assertEquals(testWRQ.getTimeout(), client.getOptionTimeout());                          
         assertEquals(testWRQ.getTransferSize(), client.getOptionTransferSize());
         
         long duration = System.currentTimeMillis() - startTime;      
         log.info("Upload OACK complete: The time to run = "+duration);
      
      } catch(IOException ioe) 
      {
         log.error("There is an upload error: "+ioe.getMessage());
      } catch(InstantiationException ie)
      {
         log.error("There is an upload error: "+ie.getMessage());
      }
   }     

   /**
    * This is a TFTP-specific property.
    * Determines whether the files created are written to the file system or 
    * stored in memory.    
    */
   private void setWriteToFileSystem(boolean fileSystem)
   {
      this.fileSystem = fileSystem;   
   }
   
   private boolean getWriteToFileSystem()
   {
      return this.fileSystem;   
   }     
   
   /** 
    * This method returns a Map off indices against byte arrays.
    * The map contains the byte arrays which are to be used for each test.
    * 
    * The indexing is as follows:
    * index 0 = download
    * index 1 = downloadOACK
    * index 2 = upload
    * index 3 = uploadOACK
    *   
    */
   private Map createMap()
   {
      map = new Hashtable();
      
      String[] contents = { "plain download test", "download test with options", "plain upload test", "upload test with options" };
       
      for (int i = 0; i < NUMBER_OF_TESTS; i++)
      {
         map.put(new Integer(i), contents[i]);
      }
      return map;                
   }
   
   /**
    * Convenience method thats returns the map.
    * getMap
    * @return
    */
   public static Map getMap() 
   {
      return map;
   }
}
