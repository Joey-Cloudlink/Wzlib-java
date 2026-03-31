package wzlib;

public class WzException extends RuntimeException {
    public WzException(String message) {
        super(message);
    }

    public WzException(String message, Throwable cause) {
        super(message, cause);
    }
}
