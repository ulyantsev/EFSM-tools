/*
 * Developed by eVelopers Corporation - 26.05.2008
 */
package verification.ltl.buchi.translator;

public class TranslationException extends RuntimeException {
    TranslationException(String message) {
        super(message);
    }

    TranslationException(Throwable cause) {
        super(cause);
    }
}
