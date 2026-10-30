package de.example.yourplugin;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.logging.Level;

public final class DependencyDownloader {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final JavaPlugin plugin;
    private final HttpClient client;

    public DependencyDownloader(JavaPlugin plugin) {
        this.plugin = plugin;
        this.client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(TIMEOUT)
                .build();
    }

    /**
     * Lädt die Datei von der URL herunter und speichert sie unter target.
     * Gibt true zurück, wenn der Download erfolgreich war.
     * Diese Methode darf nicht auf dem Hauptthread laufen.
     */
    public boolean download(String url, File target) {
        if (url == null || url.isBlank()) {
            plugin.getLogger().severe("Keine dependency.download-url in der config.yml gesetzt.");
            return false;
        }

        File partFile = new File(target.getParentFile(), target.getName() + ".part");

        try {
            HttpResponse<InputStream> response = sendRequest(url);
            if (!writeBody(response, partFile)) {
                deleteQuietly(partFile);
                return false;
            }
            moveIntoPlace(partFile, target);
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Fehler beim Download der Abhängigkeit.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            plugin.getLogger().log(Level.SEVERE, "Download der Abhängigkeit wurde unterbrochen.", e);
        }

        deleteQuietly(partFile);
        return false;
    }

    private HttpResponse<InputStream> sendRequest(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(TIMEOUT)
                .GET()
                .build();

        return client.send(request, HttpResponse.BodyHandlers.ofInputStream());
    }

    private boolean writeBody(HttpResponse<InputStream> response, File partFile) throws IOException {
        try (InputStream in = response.body()) {
            int status = response.statusCode();
            if (status != 200) {
                plugin.getLogger().severe("Download fehlgeschlagen, HTTP-Status: " + status);
                return false;
            }
            Files.copy(in, partFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return true;
        }
    }

    private void moveIntoPlace(File partFile, File target) throws IOException {
        try {
            Files.move(partFile.toPath(), target.toPath(),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(partFile.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void deleteQuietly(File file) {
        try {
            Files.deleteIfExists(file.toPath());
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Temporäre Datei konnte nicht gelöscht werden: " + file.getPath(), e);
        }
    }
}
