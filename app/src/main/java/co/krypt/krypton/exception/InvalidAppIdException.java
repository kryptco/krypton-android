package co.krypt.krypton.exception;

public class InvalidAppIdException extends Exception {
    public InvalidAppIdException(String message) {
        super(message);
    }
    public InvalidAppIdException(Throwable t) {
        super(t);
    }
}
