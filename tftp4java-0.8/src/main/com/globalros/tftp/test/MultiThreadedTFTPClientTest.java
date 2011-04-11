/**
 * Title:        MultiThreadedTFTPClientTest<p>
 * Description:  <p>
 * Copyright:    (c) Roses B.V. 2003<p>
 * Company:      Roses B.V.<p>
 *
 * @author      Kinshuk
 * Created:      7-jul-2003
 */
package com.globalros.tftp.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import com.globalros.tftp.client.TFTPClient;
import com.globalros.tftp.common.FRQ;
import com.globalros.tftp.common.RRQ;
import com.globalros.tftp.common.WRQ;

/**
 * class:   MultiTest
 * package: com.globalros.tftp.client
 * project: tftp4java
 * 
 * This class provides the specifics for the download/upload testcases.
 * 
 * Also provides the mechanism to create the correct features for the tests to be performed.
 * Namely creating the required files etc on the file system.
 * 
 */
public class MultiThreadedTFTPClientTest extends MultiThreadedTestCase
{

   public static Logger log;

   /**
    * An array off Read Request objects
    */
   public RRQ[] testRRQs;

   /**
    * An array off Write Request objects
    */
   public WRQ[] testWRQs;

   /**
    * Number off threads to use.    
    */
   private final static int NUM_THREADS = 20;

   /**
    * Number off times to run the download.
    */
   private final static int NUMBER_OF_DOWNLOADS = 2;
   
   /**
    * Number off times to run the upload.
    */
   private final static int NUMBER_OF_UPLOADS = 1;
   
   /**
    * file on server that are to be downloaded [N.B. these files need to be created by the helper method.]
    */
   private static String[] downloadFileNames;

   /**
    * file that client wants downloaded  
    */
   private static String[] tempFiles;

   /**
    * file that the client is going to upload [N.B. these files need to be created by the helper method.]    
    */
   private static String[] client_uploadFileNames;

   /**
    * file that server wants uploaded 
    */
   private static String[] server_uploadFileNames;
   
   static {
      BasicConfigurator.resetConfiguration();
      BasicConfigurator.configure();

      log = Logger.getLogger(MultiThreadedTFTPClientTest.class);
   }

   /**
    * MultiTest constructor
    * @param s
    */
   public MultiThreadedTFTPClientTest(String s)
   {
      super(s);
   }
        
   public void setUp() throws InstantiationException, UnknownHostException
   {
      testRRQs = this.initialiseDownload();
      testWRQs = this.initialiseUpload();
   }

   /**
    * Sets up the features for the download operation to be tested.
    * Namely creates an array off RRQ objects and invokes the helper method to create the files 
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
      this.createDownloadFiles();

      testRRQs = new RRQ[NUM_THREADS];

      for (int i = 0; i < NUM_THREADS; i++)
      {
         testRRQs[i] = new RRQ();
         testRRQs[i].setFileName(downloadFileNames[i]);
         testRRQs[i].setMode(FRQ.OCTET_MODE);
         testRRQs[i].setAddress(
            InetAddress.getByName(TFTPClient.DEFAULT_HOSTNAME));

         testRRQs[i].setPort(TFTPClient.DEFAULT_PORT);
      }
      return testRRQs;
   }

   private WRQ[] initialiseUpload()
      throws InstantiationException, UnknownHostException
   {
      // need to call the helper method that ensures all the files that are to be uploaded from 
      // the client exist already on the file system!!
      this.createUploadFiles();

      testWRQs = new WRQ[NUM_THREADS];
      
      for (int i = 0; i < NUM_THREADS; i++)
      {
         testWRQs[i] = new WRQ();
         testWRQs[i].setFileName(server_uploadFileNames[i]);
         testWRQs[i].setMode(FRQ.OCTET_MODE);
         testWRQs[i].setAddress(
            InetAddress.getByName(TFTPClient.DEFAULT_HOSTNAME));
         testWRQs[i].setPort(TFTPClient.DEFAULT_PORT);
      }
      return testWRQs;
   }

   /**
    * testDownload
    * 
    */
   public void testDownload()
   {
      TFTPClientTestCaseRunnable ctcr[] =
          new TFTPClientTestCaseRunnable[NUM_THREADS];
      
         for (int i = 0; i < ctcr.length; i++)
         {                       
            // need to create
            final int j = i;
                                    
            ctcr[i] = new TFTPClientTestCaseRunnable()
            {               
               public void runTestCase()
               {
                  try
                  {
                     // used to calculate the length off time to perform the download...   
                     long startTime = System.currentTimeMillis();
   
                     File newFile = new File(tempFiles[j]);
                     FileOutputStream readFos = new FileOutputStream(newFile);
   
                     TFTPClient client =
                        new TFTPClient(TFTPClient.DEFAULT_HOSTNAME);
   
                     boolean didWork = client.download(testRRQs[j], readFos);
                     assertTrue(didWork);
   
                     // then need to check that the 2 files are the same....
                     // find the size off file on server, and compare with that off the new file on the client,         
                     long newSize = newFile.length();
                     File oldFile = new File("/temp/" + downloadFileNames[j]);
   
                     long oldSize = oldFile.length();
   
                     assertEquals(newSize, oldSize);
   
                     long duration = System.currentTimeMillis() - startTime;
                     log.debug(
                        "******** The time for download to run = " + duration);
   
                  } catch (IOException ioe)
                  {
                     log.error("There is download error: " + ioe.getMessage());
                  } catch (InstantiationException ie)
                  {
                     log.error("There is download error: " + ie.getMessage());
                  }
               }
            };
            
            ctcr[i].setNumberOfRuns(NUMBER_OF_DOWNLOADS);
         }
         // run the threads!!!   
         this.runTestCaseRunnables(ctcr);   
         log.debug("******** Number off threads run = " + ctcr.length);    
         log.debug("******** Number off download iterations = "+NUMBER_OF_DOWNLOADS);        
   }

   /**
    * testUpload
    */
   public void testUpload()
   {      
      TFTPClientTestCaseRunnable ctcr[] =
         new TFTPClientTestCaseRunnable[NUM_THREADS];

      // used to- calculate the length off time to perform the download...   
      for (int i = 0; i < ctcr.length; i++)
      {
         final int j = i;
         ctcr[i] = new TFTPClientTestCaseRunnable()
         {
            public void runTestCase()
            {
               try
               {

                  long startTime = System.currentTimeMillis();

                  File newFile = new File("/temp/"+client_uploadFileNames[j]);
                  FileInputStream writeFos = new FileInputStream(newFile);

                  TFTPClient client =
                     new TFTPClient(TFTPClient.DEFAULT_HOSTNAME);

                  boolean didWork = client.upload(testWRQs[j], writeFos);
                  assertTrue(didWork);

                  // then need to check that the 2 files are the same....
                  // find the size off file on server, and compare with that off the new file on the client,         
                  long newSize = newFile.length();
                  File oldFile = new File("/temp/" + client_uploadFileNames[j]);

                  long oldSize = oldFile.length();

                  log.debug("newSize = " + newSize);
                  log.debug("oldSize = " + oldSize);
                  log.debug("old file name = /temp/" + server_uploadFileNames[j]);
                  assertEquals(newSize, oldSize);

                  long duration = System.currentTimeMillis() - startTime;
                  log.debug(
                     "******** The time for upload to run = " + duration);

               } catch (IOException ioe)
               {
                  log.error("There is upload error: " + ioe.getMessage());
               } catch (InstantiationException ie)
               {
                  log.error("There is upload error: " + ie.getMessage());
               }
            }
         };        
         ctcr[i].setNumberOfRuns(NUMBER_OF_UPLOADS);
      }
      // run the threads!!!
      this.runTestCaseRunnables(ctcr);

      log.debug("******** Number off threads run = " + ctcr.length);
      log.debug("******** Number off upload iterations = "+NUMBER_OF_UPLOADS);                 
   }

   /**
    * Helper method that creates all the files that are to be downloaded during the 
    * download tests.
    * 
    */
   private void createDownloadFiles()
   {
      downloadFileNames = new String[NUM_THREADS];
      File file = null;
      FileOutputStream fos = null;
      String content = null;
      String fileName = null;

      byte[] byteArray;
      for (int i = 0, j = 1; j <= NUM_THREADS; j++)
      {
         fileName = "download_test" + j + ".txt";
         file = new File("/temp/"+fileName);
         try
         {
            fos = new FileOutputStream(file);

            content =   "This is a download test file." +
                        "This is a download test file." +
                        "This is a download test file." +
                        "This is a download test file." +
                        "This is a download test file." +
                        "This is a download test file." +
                        "This is a download test file." +
                        "This is a download test file." +
                        "This is a download test file." +
                        "This is a download test file." +
                        "This is a download test file." +
                        "This is a download test file." +
                        "This is a download test file." +
                        "This is a download test file." +
                        "This is a download test file." +
                        "This is a download test file." +
                        "This is a download test file." +
                        "This is a download test file." +
                        "This is a download test file." +
                        "This is a download test file." +
                        "This is a download test file." +
                        "This is a download test file." +
                        "This is a download test file." +
                        "This is a download test file." +
                        "This is a download test file." +
                        "This is a download test file." +
                        "This is a download test file." +
                        "This is a download test file." +
                        "This is a download test file." +
                        "This is a download test file." +
                        "This is a download test file." +
                        "This is a download test file." +
                        "This is a download test file." +
                        "This is a download test file." +
                        "This is a download test file." +
                        "This is a download test file." +
                        "This is a download test file." +
                        "This is a download test file." +
                        "This is a download test file." +
                        "This is a download test file." +
                        "This is a download test file." +
                        "This is a download test file." +
                        "This is a download test file." +
                        "This is a download test file." +
                        "This is a download test file." +
                        "This is a download test file." +
                        "This is a download test file." +
                        "This is a download test file." +
                        "This is a download test file." +
                        "This is a download test file." +
                        "This is a download test file." +
                        "This is a download test file." +
                        "This is a download test file." +
                        "This is a download test file." +            
                        + j;
                                                
            byteArray = content.getBytes();
            fos.write(byteArray);
            fos.close();
         } catch (IOException ioe)
         {
            log.error("whoops : " + ioe.getMessage());
         }

         downloadFileNames[i++] = fileName;
      }

      tempFiles = new String[NUM_THREADS];

      int k = 0;
      while (k < NUM_THREADS)
      {
         tempFiles[k] = "/temp/testRRQ" + ++k + ".txt";
      }
   }

   /**
    * Helper method that creates the files that are to be uploaded during the upload tests.
    * 
    */
   private void createUploadFiles()
   {
      client_uploadFileNames = new String[NUM_THREADS];
      File file = null;
      FileOutputStream fos = null;
      String content = null;
      String fileName = null;

      byte[] byteArray;
      for (int i = 0, j = 1; j <= NUM_THREADS; j++)
      {
         fileName = "upload_test" + j + ".txt";
         file = new File("/temp/" + fileName);
         try
         {
            fos = new FileOutputStream(file);

            content = "This is a upload test file: " + j;
            byteArray = content.getBytes();
            fos.write(byteArray);
            fos.close();

         } catch (IOException ioe)
         {
            log.error("whoops : " + ioe.getMessage());
         }

         client_uploadFileNames[i++] = fileName;
      }
      server_uploadFileNames = new String[NUM_THREADS];

      int k = 0;
      while (k < NUM_THREADS)
      {
         server_uploadFileNames[k] = "testWRQ" + ++k + ".txt";
      }
   }
}
