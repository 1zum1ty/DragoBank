package de.dragobank.service;

import de.dragobank.database.DatabaseManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BankServiceTest {

    @TempDir
    Path tempDirectory;

    private BankService bankService;

    private UUID sender;
    private UUID receiver;
    private UUID thirdPlayer;

    @BeforeEach
    void setUp() throws Exception {

        DatabaseManager database =
                new DatabaseManager(tempDirectory.toFile());

        bankService = new BankService(database);

        sender = UUID.randomUUID();
        receiver = UUID.randomUUID();
        thirdPlayer = UUID.randomUUID();

        bankService.createAccount(sender, "Steve", 1000);
        bankService.createAccount(receiver, "Alex", 1000);
        bankService.createAccount(thirdPlayer, "Player3", 1000);
    }

    @Test
    void startingBalanceIsCorrect() throws Exception {

        assertEquals(1000, bankService.getBalance(sender));
    }

    @Test
    void accountCreationDoesNotResetBalance() throws Exception {

        bankService.addMoney(sender, 500);

        bankService.createAccount(sender, "NewName", 1000);

        assertEquals(1500, bankService.getBalance(sender));
    }

    @Test
    void moneyCanBeAdded() throws Exception {

        bankService.addMoney(sender, 500);

        assertEquals(1500, bankService.getBalance(sender));
    }

    @Test
    void moneyCanBeRemoved() throws Exception {

        bankService.removeMoney(sender, 250);

        assertEquals(750, bankService.getBalance(sender));
    }

    @Test
    void balanceCanBeSet() throws Exception {

        bankService.setBalance(sender, 5000);

        assertEquals(5000, bankService.getBalance(sender));
    }

    @Test
    void negativeAmountsAreRejected() {

        assertThrows(
                IllegalArgumentException.class,
                () -> bankService.addMoney(sender, -500)
        );
    }

    @Test
    void insufficientFundsAreRejected() throws Exception {

        assertThrows(
                IllegalArgumentException.class,
                () -> bankService.removeMoney(sender, 2000)
        );

        assertEquals(1000, bankService.getBalance(sender));
    }

    @Test
    void transferIsSuccessful() throws Exception {

        bankService.transfer(sender, receiver, 250);

        assertEquals(750, bankService.getBalance(sender));
        assertEquals(1250, bankService.getBalance(receiver));
    }

    @Test
    void selfTransferIsRejected() {

        assertThrows(
                IllegalArgumentException.class,
                () -> bankService.transfer(sender, sender, 100)
        );
    }

    @Test
    void moneydropDistributesMoneyEqually() throws Exception {

        BankService.MoneydropResult result =
                bankService.moneydrop(
                        sender,
                        List.of(receiver, thirdPlayer),
                        501
                );

        assertEquals(250, result.share());
        assertEquals(1, result.remainder());
        assertEquals(2, result.recipients());

        assertEquals(500, bankService.getBalance(sender));
        assertEquals(1250, bankService.getBalance(receiver));
        assertEquals(1250, bankService.getBalance(thirdPlayer));
    }

    @Test
    void moneydropRollsBackOnOverflow() throws Exception {

        bankService.setBalance(thirdPlayer, Long.MAX_VALUE);

        assertThrows(
                ArithmeticException.class,
                () -> bankService.moneydrop(
                        sender,
                        List.of(receiver, thirdPlayer),
                        500
                )
        );

        assertEquals(1000, bankService.getBalance(sender));
        assertEquals(1000, bankService.getBalance(receiver));

        assertEquals(
                Long.MAX_VALUE,
                bankService.getBalance(thirdPlayer)
        );
    }

    @Test
    void balancePersistsAcrossDatabaseConnections()
            throws Exception {

        bankService.addMoney(sender, 500);

        DatabaseManager reopenedDatabase =
                new DatabaseManager(tempDirectory.toFile());

        BankService reopenedService =
                new BankService(reopenedDatabase);

        assertEquals(1500, reopenedService.getBalance(sender));
    }
}