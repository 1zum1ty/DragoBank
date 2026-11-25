package de.dragobank.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseManager {

    private final String databaseUrl;

    public DatabaseManager(File dataFolder)
            throws SQLException, ClassNotFoundException {

        Class.forName("org.sqlite.JDBC");

        File databaseFile = new File(dataFolder, "dragobank.db");

        this.databaseUrl =
                "jdbc:sqlite:" + databaseFile.getAbsolutePath();

        initializeDatabase();
    }

    private void initializeDatabase() throws SQLException {

        transaction(connection -> {

            String sql = """
                    CREATE TABLE IF NOT EXISTS accounts (
                        uuid TEXT PRIMARY KEY,
                        name TEXT NOT NULL,
                        balance INTEGER NOT NULL DEFAULT 0
                            CHECK(balance >= 0)
                    )
                    """;

            try (Statement statement = connection.createStatement()) {
                statement.execute(sql);
            }

            return null;
        });
    }

    public synchronized <T> T transaction(SqlWork<T> work)
            throws SQLException {

        try (Connection connection =
                     DriverManager.getConnection(databaseUrl)) {

            connection.setAutoCommit(false);

            try {

                T result = work.execute(connection);

                connection.commit();

                return result;

            } catch (SQLException | RuntimeException exception) {

                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    exception.addSuppressed(rollbackException);
                }

                throw exception;
            }
        }
    }

    @FunctionalInterface
    public interface SqlWork<T> {

        T execute(Connection connection) throws SQLException;
    }
}