package ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import dao.ReportDAO;
import model.User;
import report.PerformanceReport;
import report.Report;
import report.SalesReport;
import report.StockReport;

// The manager chooses a report type and a date range and reads the result.
public class ReportsPanel extends ScreenPanel {

    private User user;
    private ReportDAO reportDAO = new ReportDAO();

    private JComboBox<String> typeBox = new JComboBox<String>(
            new String[] { "Stock report", "Sales report", "Performance report" });
    private JTextField startField = new JTextField(LocalDate.now().minusDays(30).toString(), 10);
    private JTextField endField = new JTextField(LocalDate.now().toString(), 10);
    private JCheckBox logBox = new JCheckBox("Save a record in the Report table", true);
    private JTextArea output = new JTextArea();

    public ReportsPanel(User user) {
        super("Reports");
        this.user = user;

        JButton generateButton = new JButton("Generate");
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        bar.add(new JLabel("Report:"));
        bar.add(typeBox);
        bar.add(new JLabel("From (yyyy-mm-dd):"));
        bar.add(startField);
        bar.add(new JLabel("To:"));
        bar.add(endField);
        bar.add(logBox);
        bar.add(generateButton);

        output.setEditable(false);
        output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.add(bar, BorderLayout.NORTH);
        body.add(new JScrollPane(output), BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        generateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                generate();
            }
        });
    }

    private LocalDate readDate(JTextField field, String name) {
        try {
            return LocalDate.parse(field.getText().trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(name + " must look like 2026-10-09.");
        }
    }

    private void generate() {
        try {
            LocalDate start = readDate(startField, "The start date");
            LocalDate end = readDate(endField, "The end date");

            // One variable of the parent type can hold any of the three reports (polymorphism).
            Report report;
            int choice = typeBox.getSelectedIndex();
            if (choice == 0) {
                report = new StockReport(start, end);
            } else if (choice == 1) {
                report = new SalesReport(start, end);
            } else {
                report = new PerformanceReport(start, end);
            }

            // generate() runs the version that belongs to the chosen report class
            String text = report.generate();

            if (logBox.isSelected()) {
                int reportId = reportDAO.logReport(report, user.getUserId());
                text = text + "\n(Saved as report " + reportId + ")\n";
            }
            output.setText(text);
            output.setCaretPosition(0);

        } catch (Exception e) {
            handleError(e);
        }
    }

    @Override
    public void refresh() {
        // reports are only made when the button is pressed
    }
}
