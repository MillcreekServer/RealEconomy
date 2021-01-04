package io.github.wysohn.realeconomy.main;

import io.github.wysohn.rapidframework3.interfaces.io.IPluginResourceProvider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.DecimalFormat;

public class Metrics {
    public static final DecimalFormat df = new DecimalFormat("#,##0.00");

    public static final double SECOND = 1.0;
    public static final double MINUTE = 60 * SECOND;
    public static final double HOUR = 60 * MINUTE;
    public static final double DAY = 24 * HOUR;

    public static void createTable(IPluginResourceProvider resourceProvider,
                                   Connection connection,
                                   String filename) throws IOException, SQLException {
        try (PreparedStatement pstmt = connection.prepareStatement(resourceToString(resourceProvider, filename))) {
            pstmt.executeUpdate();
        }
    }

    public static String resourceToString(IPluginResourceProvider resourceProvider,
                                          String filename) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (InputStreamReader isr = new InputStreamReader(resourceProvider.getResource(filename), StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(isr)) {
            String buffer = null;
            while ((buffer = br.readLine()) != null) {
                builder.append(buffer);
                builder.append('\n');
            }
        }
        return builder.toString();
    }
}
