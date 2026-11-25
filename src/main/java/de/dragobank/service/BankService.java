package de.dragobank.service;

import de.dragobank.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.List;
import java.util.UUID;

public final class BankService {

    private final DatabaseManager database;

    public BankService(DatabaseManager database) {
        this.database = database;
    }

    public void createAccount(
            UUID uuid,
            String name,
            long startingBalance
    ) throws SQLException {

        if (startingBalance < 0) {
            throw new IllegalArgumentException(
                    "Starting balance cannot be negative."
            );
        }

        database.transaction(connection -> {

            String sql = """
                    INSERT INTO accounts (uuid, name, balance)
                    VALUES (?, ?, ?)
                    ON CONFLICT(uuid) DO UPDATE
                    SET name = excluded.name
                    """;

            try (PreparedStatement statement =
                         connection.prepareStatement(sql)) {

                statement.setString(1, uuid.toString());
                statement.setString(2, name);
                statement.setLong(3, startingBalance);

                statement.executeUpdate();
            }

            return null;
        });
    }

    public long getBalance(UUID uuid) throws SQLException {

        return database.transaction(connection ->
                readBalance(connection, uuid)
        );
    }

    public UUID findAccount(String name) throws SQLException {

        return database.transaction(connection -> {

            String sql = """
                    SELECT uuid FROM accounts
                    WHERE name = ? COLLATE NOCASE
                    LIMIT 2
                    """;

            try (PreparedStatement statement =
                         connection.prepareStatement(sql)) {

                statement.setString(1, name);

                try (ResultSet result = statement.executeQuery()) {

                    if (!result.next()) {
                        throw new IllegalArgumentException(
                                "Player account not found."
                        );
                    }

                    UUID uuid = UUID.fromString(
                            result.getString("uuid")
                    );

                    if (result.next()) {
                        throw new IllegalArgumentException(
                                "Player name is ambiguous."
                        );
                    }

                    return uuid;
                }
            }
        });
    }

    public long addMoney(UUID uuid, long amount)
            throws SQLException {

        validateAmount(amount);

        return database.transaction(connection -> {

            long balance = readBalance(connection, uuid);

            long newBalance = Math.addExact(balance, amount);

            writeBalance(connection, uuid, newBalance);

            return newBalance;
        });
    }

    public long removeMoney(UUID uuid, long amount)
            throws SQLException {

        validateAmount(amount);

        return database.transaction(connection -> {

            long balance = readBalance(connection, uuid);

            if (balance < amount) {
                throw new IllegalArgumentException(
                        "Insufficient funds."
                );
            }

            long newBalance = balance - amount;

            writeBalance(connection, uuid, newBalance);

            return newBalance;
        });
    }

    public long setBalance(UUID uuid, long amount)
            throws SQLException {

        if (amount < 0) {
            throw new IllegalArgumentException(
                    "Balance cannot be negative."
            );
        }

        return database.transaction(connection -> {

            writeBalance(connection, uuid, amount);

            return amount;
        });
    }

    public void transfer(
            UUID sender,
            UUID receiver,
            long amount
    ) throws SQLException {

        validateAmount(amount);

        if (sender.equals(receiver)) {
            throw new IllegalArgumentException(
                    "You cannot pay yourself."
            );
        }

        database.transaction(connection -> {

            long senderBalance =
                    readBalance(connection, sender);

            long receiverBalance =
                    readBalance(connection, receiver);

            if (senderBalance < amount) {
                throw new IllegalArgumentException(
                        "Insufficient funds."
                );
            }

            long newReceiverBalance =
                    Math.addExact(receiverBalance, amount);

            writeBalance(
                    connection,
                    sender,
                    senderBalance - amount
            );

            writeBalance(
                    connection,
                    receiver,
                    newReceiverBalance
            );

            return null;
        });
    }

    public MoneydropResult moneydrop(
            UUID sender,
            List<UUID> recipients,
            long amount
    ) throws SQLException {

        validateAmount(amount);

        List<UUID> players =
                recipients.stream().distinct().toList();

        if (players.isEmpty()) {
            throw new IllegalArgumentException(
                    "No eligible players online."
            );
        }

        long share = amount / players.size();
        long remainder = amount % players.size();

        if (share == 0) {
            throw new IllegalArgumentException(
                    "Amount is too small for all recipients."
            );
        }

        return database.transaction(connection -> {

            long senderBalance =
                    readBalance(connection, sender);

            if (senderBalance < amount) {
                throw new IllegalArgumentException(
                        "Insufficient funds."
                );
            }

            long distributed = amount - remainder;

            writeBalance(
                    connection,
                    sender,
                    senderBalance - distributed
            );

            for (UUID recipient : players) {

                long balance =
                        readBalance(connection, recipient);

                long newBalance =
                        Math.addExact(balance, share);

                writeBalance(
                        connection,
                        recipient,
                        newBalance
                );
            }

            return new MoneydropResult(
                    share,
                    remainder,
                    players.size()
            );
        });
    }

    private long readBalance(
            Connection connection,
            UUID uuid
    ) throws SQLException {

        String sql = """
                SELECT balance FROM accounts
                WHERE uuid = ?
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, uuid.toString());

            try (ResultSet result = statement.executeQuery()) {

                if (result.next()) {
                    return result.getLong("balance");
                }

                throw new IllegalArgumentException(
                        "Player account not found."
                );
            }
        }
    }

    private void writeBalance(
            Connection connection,
            UUID uuid,
            long balance
    ) throws SQLException {

        String sql = """
                UPDATE accounts
                SET balance = ?
                WHERE uuid = ?
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setLong(1, balance);
            statement.setString(2, uuid.toString());

            int updated = statement.executeUpdate();

            if (updated == 0) {
                throw new IllegalArgumentException(
                        "Player account not found."
                );
            }
        }
    }

    private void validateAmount(long amount) {

        if (amount <= 0) {
            throw new IllegalArgumentException(
                    "Amount must be greater than zero."
            );
        }
    }

    public record MoneydropResult(
            long share,
            long remainder,
            int recipients
    ) {}
}