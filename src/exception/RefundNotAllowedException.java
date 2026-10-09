package exception;

public class RefundNotAllowedException extends Exception {

    public RefundNotAllowedException(String message) {
        super(message);
    }
}