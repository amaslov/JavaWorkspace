/**
 * Title:        MTDownloadTest.java<p>
 * Description:  <p>
 * Copyright:    (c) Roses B.V. 2003<p>
 * Company:      Roses B.V.<p>
 *
 * @author      Kinshuk
 * Created:      23-jul-2003
 */
package com.globalros.tftp.test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import junit.framework.Assert;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import com.globalros.tftp.client.TFTPClient;
import com.globalros.tftp.common.FRQ;
import com.globalros.tftp.common.RRQ;

/**
 * class:   MTDownloadTest
 * package: com.globalros.tftp.test
 * project: tftp4java
 * 
 * This class provides the test case to test the download of the TFTP4java module.
 * 
 * The test framework uses its own TFTPTestServer, which it starts and shuts down internally.
 * 
 * There are 2 options for the test, either create files on the server and transfer them 
 * to client. This is performed by physical files being created and transferred so using the
 * local file system.
 * 
 * However, a neater way is the in-memory testing. Using this technique byte arrays are created in-memory
 * and stored against an Integer key in a Map.
 * This technique allows us to develop a TFTPTestFileSystem class which is based on the TFTP FilSystem, 
 * however it provides a ByteArrayOutputStream layer.
 * 
 * Anyway using either mechanism, the JUnit tests basically check the size off the files/byte arrays 
 * that are being transferred.  
 *   
 * 
 */
public class MTDownloadTest extends MultiThreadedTestCase
{

   public static Logger log;

   /**
    * An array off Read Request objects
    */
   public RRQ[] testRRQs;

   /**
    * Number off threads to use. This is the default value, if the properties file cannot be read.    
    */
   private final static int NUM_THREADS = 5;

   /**
    * Number off times to run the download. This is the default value, if the properties file cannot be read. 
    */
   private final static int NUMBER_OF_DOWNLOADS = 2;

   /**
    * this represents physical files on server that are to be downloaded 
    *  
    * [N.B. these need to be created by the helper method.]
    */
   private static String[] downloadFileNames;

   /**
    * this represents in-memory byte array representation off the file contents...
    * 
    * [N.B. these need to be created by the helper method.]    
    */
   private static String[] downloadByteArrays;
   
   /**
    * This contains a map off the byte array and an index, so we can check them easily.
    */
   private static Map map; 
   
   /**
    * file that client wants downloaded  
    */
   private static String[] tempFiles;

   /**
    * Refers to the property file for the TFTP Testing module.
    */
   private final static String PROPERTY_FILE = "TFTPTest.properties";

   /**
    * Configuration data....
    */
   private Properties props;
   private int numIterations;
   private int numThreads;
   private boolean fileSystem;
   
   private static int threadCount;

   private TFTPTestServer server;
   
   static {
      BasicConfigurator.resetConfiguration();
      BasicConfigurator.configure();

      log = Logger.getLogger(MTDownloadTest.class);
   }

   /**
    * MTDownloadTest constructor
    * 
    */
   public MTDownloadTest(String name)
   {
      super(name);
   }

   public void setUp() throws InstantiationException, UnknownHostException
   {
      // first thing to do is load up properties!!!
      this.loadProperties();
      
      map = new Hashtable(this.getNumberOfThreads()); 
      testRRQs = this.initialiseDownload();
      
      server = new TFTPTestServer(false);
      try {      
         server.connect();         
      } catch(Throwable t)
      {
         log.error("Problems starting the server!!"+t.getMessage());                 
      }      
   }

   /**
    * Clean disconnect for the server after the test has been run.
    */
   public void tearDown()
   {  
      server.disconnect();   
   }
   
   /**
    * Sets up the features for the download operation to be tested.
    * 
    * Creates an array off RRQ objects and invokes the helper method to create the files 
    * that are downloaded from server to client. 
    * 
    * @return an array off RRQ objects
    * 
    * @throws InstantiationException
    * @throws UnknownHostException
    */
   private RRQ[] initialiseDownload()
      throws InstantiationException, UnknownHostException
   {
      // need to call the helper method that ensures all the files that the test is going to download to the client
      // actually exists on the file system!!!
      this.createDownloadFiles(this.getWriteToFileSystem());

      testRRQs = new RRQ[this.getNumberOfThreads()];

      for (int i = 0; i < this.getNumberOfThreads(); i++)
      {
         testRRQs[i] = new RRQ();
         
         // fileName depends on the type off download, in-memory or file system
         if(this.getWriteToFileSystem())
            testRRQs[i].setFileName(downloadFileNames[i]);
         else
            testRRQs[i].setFileName(new Integer(i).toString()); // pass the key for the map, which hold the byte arrays as values.
               
         testRRQs[i].setMode(FRQ.OCTET_MODE);
         testRRQs[i].setAddress(
            InetAddress.getByName(TFTPClient.DEFAULT_HOSTNAME));

         testRRQs[i].setPort(TFTPClient.DEFAULT_PORT);
      }
      return testRRQs;
   }

   /**
    * Load the properties file that configures the number off threads/iterations etc that the unit tests run against.
    * If the properties file cannot be found, then defaults declared above are used.  
    *
    */
   public void loadProperties()
   {
      try
      {
         // write code that dynamically loads the properties for this Test Module....
         InputStream in = ClassLoader.getSystemResourceAsStream(PROPERTY_FILE);
         props = new Properties();
         props.load(in);

         // set configuration for Request Conf Block test...
         this.setNumberOfThreads(
            new Integer(props.getProperty("TFTPNumberOfThreadsForDownload"))
               .intValue());
         this.setNumberOfIterations(
            new Integer(props.getProperty("TFTPNumberOfIterationsForDownload"))
               .intValue());
         
         this.setWriteToFileSystem(
            new Boolean(props.getProperty("TFTPWriteToFileSystemForDownload"))
               .booleanValue());
               
         log.debug("Loaded properties from : "+ PROPERTY_FILE + " : Number of Threads = "+this.getNumberOfThreads()+ " : Number of iterations = "+this.getNumberOfIterations());                           
      } // a bit lazy really but if we cannot find the props file, just use defaults that are declared in this file! 
      catch (Throwable t)
      {
         log.error(
            "Problem: Unable to load properties for module: TFTP => Using defaults!!!"
               + t.getMessage());
               
         // call some generic methods that basically set the same number off threads/iterations for each unit test!         
         this.setNumberOfThreads(NUM_THREADS);
         this.setNumberOfIterations(NUMBER_OF_DOWNLOADS);
         this.setWriteToFileSystem(false);
      }
   }

   /**
    * testDownload
    * 
    */
   public void testDownload()
   {
      try
      {
         log.debug("test");
         TFTPClientTestCaseRunnable ctcr[] =
            new TFTPClientTestCaseRunnable[this.getNumberOfThreads()];
            
         for (int i = 0; i < ctcr.length; i++)
         {
            log.debug("test"+i);
            ctcr[i] = new MyTestCaseImpl();
         
            ctcr[i].setTestName(props.getProperty("TFTPTestName"));
            ctcr[i].setNumberOfRuns(this.getNumberOfIterations());
            ctcr[i].setIndex(i);       
         }
         log.debug("testx");
         // run the threads!!!   
         this.runTestCaseRunnables(ctcr);
         log.debug("******** Number off threads run = " + ctcr.length);
         log.debug(
            "******** Number off request config iterations = "
               + this.getNumberOfIterations());
      } catch (RuntimeException e)
      {
         // TODO Auto-generated catch block
         log.error(e.getMessage());
         e.printStackTrace();
      }

   }

   /**
    * This inner class provides the implementation for the runTestCase() which contains 
    * the test logic. 
    */
   class MyTestCaseImpl extends TFTPClientTestCaseRunnable
   {           
      /* (non-Javadoc)
       * @see com.globalros.test.RosTestCaseRunnable#runTestCase()
       */
      public void runTestCase()
      {
         log.debug("running test");
         try
         {            
            // used to calculate the length off time to perform the download...   
            long startTime = System.currentTimeMillis();

            // need to check whether we are writing/reading from/to the local file system
            // or processing is in-memory.
            if(getWriteToFileSystem())
            {            

               File newFile = new File(tempFiles[this.getIndex()]);
               FileOutputStream readFos = new FileOutputStream(newFile);
   
               TFTPClient client = new TFTPClient(TFTPClient.DEFAULT_HOSTNAME);
               
               boolean didWork = client.download(testRRQs[this.getIndex()], readFos);
               
               // remember to close the stream
               readFos.close();
               
               assertTrue(didWork);
   
               // then need to check that the 2 files are the same....
               // find the size off file on server, and compare with that off the new file on the client,         
               long sizeAfter = newFile.length();
               File oldFile = new File("/temp/" + downloadFileNames[this.getIndex()]);
   
               long sizeBefore = oldFile.length();
   
               assertEquals(sizeAfter, sizeBefore);
            } 
            else
            {
               TFTPClient client = new TFTPClient(TFTPClient.DEFAULT_HOSTNAME);
               ByteArrayOutputStream baos = new ByteArrayOutputStream();
               
               boolean didWork = client.download(testRRQs[this.getIndex()], baos);
               
               // remember to close the stream
               baos.close();
               
               assertTrue(didWork);
               
               // then need to work out whether the byte array that was transferred was the same length 
               // before and after.
               long newSize = new String(baos.toByteArray()).length();               
               long oldSize = downloadByteArrays[this.getIndex()].length();                             
               assertEquals(newSize, oldSize);                              
               
               // also compare the contents off the file. Lets never say I wasnt a thorough tester!!
               assertEquals(new String(baos.toByteArray()), downloadByteArrays[this.getIndex()]);               
            }
            
            long duration = System.currentTimeMillis() - startTime;
            log.debug("******** The time for download to run = " + duration);

         } catch (IOException ioe)
         {
            log.error("There is download error: " + ioe.getMessage());
            Assert.fail("There is a download error: "+ioe.getMessage());
         } catch (InstantiationException ie)
         {
            log.error("There is download error: " + ie.getMessage());
            Assert.fail("There is a download error: "+ie.getMessage());
         }
      }           
   };

   /**
    * This method checks whether the flag is set to create the files in-memory 
    * or on the file system.
    *
    * @param boolean flag to determine whether to create in-memory or on file system.
    */
   private void createDownloadFiles(boolean fileSystem)
   {
      // if fileSystem == true, then create files that physically exist...      
      // if fileSystem == false, then create files in memory....            
      log.debug("Check: write to fileSystem: "+this.getWriteToFileSystem());
      if (!this.getWriteToFileSystem()) {
      
         downloadByteArrays = new String[this.getNumberOfThreads()];                 
         for(int i = 0; i < this.getNumberOfThreads(); i++) 
         
         {                
            downloadByteArrays[i] = "This is a download byte array." + i;
            map.put(new Integer(i), downloadByteArrays[i]); 
         }
      } else 
      {
         this.createDownloadFiles();
      }
      //log.debug("Created array = "+downloadByteArrays[0]);     
   }
      
   private void createDownloadFiles()
   {         
      
      File file = null;
      FileOutputStream fos = null;
      String fileName = null;
      String content = null;
      downloadFileNames = new String[this.getNumberOfThreads()];
      byte[] byteArray;
      
      for (int i = 0, j = 1; j <= this.getNumberOfThreads(); j++)
      {
         fileName = "download_test" + j + ".txt";
         file = new File("/temp/" + fileName);
         try
         {
            fos = new FileOutputStream(file);

            content =
                    "This is a download test file."
                  + "This is a download test file."
                  + "This is a download test file."
                  + "This is a download test file."
                  + "This is a download test file."
                  + "This is a download test file."
                  + "This is a download test file."
                  + "This is a download test file."
                  + "This is a download test file."
                  + "This is a download test file."
                  + "This is a download test file."
                  + "This is a download test file."
                  + "This is a download test file."
                  + "This is a download test file."
                  + "This is a download test file."
                  + "This is a download test file."
                  + "This is a download test file."
                  + "This is a download test file."
                  + "This is a download test file."
                  + "This is a download test file."
                  + "This is a download test file."
                  + "This is a download test file."
                  + "This is a download test file."
                  + "This is a download test file."
                  + "This is a download test file."
                  + "This is a download test file."
                  + "This is a download test file."
                  + "This is a download test file."
                  + "This is a download test file."
                  + "This is a download test file."
                  + "This is a download test file."
                  + "This is a download test file."
                  + "This is a download test file."
                  + "This is a download test file."
                  + "This is a download test file."
                  + "This is a download test file."
                  + "This is a download test file."
                  + "This is a download test file."
                  + "This is a download test file."
                  + "This is a download test file."
                  + "This is a download test file."
                  + "This is a download test file."
                  + "This is a download test file."
                  + "This is a download test file."
                  + "This is a download test file."
                  + "This is a download test file."
                  + "This is a download test file."
                  + "This is a download test file."
                  + "This is a download test file."
                  + "This is a download test file."
                  + "This is a download test file."
                  + "This is a download test file."
                  + "This is a download test file."
                  + "This is a download test file."
                  + j;

            byteArray = content.getBytes();
            fos.write(byteArray);
            fos.close();
         } catch (IOException ioe)
         {
            log.error("Error : " + ioe.getMessage());
         }

         downloadFileNames[i++] = fileName;
      }

      tempFiles = new String[this.getNumberOfThreads()];

      int k = 0;
      while (k < this.getNumberOfThreads())
      {
         tempFiles[k] = "/temp/testRRQ" + ++k + ".txt";
      }
   }

   /************************************************************************************
    * Setter/Getter methods for the params that can be obtained from the properties file.
    ************************************************************************************/
   public void setNumberOfThreads(int numThreads)
   {
      this.numThreads = numThreads;
   }

   public int getNumberOfThreads()
   {
      return this.numThreads;
   }

   public void setNumberOfIterations(int numIterations)
   {
      this.numIterations = numIterations;
   }

   public int getNumberOfIterations()
   {
      return this.numIterations;
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
    * This map holds the in-memory byte array data. The key is an counter-based Integer....
    * 
    */
   public static Map getMap()
   {
      return map;
   }
}
