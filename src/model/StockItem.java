package model;

public class StockItem {
    private int itemId;
    private int batchId;
    private String location;
    private int quantity;
    private String availabilityStatus; // Curing, Available, Reserved, Shipped or Damaged

    public StockItem(int itemId, int batchId, String location,
                     int quantity, String availabilityStatus) {
        this.itemId = itemId;
        this.batchId = batchId;
        this.location = location;
        setQuantity(quantity);
        this.availabilityStatus = availabilityStatus;
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public int getBatchId() {
        return batchId;
    }

    public void setBatchId(int batchId) {
        this.batchId = batchId;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative.");
        }
        this.quantity = quantity;
    }

    public String getAvailabilityStatus() {
        return availabilityStatus;
    }

    public void setAvailabilityStatus(String availabilityStatus) {
        this.availabilityStatus = availabilityStatus;
    }

    // Only stock marked "Available" can be sold
    public boolean isSellable() {
        return availabilityStatus.equals("Available") && quantity > 0;
    }

    @Override
    public String toString() {
        return "StockItem " + itemId + " | Batch " + batchId + " | Location: " + location
                + " | Qty: " + quantity + " | Status: " + availabilityStatus;
    }
}