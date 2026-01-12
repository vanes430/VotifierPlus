package com.vexsoftware.votifier.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Consumer;

public class Debug {

    private static boolean enabled = false;
    private static Consumer<String> infoConsumer;
    private static File dataFolder;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void initialize(Consumer<String> info, File dataFolder, boolean enabled) {
        Debug.infoConsumer = info;
        Debug.dataFolder = dataFolder;
        Debug.enabled = enabled;
    }

    public static void setEnabled(boolean enabled) {
        Debug.enabled = enabled;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Log basic information (always shown as [INFO])
     */
    public static void log(String message) {
        if (infoConsumer != null) {
            infoConsumer.accept(message);
        }
    }

    /**
     * Log detailed debug information (only shown if debug is true)
     */
    public static void debug(String message) {
        if (enabled && infoConsumer != null) {
            infoConsumer.accept("[DEBUG] " + message);
        }
    }

    /**
     * Log error message and handle stacktrace
     */
    public static void error(String message, Throwable throwable) {
        String briefEntry = "[ERROR] " + message + (throwable != null ? ": " + throwable.getMessage() : "");
        if (infoConsumer != null) {
            infoConsumer.accept(briefEntry);
        }

        // If debug is enabled, print full stacktrace to console
        if (enabled && throwable != null) {
            throwable.printStackTrace();
        }

        // Always save full stacktrace to error.log
        if (throwable != null) {
            saveToFile(message, throwable);
        }
    }

    private static void saveToFile(String message, Throwable throwable) {
        if (dataFolder == null) return;
        
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        File errorLog = new File(dataFolder, "error.log");
        try (FileWriter fw = new FileWriter(errorLog, true);
             PrintWriter pw = new PrintWriter(fw)) {
            
            pw.println("========================================");
            pw.println("Date: " + dateFormat.format(new Date()));
            pw.println("Message: " + message);
            pw.println("Stacktrace:");
            throwable.printStackTrace(pw);
            pw.println("========================================");
            pw.println();
            
        } catch (IOException e) {
            if (infoConsumer != null) {
                infoConsumer.accept("[ERROR] Could not write to error.log: " + e.getMessage());
            }
        }
    }

    public static void logFailedVote(String address, String error, String payload) {
        if (dataFolder == null) return;

        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        File failedLog = new File(dataFolder, "failed-votes.log");
        try (FileWriter fw = new FileWriter(failedLog, true);
             PrintWriter pw = new PrintWriter(fw)) {

            pw.println("========================================");
            pw.println("Date: " + dateFormat.format(new Date()));
            pw.println("Source: " + address);
            pw.println("Error: " + error);
            if (payload != null && !payload.isEmpty()) {
                pw.println("Payload/Data:");
                pw.println(payload);
            }
            pw.println("========================================");
            pw.println();

        } catch (IOException e) {
            if (infoConsumer != null) {
                infoConsumer.accept("[ERROR] Could not write to failed-votes.log: " + e.getMessage());
            }
        }
    }
}
