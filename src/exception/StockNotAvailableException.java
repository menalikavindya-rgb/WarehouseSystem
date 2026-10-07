package exception;

public class StockNotAvailableException extends Exception {

    public StockNotAvailableException(String message) {
        super(message);
    }
}