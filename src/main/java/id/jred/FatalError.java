package id.jred;

public class FatalError extends RuntimeException {
    public FatalError(String message) {
        super(message);
    }
}
