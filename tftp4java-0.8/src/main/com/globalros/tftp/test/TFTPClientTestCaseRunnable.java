/**
 * Title:        TFTPClientTestCaseRunnable<p>
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

/**
 * class:   TFTPClientTestCaseRunnable
 * package: com.globalros.mcp.test
 * project: tftp4java
 */
public abstract class TFTPClientTestCaseRunnable implements Runnable
{

   public static Logger log;
      
   static {
      BasicConfigurator.resetConfiguration();
      BasicConfigurator.configure();

      log = Logger.getLogger(TFTPClientTestCaseRunnable.class);
   }
   
   /**
    * Method is implemented in the Test Case for the MCP module.       
    * 
    * @throws Runnable
    */
    public abstract void runTestCase() throws Throwable;
   
    private int iterations;
    private String testName;
    private int index;
    
    public void run() 
    {
        try
        {
            // this is a temporary variable that displays the number off downloads/uploads performed.
            int iterationCount = 0;

            for (int i = 0; i < this.getNumberOfRuns(); i++)
            {
                runTestCase();
                iterationCount = i;
                log.debug(
                        "******* Iteration :" + ++iterationCount + " completed for: "+ Thread.currentThread().getName()  +"!!!");
            }
        } catch (Throwable t)
        {
            // need a way to handle the exception....!!
            log.error("There is a problem running the test: for "+ Thread.currentThread().getName() + ". Error: "+t.getMessage());         
            //throw new RosTestException("There is a problem running the test: "+t.getMessage());
        }
    }   

    /**
     * Getter and setter methods for some off the properties that are read from a properties
     * file.        
     */
    public String getTestName()
    {
        return testName;   
    }
    public void setTestName(String testName)
    {
        this.testName = testName;
    }
    
    public void setNumberOfRuns(int iterations) 
    {
        this.iterations = iterations;
    }
    
    public int getNumberOfRuns() 
    {
        return iterations;
    }          
    
    /**
     * This the index is the number thread that is being processed.
     * 
     * For example in TFTP testing, it is useful to have a thread index that is 
     * associated with a file that is downloaded/uploaded. This can be referenced
     * using the index.
     * For example, thread 1 downloads files[0], thread 2 downloads files[1] etc.... 
     * 
     */
    public void setIndex(int index) 
    {
        this.index = index;
    }
    
    public int getIndex() 
    {
        return this.index;
    }
}