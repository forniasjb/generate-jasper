package com.sandbox.ph.generatejasper.ticket.utils;


import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebListener
public class DatabaseConnection implements ServletContextListener {
    private static final Logger LOGGER = Logger.getLogger(DatabaseConnection.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent sce) {

        try {
            Properties properties = new Properties();
            try (InputStream config = getClass()
                    .getClassLoader()
                    .getResourceAsStream("database.properties")) {

                if (config == null) {
                    throw new IllegalStateException("Required configuration file 'database.properties' was not found in the classpath.");
                }
                properties.load(config);
            }

            String jdbcUrl = properties.getProperty("db.url");
            String user = properties.getProperty("db.user");
            String password = properties.getProperty("db.password");

            LOGGER.info("Loading IBM DB2 iSeries JDBC Driver...");
            Class.forName("com.ibm.as400.access.AS400JDBCDriver");
            LOGGER.info("Connecting to DB2 iSeries database...");

            try (Connection conn = DriverManager.getConnection(jdbcUrl, user, password)) {
                LOGGER.info("DB2 iSeries database connected successfully.");
                try (InputStream is = getClass()
                        .getClassLoader()
                        .getResourceAsStream("data.sql")) {

                    if (is != null) {

                        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                            StringBuilder sqlBatch = new StringBuilder();
                            String line;

                            while ((line = br.readLine()) != null) {
                                String trimmedLine = line.trim();
                                if (trimmedLine.startsWith("--") || trimmedLine.isEmpty()) {
                                    continue;
                                }
                                sqlBatch.append(line).append('\n');

                                if (trimmedLine.endsWith(";")) {
                                    try (Statement stmt = conn.createStatement()) {
                                        String sqlToExecute = sqlBatch.toString().trim();
                                        // Remove trailing semicolon if present
                                        if (sqlToExecute.endsWith(";")) {
                                            sqlToExecute = sqlToExecute.substring(0, sqlToExecute.length() - 1).trim();
                                        }
                                        if (!sqlToExecute.isEmpty()) {
                                            stmt.execute(sqlToExecute);
                                        }
                                    }
                                    sqlBatch.setLength(0);
                                }
                            }
                            LOGGER.info("data.sql executed successfully.");
                        }
                    } else {
                        LOGGER.warning("data.sql not found. Skipping database initialization.");
                    }
                }
            }
        } catch (IllegalStateException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "DB2 iSeries database initialization failed.", e);
        }
    }
}
