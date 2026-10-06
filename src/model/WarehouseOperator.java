package model;

public class WarehouseOperator extends User {
    private String assignedYardZone;

    public WarehouseOperator(int userId, String name, String email, String passwordHash,
                             int accessLevel, String assignedYardZone) {
        super(userId, name, email, passwordHash, accessLevel);
        this.assignedYardZone = assignedYardZone;
    }

    public String getAssignedYardZone() {
        return assignedYardZone;
    }

    public void setAssignedYardZone(String assignedYardZone) {
        this.assignedYardZone = assignedYardZone;
    }

    @Override
    public String getRole() {
        return "WarehouseOperator";
    }
}