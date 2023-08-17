package ru.netology;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;

public class Logger {
    private static final String LOG_FILE = "log.txt";

    public static synchronized void logRequest(String requestLine, int statusCode) {
        String logEntry = String.format("[%s] [%s] %s %d%n", LocalDateTime.now(), requestLine, getStatusText(statusCode));
        try (FileWriter writer = new FileWriter(LOG_FILE, true)) {
            writer.write(logEntry);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getStatusText(int statusCode) {
        switch (statusCode) {
            case 200:
                return "OK";
            case 404:
                return "Not Found";
            default:
                return "Unknown";
        }
    }
}
