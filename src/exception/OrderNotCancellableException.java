package exception;

public class OrderNotCancellableException extends Exception {

    public OrderNotCancellableException(String message) {
        super(message);
    }
}