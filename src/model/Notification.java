package model;

import java.time.LocalDateTime;

public class Notification {
    private int notificationId;
    private int userId;
    private String message;
    private LocalDateTime timestamp;
    private boolean read;

    public Notification(int notificationId, int userId, String message,
                        LocalDateTime timestamp, boolean read) {
        this.notificationId = notificationId;
        this.userId = userId;
        setMessage(message);
        this.timestamp = timestamp;
        this.read = read;
    }

    public int getNotificationId() {
        return notificationId;
    }

    public int getUserId() {
        return userId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("A notification message cannot be empty.");
        }
        this.message = message.trim();
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    @Override
    public String toString() {
        String mark;
        if (read) {
            mark = "read";
        } else {
            mark = "UNREAD";
        }
        return notificationId + " [" + mark + "] " + timestamp + " - " + message;
    }
}
