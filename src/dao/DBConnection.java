package dao;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Properties;

// Opens a connection to MySQL.
// The address, user name and password are NOT written in the code. They are read once from a small
// text file called db.properties, so the same program works on any computer: only that file changes.
public class DBConnection {

    private static final String FILE_NAME = "db.properties";

    private static Properties settings = null;   // filled the first time a connection is asked for

    public static Connection getConnection() throws SQLException {
        Properties p = loadSettings();
        return DriverManager.getConnection(p.getProperty("url"), p.getProperty("user"), p.getProperty("password"));
    }

    // Reads db.properties (only once) and checks that it has the three lines we need.
    private static synchronized Properties loadSettings() throws SQLException {
        if (settings != null) {
            return settings;
        }

        File file = findSettingsFile();
        if (file == null) {
            throw new SQLException("The file " + FILE_NAME + " was not found. Create it in the same folder as the "
                    + "program (or the project folder) with the lines url, user and password.");
        }

        Properties p = new Properties();
        InputStream in = null;
        try {
            in = new FileInputStream(file);
            p.load(in);
        } catch (IOException e) {
            throw new SQLException("Could not read " + file.getPath() + ": " + e.getMessage());
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                System.out.println("Error closing " + FILE_NAME + ": " + e.getMessage());
            }
        }

        String[] needed = { "url", "user", "password" };
        for (int i = 0; i < needed.length; i++) {
            String value = p.getProperty(needed[i]);
            if (value == null) {
                throw new SQLException(FILE_NAME + " (" + file.getPath() + ") has no line for \"" + needed[i] + "\".");
            }
            p.setProperty(needed[i], value.trim());   // ignore spaces at the ends
        }

        settings = p;
        return settings;
    }

    // Looks for db.properties in these places, in this order:
    //   1. the folder that holds the program (the .jar, or the compiled classes)
    //   2. the folder above that (the folder with WarehouseSystem.exe, or the project folder)
    //   3. the folder the program was started from
    private static File findSettingsFile() {
        ArrayList<File> folders = new ArrayList<File>();
        try {
            File here = new File(DBConnection.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            File programFolder = here.isFile() ? here.getParentFile() : here;
            folders.add(programFolder);
            if (programFolder != null && programFolder.getParentFile() != null) {
                folders.add(programFolder.getParentFile());
            }
        } catch (URISyntaxException e) {
            System.out.println("Could not find the program folder: " + e.getMessage());
        } catch (SecurityException e) {
            System.out.println("Not allowed to look at the program folder: " + e.getMessage());
        }
        folders.add(new File(System.getProperty("user.dir")));

        for (File folder : folders) {
            if (folder == null) {
                continue;
            }
            File candidate = new File(folder, FILE_NAME);
            if (candidate.isFile()) {
                return candidate;
            }
        }
        return null;
    }
}
