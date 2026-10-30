package de.example.yourplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class JoinListener implements Listener {

    private final YourPlugin plugin;

    public JoinListener(YourPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        String message = buildJoinMessage(event.getPlayer());
        if (message != null) {
            announce(message);
        }
    }

    // ----- Entscheidung -----

    private String buildJoinMessage(Player player) {
        if (isWatchedPlayer(player)) {
            return formatMessage("watched-join-message", player);
        }
        if (isAdmin(player)) {
            return formatMessage("admin-join-message", player);
        }
        return null;
    }

    private boolean isWatchedPlayer(Player player) {
        String watched = getWatchedName();
        return !watched.isEmpty() && player.getName().equalsIgnoreCase(watched);
    }

    private boolean isAdmin(Player player) {
        if (player.isOp()) {
            return true;
        }
        String perm = getAdminPermission();
        return !perm.isEmpty() && player.hasPermission(perm);
    }

    // ----- Konfiguration -----

    private String getWatchedName() {
        return plugin.getConfig().getString("notifications.watched-player", "");
    }

    private String getAdminPermission() {
        return plugin.getConfig().getString("notifications.admin-permission", "");
    }

    private String formatMessage(String configKey, Player player) {
        String template = plugin.getConfig().getString("notifications." + configKey, "%player% ist beigetreten.");
        return template.replace("%player%", player.getName());
    }

    // ----- Ausgabe -----

    private void announce(String rawMessage) {
        String colored = ChatColor.translateAlternateColorCodes('&', rawMessage);
        logToConsole(colored);
        broadcast(colored);
    }

    private void logToConsole(String coloredMessage) {
        plugin.getLogger().info(ChatColor.stripColor(coloredMessage));
    }

    private void broadcast(String coloredMessage) {
        Bukkit.broadcastMessage(coloredMessage);
    }
}
