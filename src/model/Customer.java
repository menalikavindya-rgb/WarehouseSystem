package model;

public class Customer {
    private int customerId;
    private String name;
    private String contactNumber;
    private String email;
    private String nicNumber;
    private String address;

    public Customer(int customerId, String name, String contactNumber,
                    String email, String nicNumber, String address) {
        this.customerId = customerId;
        setName(name);
        this.contactNumber = contactNumber;
        setEmail(email);
        setNicNumber(nicNumber);
        this.address = address;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name cannot be empty.");
        }
        this.name = name.trim();
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getEmail() {
        return email;
    }

    // An empty email is stored as null, because the database column is UNIQUE
    public void setEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            this.email = null;
        } else {
            this.email = email.trim();
        }
    }

    public String getNicNumber() {
        return nicNumber;
    }

    // Companies have no NIC, so it is optional. If given, it must be a valid Sri Lankan NIC:
    // 9 digits followed by V or X (old format), or 12 digits (new format).
    public void setNicNumber(String nicNumber) {
        if (nicNumber == null || nicNumber.trim().isEmpty()) {
            this.nicNumber = null;
            return;
        }
        String nic = nicNumber.trim().toUpperCase();
        if (!nic.matches("\\d{9}[VX]|\\d{12}")) {
            throw new IllegalArgumentException(
                    "NIC must be 9 digits followed by V or X, or 12 digits.");
        }
        this.nicNumber = nic;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public String toString() {
        return customerId + " - " + name + " | " + contactNumber + " | " + email
                + " | NIC: " + nicNumber + " | " + address;
    }
}