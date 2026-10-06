package model;

import java.time.LocalDate;

public class Batch {
    private int batchId;
    private int productId;
    private LocalDate productionDate;
    private LocalDate curingEndDate;
    private int quantity;
    private String curingStatus; // Curing, Ready or Depleted

    public Batch(int batchId, int productId, LocalDate productionDate,
                 LocalDate curingEndDate, int quantity, String curingStatus) {
        this.batchId = batchId;
        this.productId = productId;
        this.productionDate = productionDate;
        this.curingEndDate = curingEndDate;
        setQuantity(quantity);
        this.curingStatus = curingStatus;
    }

    public int getBatchId() {
        return batchId;
    }

    public void setBatchId(int batchId) {
        this.batchId = batchId;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public LocalDate getProductionDate() {
        return productionDate;
    }

    public void setProductionDate(LocalDate productionDate) {
        this.productionDate = productionDate;
    }

    public LocalDate getCuringEndDate() {
        return curingEndDate;
    }

    public void setCuringEndDate(LocalDate curingEndDate) {
        this.curingEndDate = curingEndDate;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Batch quantity must be greater than 0.");
        }
        this.quantity = quantity;
    }

    public String getCuringStatus() {
        return curingStatus;
    }

    public void setCuringStatus(String curingStatus) {
        this.curingStatus = curingStatus;
    }

    @Override
    public String toString() {
        return "Batch " + batchId + " | Product " + productId + " | Produced: " + productionDate
                + " | Curing ends: " + curingEndDate + " | Qty: " + quantity
                + " | Status: " + curingStatus;
    }
}