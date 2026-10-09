package report;

import java.sql.SQLException;
import java.time.LocalDate;

public abstract class Report {
    private String type;
    private LocalDate startDate;
    private LocalDate endDate;

    public Report(String type, LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Both dates are required.");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("The end date cannot be before the start date.");
        }
        this.type = type;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public String getType() {
        return type;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    // The title lines that every report starts with
    protected String header() {
        return "=== " + type + " ===\nPeriod: " + startDate + " to " + endDate + "\n";
    }

    // Each kind of report builds its own text
    public abstract String generate() throws SQLException;
}