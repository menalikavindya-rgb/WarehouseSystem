import java.sql.SQLException;
import java.time.LocalDate;
import dao.ReportDAO;
import exception.AccessDeniedException;
import report.PerformanceReport;
import report.Report;
import report.SalesReport;
import report.StockReport;

public class TestReports {

    public static void main(String[] args) {
        ReportDAO reportDAO = new ReportDAO();
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.now();

        try {
            // One array that holds three different kinds of report (polymorphism)
            Report[] reports = new Report[3];
            reports[0] = new StockReport(from, to);
            reports[1] = new SalesReport(from, to);
            reports[2] = new PerformanceReport(from, to);

            // Test 1: generate each report and log it as manager (user 1)
            for (int i = 0; i < reports.length; i++) {
                System.out.println(reports[i].generate());
                int id = reportDAO.logReport(reports[i], 1);
                System.out.println("(logged as report " + id + ")\n");
            }

            // Test 2: an operator (user 4) may not log reports
            try {
                reportDAO.logReport(reports[0], 4);
                System.out.println("Test 2: this line should not print.");
            } catch (AccessDeniedException e) {
                System.out.println("Test 2 refused as expected: " + e.getMessage());
            }

            // Test 3: an end date before the start date
            try {
                new StockReport(to, from);
                System.out.println("Test 3: this line should not print.");
            } catch (IllegalArgumentException e) {
                System.out.println("Test 3 refused as expected: " + e.getMessage());
            }

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Unexpected problem: " + e.getMessage());
        }
    }
}