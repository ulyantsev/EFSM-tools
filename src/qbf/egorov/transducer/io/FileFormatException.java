/* 
 * Developed by eVelopers Corporation, 2009
 */
package qbf.egorov.transducer.io;

/**
 * @author kegorov
 *         Date: Jun 18, 2009
 */
public class FileFormatException extends Exception {
    public FileFormatException() {
	    super();
    }

    public FileFormatException(String message) {
	    super(message);
    }

    public FileFormatException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileFormatException(Throwable cause) {
        super(cause);
    }
}
