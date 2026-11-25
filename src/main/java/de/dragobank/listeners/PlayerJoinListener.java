package de.dragobank.listeners;

import de.dragobank.DragoBank;
import de.dragobank.service.BankService;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.sql.SQLException;
import java.util.logging.Level;

public final class PlayerJoinListener implements Listener {

    private final DragoBank plugin;
    private final BankService bankService;

    public PlayerJoinListener(
            DragoBank plugin,
            BankService bankService
    ) {
        this.plugin = plugin;
        this.bankService = bankService;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {

        initializeAccount(event.getPlayer());
    }

    public void initializeAccount(Player player) {

        long startingBalance = plugin.getConfig()
                .getLong("economy.starting-balance");

        try {

            bankService.createAccount(
                    player.getUniqueId(),
                    player.getName(),
                    startingBalance
            );

        } catch (SQLException | IllegalArgumentException exception) {

            plugin.getLogger().log(
                    Level.SEVERE,
                    "Could not initialize account for "
                            + player.getName(),
                    exception
            );
        }
    }
}