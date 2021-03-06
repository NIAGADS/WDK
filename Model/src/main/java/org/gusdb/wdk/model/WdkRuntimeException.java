package org.gusdb.wdk.model;

/**
 * This exception is thrown when WDK is not able to recover from a failure.
 * 
 * TODO - this exception might be redundant with WdkModelException, might need
 * to refactor these two into one exception.
 * 
 * @author jerric
 * 
 */
public class WdkRuntimeException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public WdkRuntimeException(String message) {
    super(message);
  }

  public WdkRuntimeException(Throwable cause) {
    super(cause);
  }

  public WdkRuntimeException(String message, Throwable cause) {
    super(message, cause);
  }

}
