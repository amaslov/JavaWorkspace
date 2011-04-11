/**
 * Title:        RosMultiThreadedTestCase.java<p>
 * Description:  <p>
 * Copyright:    (c) Roses B.V. 2003<p>
 * Company:      Roses B.V.<p>
 *
 * @author      Kinshuk
 * Created:      21-jul-2003
 */
package com.globalros.tftp.test;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

//import com.globalros.test.RosTestCaseRunnable;

import junit.framework.TestCase;
import junit.framework.TestResult;

/**
 * class:   RosMultiThreadedTestCase
 * package: com.globalros.test
 * project: ROSTest
 * 
 * 
 * This class is a generic class that is used for all multi-threaded testing
 * throughout the ROS application.
 * 
 * This class spawns off a configurable number off threads that perform the
 * testing operations.
 * 
 * It is required because JUnit only provides a single-threaded testing framework. 
 * 
 */
public abstract class MultiThreadedTestCase extends TestCase
{

   public static Logger log;
  
   static {
      BasicConfigurator.resetConfiguration();
      BasicConfigurator.configure();

      log = Logger.getLogger(MultiThreadedTestCase.class);
   }

   /**
    * The threads that will be running.    
    */
   private Thread threads[] = null;

   /**
    * The tests TestResult    
    */
   private TestResult testResult = null;

   /**
    * Simple constructor.    
    */
   public MultiThreadedTestCase(final String s)
   {
      super(s);
   }

   /**
    * Need to override the run() so we can save the TestResult.     
    */
   public void run(final TestResult result)
   {
      testResult = result;
      super.run(result);
      testResult = null;
   }

   /**
    * Method that controls the thread processing within the test cycle.
    * The aim is for all the threads to start together and also finish together.
    * 
    * @param runnables
    */
   protected void runTestCaseRunnables(TFTPClientTestCaseRunnable[] runnables)
   {
      if (runnables == null)
         throw new IllegalArgumentException("runnables is null");

      // populate the thread array and ...
      threads = new Thread[runnables.length];

      for (int i = 0; i < threads.length; i++)
      {
         threads[i] = new Thread(runnables[i]);
      }

      // start them all off at same time...
      for (int i = 0; i < threads.length; i++)
      {
         //log.debug("kicking off Thread number :"+i);
         threads[i].start();
      }

      // wait until all the threads finish and then return...!!
      try
      {

         for (int i = 0; i < threads.length; i++)
         {
            //log.debug("Thread number :"+i+ " joining.");
            threads[i].join();
         }
      } catch (InterruptedException ie)
      {
         log.error("Thread join interrupted!!!!");
      }
   }

}
