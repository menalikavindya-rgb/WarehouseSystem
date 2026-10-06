import model.User;
import model.WarehouseOperator;
import model.SalesOfficer;
import model.WarehouseManager;

public class TestUsers {
    public static void main(String[] args) {
        User u1 = new WarehouseOperator(1, "Nimal", "nimal@dilani.lk", "hash1", 1, "Zone A");
        User u2 = new SalesOfficer(2, "Kamala", "kamala@dilani.lk", "hash2", 2, 500000.00);
        User u3 = new WarehouseManager(3, "Sunil", "sunil@dilani.lk", "hash3", 3);

        System.out.println(u1.getDetails());
        System.out.println(u2.getDetails());
        System.out.println(u3.getDetails());
    }
}