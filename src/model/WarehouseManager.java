package model;

public class WarehouseManager extends User {

    public WarehouseManager(int userId, String name, String email, String passwordHash, int accessLevel) {
        super(userId, name, email, passwordHash, accessLevel);
    }

    @Override
    public String getRole() {
        return "WarehouseManager";
    }
}