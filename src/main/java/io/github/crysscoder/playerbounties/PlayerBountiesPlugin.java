package io.github.crysscoder.playerbounties;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class PlayerBountiesPlugin extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {
    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacyAmpersand();
    private final Map<UUID, Bounty> bounties = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadBounties();
        getServer().getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("bounty")).setExecutor(this);
        Objects.requireNonNull(getCommand("bounty")).setTabCompleter(this);
    }

    @Override
    public void onDisable() {
        saveBounties();
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player target = event.getEntity();
        Player killer = target.getKiller();

        if (killer == null) {
            return;
        }

        Bounty bounty = bounties.remove(target.getUniqueId());

        if (bounty == null) {
            return;
        }

        giveDiamonds(killer, bounty.amount());
        saveBounties();
        send(killer, "paid", Map.of("target", bounty.name(), "amount", String.valueOf(bounty.amount())));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("playerbounties.use")) {
            send(sender, "no-permission");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            list(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("playerbounties.reload")) {
                send(sender, "no-permission");
                return true;
            }
            reloadConfig();
            loadBounties();
            send(sender, "reloaded");
            return true;
        }

        if (args[0].equalsIgnoreCase("set") && args.length >= 3) {
            if (!(sender instanceof Player player)) {
                send(sender, "only-player");
                return true;
            }
            set(player, args[1], args[2]);
            return true;
        }

        send(sender, "usage");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("set", "list", "reload").stream().filter(item -> item.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(name -> name.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))).toList();
        }
        return List.of();
    }

    private void set(Player player, String targetName, String amountValue) {
        Player target = Bukkit.getPlayerExact(targetName);

        if (target == null || target.equals(player)) {
            send(player, "bad-target");
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(amountValue);
        } catch (NumberFormatException exception) {
            send(player, "bad-number");
            return;
        }

        if (amount <= 0 || !takeDiamonds(player, amount)) {
            send(player, "no-diamonds");
            return;
        }

        Bounty current = bounties.get(target.getUniqueId());
        int total = amount + (current == null ? 0 : current.amount());
        bounties.put(target.getUniqueId(), new Bounty(target.getName(), total));
        saveBounties();
        send(player, "added", Map.of("target", target.getName(), "amount", String.valueOf(total)));
    }

    private void list(CommandSender sender) {
        if (bounties.isEmpty()) {
            send(sender, "list-empty");
            return;
        }

        bounties.values().forEach(bounty -> send(sender, "list-line", Map.of("target", bounty.name(), "amount", String.valueOf(bounty.amount()))));
    }

    private boolean takeDiamonds(Player player, int amount) {
        int left = amount;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.DIAMOND) {
                left -= item.getAmount();
            }
        }

        if (left > 0) {
            return false;
        }

        left = amount;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() != Material.DIAMOND || left <= 0) {
                continue;
            }
            int take = Math.min(left, item.getAmount());
            item.setAmount(item.getAmount() - take);
            left -= take;
        }
        return true;
    }

    private void giveDiamonds(Player player, int amount) {
        player.getInventory().addItem(new ItemStack(Material.DIAMOND, amount)).values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
    }

    private void loadBounties() {
        bounties.clear();
        if (getConfig().getConfigurationSection("bounties") == null) {
            return;
        }
        for (String key : getConfig().getConfigurationSection("bounties").getKeys(false)) {
            String path = "bounties." + key;
            bounties.put(UUID.fromString(key), new Bounty(getConfig().getString(path + ".name", key), getConfig().getInt(path + ".amount")));
        }
    }

    private void saveBounties() {
        getConfig().set("bounties", null);
        for (Map.Entry<UUID, Bounty> entry : bounties.entrySet()) {
            String path = "bounties." + entry.getKey();
            getConfig().set(path + ".name", entry.getValue().name());
            getConfig().set(path + ".amount", entry.getValue().amount());
        }
        saveConfig();
    }

    private void send(CommandSender sender, String key) {
        send(sender, key, Map.of());
    }

    private void send(CommandSender sender, String key, Map<String, String> values) {
        String prefix = getConfig().getString("messages.prefix", "&7[&aBounty&7]");
        String result = getConfig().getString("messages." + key, "").replace("%prefix%", prefix);
        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        sender.sendMessage(legacy.deserialize(result));
    }

    private record Bounty(String name, int amount) {
    }
}
