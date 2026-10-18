package ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.time.format.DateTimeParseException;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
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
    private JCheckBox logBox = new JCheckBox("Keep a copy in the system", true);
    private JTextArea output = new JTextArea();
    private JTable savedTable = createTable();          // the reports saved earlier
    private boolean loadingList = false;                // true while the list is being refilled
    private String shownTitle = "report";               // used for the suggested file name

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

        // right side: the report text with two buttons under it
        JButton saveFileButton = new JButton("Save as file");
        JButton printButton = new JButton("Print or save as PDF");
        JPanel fileRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        fileRow.add(saveFileButton);
        fileRow.add(printButton);
        JPanel textSide = new JPanel(new BorderLayout(0, 6));
        textSide.add(new JScrollPane(output), BorderLayout.CENTER);
        textSide.add(fileRow, BorderLayout.SOUTH);

        // left side: reports saved earlier
        JPanel listSide = new JPanel(new BorderLayout(0, 6));
        listSide.add(new Theme.MutedLabel("Saved reports (click one to open it)"), BorderLayout.NORTH);
        listSide.add(new JScrollPane(savedTable), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listSide, textSide);
        split.setResizeWeight(0.38);
        split.setBorder(null);

        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.add(bar, BorderLayout.NORTH);
        body.add(split, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        savedTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting() && !loadingList) {
                    openSelectedReport();
                }
            }
        });
        saveFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveAsFile();
            }
        });
        printButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                printReport();
            }
        });

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

            String savedNote = "";
            if (logBox.isSelected()) {
                int reportId = reportDAO.logReport(report, user.getUserId(), text);
                savedNote = "\n(Saved as report " + reportId + ")\n";
                loadSavedList();
            }
            shownTitle = report.getType().replace(' ', '_') + "_" + end;
            output.setText(text + savedNote);
            output.setCaretPosition(0);

        } catch (Exception e) {
            handleError(e);
        }
    }

    // Reloads the list of saved reports.
    private void loadSavedList() {
        try {
            ArrayList<Object[]> rows = reportDAO.getSavedReports(user.getUserId());
            loadingList = true;
            fillTable(savedTable, new String[] { "Report", "Type", "From", "To", "Saved at", "By" }, rows);
        } catch (Exception e) {
            handleError(e);
        } finally {
            loadingList = false;
        }
    }

    // Shows the text of the report that was clicked in the list.
    private void openSelectedReport() {
        int viewRow = savedTable.getSelectedRow();
        if (viewRow < 0) {
            return;
        }
        try {
            int modelRow = savedTable.convertRowIndexToModel(viewRow);
            int reportId = ((Number) savedTable.getModel().getValueAt(modelRow, 0)).intValue();
            String type = String.valueOf(savedTable.getModel().getValueAt(modelRow, 1));
            String to = String.valueOf(savedTable.getModel().getValueAt(modelRow, 3));

            String content = reportDAO.getReportContent(reportId, user.getUserId());
            if (content == null) {
                content = "Report " + reportId + " was saved before report texts were kept,\n"
                        + "so only its type and dates are available. Generate it again to get the text.";
            }
            shownTitle = type.replace(' ', '_') + "_" + to;
            output.setText(content);
            output.setCaretPosition(0);
        } catch (Exception e) {
            handleError(e);
        }
    }

    // Writes the report on screen to a text file the user chooses.
    private void saveAsFile() {
        if (output.getText().trim().isEmpty()) {
            showWarning("There is no report on screen yet. Generate or open one first.");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(shownTitle + ".txt"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            File file = chooser.getSelectedFile();
            Files.write(file.toPath(), output.getText().getBytes(StandardCharsets.UTF_8));
            showInfo("Report saved to " + file.getAbsolutePath());
        } catch (Exception e) {
            handleError(e);
        }
    }

    // Opens the print window. Choose "Microsoft Print to PDF" there to get a PDF file.
    private void printReport() {
        if (output.getText().trim().isEmpty()) {
            showWarning("There is no report on screen yet. Generate or open one first.");
            return;
        }
        try {
            output.print();
        } catch (Exception e) {
            handleError(e);
        }
    }

    @Override
    public void refresh() {
        loadSavedList();
    }
}
