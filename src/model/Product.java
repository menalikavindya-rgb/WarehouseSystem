package model;

public class Product {
    private int productId;
    private String name;
    private String category;
    private double unitPrice;
    private int defaultCuringDurationDays;

    public Product(int productId, String name, String category,
                   double unitPrice, int defaultCuringDurationDays) {
        this.productId = productId;
        this.name = name;
        this.category = category;
        this.unitPrice = unitPrice;
        this.defaultCuringDurationDays = defaultCuringDurationDays;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        if (unitPrice < 0) {
            throw new IllegalArgumentException("Unit price cannot be negative.");
        }
        this.unitPrice = unitPrice;
    }

    public int getDefaultCuringDurationDays() {
        return defaultCuringDurationDays;
    }

    public void setDefaultCuringDurationDays(int defaultCuringDurationDays) {
        this.defaultCuringDurationDays = defaultCuringDurationDays;
    }

    @Override
    public String toString() {
        return productId + " - " + name + " (" + category + ") Rs. " + unitPrice
                + ", curing: " + defaultCuringDurationDays + " days";
    }
}