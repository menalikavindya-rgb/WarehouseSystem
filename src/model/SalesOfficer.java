package model;

public class SalesOfficer extends User {
    private double salesTarget;

    public SalesOfficer(int userId, String name, String email, String passwordHash,
                        int accessLevel, double salesTarget) {
        super(userId, name, email, passwordHash, accessLevel);
        this.salesTarget = salesTarget;
    }

    public double getSalesTarget() {
        return salesTarget;
    }

    public void setSalesTarget(double salesTarget) {
        this.salesTarget = salesTarget;
    }

    @Override
    public String getRole() {
        return "SalesOfficer";
    }
}
