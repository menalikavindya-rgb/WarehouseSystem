package model;

public abstract class User {
    private int userId;
    private String name;
    private String email;
    private String passwordHash;
    private int accessLevel;
    private boolean active = true;   // false = deactivated account

    public User(int userId, String name, String email, String passwordHash, int accessLevel) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.accessLevel = accessLevel;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public int getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(int accessLevel) {
        this.accessLevel = accessLevel;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    // Each child class must provide its own role name
    public abstract String getRole();

    public String getDetails() {
        return "ID: " + userId + ", Name: " + name + ", Email: " + email + ", Role: " + getRole()
                + (active ? "" : " (INACTIVE)");
    }
}