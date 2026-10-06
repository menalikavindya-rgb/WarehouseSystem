import java.time.LocalDate;
import model.Product;
import model.Batch;
import model.StockItem;

public class TestStock {
    public static void main(String[] args) {
        Product p = new Product(1, "Grade 25 Concrete Bags", "Concrete", 1800.00, 7);
        System.out.println(p);

        LocalDate made = LocalDate.of(2026, 10, 1);
        LocalDate ready = made.plusDays(p.getDefaultCuringDurationDays());
        Batch b = new Batch(1, p.getProductId(), made, ready, 500, "Curing");
        System.out.println(b);

        StockItem s = new StockItem(1, b.getBatchId(), "Zone A", 500, "Curing");
        System.out.println(s);
        System.out.println("Sellable? " + s.isSellable());

        // Trying a wrong value, handled with try / catch / finally
        try {
            s.setQuantity(-5);
        } catch (IllegalArgumentException e) {
            System.out.println("Error caught: " + e.getMessage());
        } finally {
            System.out.println("Quantity check finished.");
        }
    }
}
