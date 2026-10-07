package exception;

public class PaymentRequiredException extends Exception {

    public PaymentRequiredException(String message) {
        super(message);
    }
}