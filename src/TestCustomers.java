import java.sql.SQLException;
import java.util.ArrayList;
import dao.CustomerDAO;
import exception.CustomerNotFoundException;
import exception.DuplicateCustomerException;
import model.Customer;

public class TestCustomers {

    public static void main(String[] args) {
        CustomerDAO customerDAO = new CustomerDAO();

        // Test 1: add a new customer
        try {
            Customer c = new Customer(0, "Test Customer", "+94771234567",
                    "test.customer@mail.lk", "200012345678", "No. 1, Temple Road, Kandy");
            int id = customerDAO.addCustomer(c);
            System.out.println("Test 1 OK: customer added with ID " + id);
        } catch (DuplicateCustomerException e) {
            System.out.println("Test 1: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("Test 1 database error: " + e.getMessage());
        }

        // Test 2: the same customer again, which must be rejected as a duplicate
        try {
            Customer c = new Customer(0, "Test Customer", "+94771234567",
                    "test.customer@mail.lk", "200012345678", "No. 1, Temple Road, Kandy");
            customerDAO.addCustomer(c);
            System.out.println("Test 2: this line should not print.");
        } catch (DuplicateCustomerException e) {
            System.out.println("Test 2 duplicate caught as expected: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("Test 2 database error: " + e.getMessage());
        }

        // Test 3: an invalid NIC, which the Customer class must refuse
        try {
            Customer bad = new Customer(0, "Bad Nic", "+94770000000",
                    "bad@mail.lk", "12345", "Colombo");
            System.out.println("Test 3: this line should not print.");
        } catch (IllegalArgumentException e) {
            System.out.println("Test 3 caught as expected: " + e.getMessage());
        }

        // Test 4: search by name
        try {
            ArrayList<Customer> found = customerDAO.searchByName("Amal");
            System.out.println("Test 4: found " + found.size() + " customer(s) matching 'Amal'");
            if (found.size() > 0) {
                System.out.println("   first: " + found.get(0));
            }
        } catch (SQLException e) {
            System.out.println("Test 4 database error: " + e.getMessage());
        }

        // Test 5: a customer ID that does not exist
        try {
            customerDAO.getCustomerById(99999);
            System.out.println("Test 5: this line should not print.");
        } catch (CustomerNotFoundException e) {
            System.out.println("Test 5 caught as expected: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("Test 5 database error: " + e.getMessage());
        }
    }
}