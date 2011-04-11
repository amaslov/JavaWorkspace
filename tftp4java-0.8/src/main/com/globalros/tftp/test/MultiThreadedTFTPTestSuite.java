/**
 * Title:        MultiThreadedTFTPTestSuite.java<p>
 * Description:  <p>
 * Copyright:    (c) Roses B.V. 2003<p>
 * Company:      Roses B.V.<p>
 *
 * @author      Kinshuk
 * Created:      25-jul-2003
 */
package com.globalros.tftp.test;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * class:   MultiThreadedTFTPTestSuite
 * package: com.globalros.tftp.test
 * project: tftp4java
 * 
 * This class provides a test suite for the MultiThreaded tests cases for the 
 * TFTP4java module.
 * 
 * Refer to each test case for further detail about individual test criteria.
 * 
 */
public class MultiThreadedTFTPTestSuite extends TestSuite
{
   public static Test suite() {
      TestSuite suite = new TestSuite("Test for MultiThreaded TFTP Service");
      suite.addTestSuite(MTDownloadTest.class);
      suite.addTestSuite(MTUploadTest.class);
      return suite;
   }
   
   public static void main (String[] args) {
      junit.textui.TestRunner.run (suite());
   }
}
