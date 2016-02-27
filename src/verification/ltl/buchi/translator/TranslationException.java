/*
 * Developed by eVelopers Corporation - 26.05.2008
 */
package verification.ltl.buchi.translator;

public class TranslationException extends RuntimeException {
    public TranslationException(String message) {
        super(message);
    }

    public TranslationException(Throwable cause) {
        super(cause);
    }
}
