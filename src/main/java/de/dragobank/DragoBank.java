package de.dragobank;

import de.dragobank.commands.BankCommand;
import de.dragobank.database.DatabaseManager;
import de.dragobank.listeners.PlayerJoinListener;
import de.dragobank.service.BankService;

import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;

public final class DragoBank extends JavaPlugin {

    private BankService bankService;

    @Override
    public void onEnable() {

        saveDefaultConfig();

        try {

            DatabaseManager database =
                    new DatabaseManager(getDataFolder());

            bankService = new BankService(database);

        } catch (SQLException | ClassNotFoundException exception) {

            getLogger().log(
                    Level.SEVERE,
                    "Could not initialize DragoBank database.",
                    exception
            );

            getServer().getPluginManager().disablePlugin(this);

            return;
        }

        PlayerJoinListener joinListener =
                new PlayerJoinListener(this, bankService);

        getServer().getPluginManager().registerEvents(
                joinListener,
                this
        );

        for (Player player : getServer().getOnlinePlayers()) {

            joinListener.initializeAccount(player);
        }

     BankCommand bankCommand =
        new BankCommand(this, bankService);

        for (String name :
                List.of("bank", "balance", "pay", "moneydrop")) {

            PluginCommand command = Objects.requireNonNull(
                    getCommand(name),
                    "Missing command: " + name
            );

            command.setExecutor(bankCommand);
            command.setTabCompleter(bankCommand);
        }

        getLogger().info("DragoBank has been enabled.");
    }

    @Override
    public void onDisable() {

        getLogger().info("DragoBank has been disabled.");
    }

    public BankService getBankService() {

        return bankService;
    }
}