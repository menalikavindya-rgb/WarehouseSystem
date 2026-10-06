import java.sql.SQLException;
import java.util.ArrayList;
import dao.ProductDAO;
import exception.ProductNotFoundException;
import model.Product;

public class TestProducts {

    public static void main(String[] args) {
        ProductDAO productDAO = new ProductDAO();

        try {
            ArrayList<Product> products = productDAO.getAllProducts();
            System.out.println("Total products: " + products.size());
            System.out.println("First product: " + products.get(0));

            Product p = productDAO.getProductById(1);
            System.out.println("Found: " + p);

            productDAO.getProductById(9999);
            System.out.println("This line should not print.");

        } catch (ProductNotFoundException e) {
            System.out.println("Not found: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }
}