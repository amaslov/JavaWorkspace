/**
 * Title:        RosTestException.java<p>
 * Description:  <p>
 * Copyright:    (c) Roses B.V. 2003<p>
 * Company:      Roses B.V.<p>
 *
 * @author      Kinshuk
 * Created:      21-jul-2003
 */
package com.globalros.tftp.test;

/**
 * class:   RosTestException
 * package: com.globalros.test
 * project: ROSTest
 * 
 * This class is a generic RosTestException, is thrown if there is any kind off error whilst test cases are 
 * run. 
 * 
 */
public class TestException extends Exception
{

   /**
    * RosTestException constructor
    * 
    */
   public TestException()
   {
      super();      
   }
   
   public TestException(String message)
   {
      super(message);   
   }
   

}
