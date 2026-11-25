package de.dragobank.commands;

import de.dragobank.DragoBank;
import de.dragobank.service.BankService;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.sql.SQLException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;

public final class BankCommand
        implements CommandExecutor, TabCompleter {

    private final DragoBank plugin;
    private final BankService bankService;

    public BankCommand(
            DragoBank plugin,
            BankService bankService
    ) {
        this.plugin = plugin;
        this.bankService = bankService;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        String action = command.getName()
                .toLowerCase(Locale.ROOT);

        String[] arguments = args;

        if (action.equals("bank")) {

            if (args.length == 0) {

                action = "balance";

            } else {

                action = args[0].toLowerCase(Locale.ROOT);

                arguments = Arrays.copyOfRange(
                        args, 1, args.length
                );
            }
        }

        try {

            switch (action) {

                case "balance" ->
                        handleBalance(sender, arguments);

                case "pay" ->
                        handlePay(sender, arguments);

                case "moneydrop" ->
                        handleMoneydrop(sender, arguments);

                case "add", "remove", "set" ->
                        handleAdmin(sender, action, arguments);

                case "reload" ->
                        handleReload(sender, arguments);

                case "help" ->
                        showHelp(sender);

                default ->
                        send(sender,
                                "&cUnknown command. Use /bank help.");
            }

        } catch (IllegalArgumentException exception) {

            send(sender, "&c" + exception.getMessage());

        } catch (ArithmeticException exception) {

            send(sender, "&cThe resulting balance is too large.");

        } catch (SQLException exception) {

            send(sender, "&cA database error occurred.");

            plugin.getLogger().log(
                    Level.SEVERE,
                    "DragoBank command failed.",
                    exception
            );
        }

        return true;
    }

    // Balance command

    private void handleBalance(
            CommandSender sender,
            String[] args
    ) throws SQLException {

        if (args.length == 0) {

            requirePermission(sender, "dragobank.balance");

            Player player = requirePlayer(sender);

            long balance = bankService.getBalance(
                    player.getUniqueId()
            );

            String currency = plugin.getConfig().getString(
                    "economy.currency-name", "DragoCoin"
            );

            send(sender,
                    "&7Your " + currency + " balance: &a"
                            + formatMoney(balance));

            return;
        }

        requirePermission(sender, "dragobank.admin.balance");

        requireArguments(
                args, 1, "/bank balance <player>"
        );

        UUID target = resolveAccount(args[0]);

        long balance = bankService.getBalance(target);

        send(sender,
                "&e" + args[0] + "&7's balance: &a"
                        + formatMoney(balance));
    }

    // Pay command

    private void handlePay(
            CommandSender sender,
            String[] args
    ) throws SQLException {

        requirePermission(sender, "dragobank.pay");

        Player player = requirePlayer(sender);

        requireArguments(
                args, 2, "/pay <player> <amount>"
        );

        UUID target = resolveAccount(args[0]);

        long amount = parseAmount(args[1], false);

        bankService.transfer(
                player.getUniqueId(),
                target,
                amount
        );

        send(player,
                "&aSuccessfully transferred &e"
                        + formatMoney(amount)
                        + " &ato " + args[0] + ".");

        Player receiver = plugin.getServer()
                .getPlayer(target);

        if (receiver != null) {

            send(receiver,
                    "&aYou received &e"
                            + formatMoney(amount)
                            + " &afrom "
                            + player.getName() + ".");
        }
    }

    // Moneydrop command

    private void handleMoneydrop(
            CommandSender sender,
            String[] args
    ) throws SQLException {

        requirePermission(sender, "dragobank.moneydrop");

        Player player = requirePlayer(sender);

        requireArguments(args, 1, "/moneydrop <amount>");

        if (!plugin.getConfig()
                .getBoolean("moneydrop.enabled", true)) {

            throw new IllegalArgumentException(
                    "Moneydrops are disabled."
            );
        }

        long amount = parseAmount(args[0], false);

        boolean includeSender = plugin.getConfig()
                .getBoolean("moneydrop.include-sender", false);

        List<Player> recipients = new ArrayList<>();

        for (Player online :
                plugin.getServer().getOnlinePlayers()) {

            if (includeSender || !online.getUniqueId()
                    .equals(player.getUniqueId())) {

                recipients.add(online);
            }
        }

        List<UUID> recipientIds = recipients.stream()
                .map(Player::getUniqueId)
                .toList();

        BankService.MoneydropResult result =
                bankService.moneydrop(
                        player.getUniqueId(),
                        recipientIds,
                        amount
                );

        long distributed = amount - result.remainder();

        for (Player online :
                plugin.getServer().getOnlinePlayers()) {

            send(online,
                    "&6&lMONEYDROP! &e"
                            + player.getName()
                            + " &7is distributing &a"
                            + formatMoney(distributed)
                            + " &7among "
                            + result.recipients()
                            + " players!");
        }

        for (Player recipient : recipients) {

            send(recipient,
                    "&aYou received &e"
                            + formatMoney(result.share())
                            + " &afrom the moneydrop!");
        }

        send(player,
                "&7Total distributed: &a"
                        + formatMoney(distributed));

        if (result.remainder() > 0) {

            send(player,
                    "&7Remaining money returned: &e"
                            + formatMoney(result.remainder()));
        }
    }

    // Administrator commands

    private void handleAdmin(
            CommandSender sender,
            String action,
            String[] args
    ) throws SQLException {

        requirePermission(
                sender,
                "dragobank.admin." + action
        );

        requireArguments(
                args,
                2,
                "/bank " + action + " <player> <amount>"
        );

        UUID target = resolveAccount(args[0]);

        long amount = parseAmount(
                args[1],
                action.equals("set")
        );

        long newBalance = switch (action) {

            case "add" ->
                    bankService.addMoney(target, amount);

            case "remove" ->
                    bankService.removeMoney(target, amount);

            case "set" ->
                    bankService.setBalance(target, amount);

            default ->
                    throw new IllegalArgumentException(
                            "Unknown administrator action."
                    );
        };

        send(sender,
                "&aAccount successfully updated.");

        send(sender,
                "&7New balance of &e" + args[0]
                        + "&7: &a" + formatMoney(newBalance));

        Player targetPlayer = plugin.getServer()
                .getPlayer(target);

        if (targetPlayer != null
                && !targetPlayer.equals(sender)) {

            send(targetPlayer,
                    "&7Your balance has been updated: &a"
                            + formatMoney(newBalance));
        }
    }

    // Reload command

    private void handleReload(
            CommandSender sender,
            String[] args
    ) {

        requirePermission(sender, "dragobank.admin.reload");

        requireArguments(args, 0, "/bank reload");

        plugin.reloadConfig();

        send(sender, "&aConfiguration reloaded.");
    }

    // Help command

    private void showHelp(CommandSender sender) {

        send(sender, "&6&lDragoBank &7- Commands");

        if (sender.hasPermission("dragobank.balance")) {

            send(sender,
                    "&e/balance &7- View your balance");
        }

        if (sender.hasPermission("dragobank.pay")) {

            send(sender,
                    "&e/pay <player> <amount> &7- Transfer money");
        }

        if (sender.hasPermission("dragobank.moneydrop")) {

            send(sender,
                    "&e/moneydrop <amount> &7- Distribute money");
        }

        send(sender,
                "&7Player commands also work with /bank.");

        for (String action :
                List.of("balance", "add", "remove", "set")) {

            if (sender.hasPermission(
                    "dragobank.admin." + action)) {

                String arguments = action.equals("balance")
                        ? " <player>"
                        : " <player> <amount>";

                send(sender,
                        "&e/bank " + action + arguments);
            }
        }

        if (sender.hasPermission("dragobank.admin.reload")) {

            send(sender, "&e/bank reload");
        }
    }

    // Tab completion

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args
    ) {

        if (args.length == 0) {
            return List.of();
        }

        String action = command.getName()
                .toLowerCase(Locale.ROOT);

        if (action.equals("bank")) {

            if (args.length == 1) {

                List<String> suggestions = new ArrayList<>();

                suggestions.add("help");

                if (sender.hasPermission("dragobank.balance")
                        || sender.hasPermission(
                                "dragobank.admin.balance")) {

                    suggestions.add("balance");
                }

                if (sender.hasPermission("dragobank.pay")) {

                    suggestions.add("pay");
                }

                if (sender.hasPermission("dragobank.moneydrop")) {

                    suggestions.add("moneydrop");
                }

                for (String admin :
                        List.of("add", "remove", "set", "reload")) {

                    if (sender.hasPermission(
                            "dragobank.admin." + admin)) {

                        suggestions.add(admin);
                    }
                }

                return filter(suggestions, args[0]);
            }

            action = args[0].toLowerCase(Locale.ROOT);

            args = Arrays.copyOfRange(args, 1, args.length);
        }

        String permission = switch (action) {

            case "pay" -> "dragobank.pay";

            case "balance", "add", "remove", "set" ->
                    "dragobank.admin." + action;

            default -> null;
        };

        if (permission != null
                && sender.hasPermission(permission)
                && args.length == 1) {

            List<String> names = new ArrayList<>();

            for (Player player :
                    plugin.getServer().getOnlinePlayers()) {

                if (action.equals("pay")
                        && player.equals(sender)) {

                    continue;
                }

                names.add(player.getName());
            }

            return filter(names, args[0]);
        }

        return List.of();
    }

    // Utility methods

    private List<String> filter(
            List<String> values,
            String input
    ) {

        String lowerInput = input.toLowerCase(Locale.ROOT);

        return values.stream()
                .filter(value -> value
                        .toLowerCase(Locale.ROOT)
                        .startsWith(lowerInput))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private UUID resolveAccount(String name)
            throws SQLException {

        Player online = plugin.getServer()
                .getPlayerExact(name);

        if (online != null) {

            return online.getUniqueId();
        }

        return bankService.findAccount(name);
    }

    private Player requirePlayer(CommandSender sender) {

        if (sender instanceof Player player) {

            return player;
        }

        throw new IllegalArgumentException(
                "Only players can use this command."
        );
    }

    private void requirePermission(
            CommandSender sender,
            String permission
    ) {

        if (!sender.hasPermission(permission)) {

            throw new IllegalArgumentException(
                    "You do not have permission."
            );
        }
    }

    private void requireArguments(
            String[] args,
            int expected,
            String usage
    ) {

        if (args.length != expected) {

            throw new IllegalArgumentException(
                    "Usage: " + usage
            );
        }
    }

    private long parseAmount(
            String input,
            boolean allowZero
    ) {

        long amount;

        try {

            amount = Long.parseLong(input);

        } catch (NumberFormatException exception) {

            throw new IllegalArgumentException(
                    "Please enter a valid whole number."
            );
        }

        if (amount < 0 || (!allowZero && amount == 0)) {

            throw new IllegalArgumentException(
                    allowZero
                            ? "Amount cannot be negative."
                            : "Amount must be greater than zero."
            );
        }

        return amount;
    }

    private String formatMoney(long amount) {

        String symbol = plugin.getConfig().getString(
                "economy.currency-symbol", "$"
        );

        return String.format(
                Locale.US, "%,d %s", amount, symbol
        );
    }

    private void send(
            CommandSender sender,
            String message
    ) {

        String prefix = plugin.getConfig().getString(
                "messages.prefix", "&6[DragoBank] &r"
        );

        sender.sendMessage(
                LegacyComponentSerializer.legacyAmpersand()
                        .deserialize(prefix + message)
        );
    }
}