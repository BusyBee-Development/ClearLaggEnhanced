package com.clearlagenhanced.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.clearlagenhanced.ClearLaggEnhanced;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class VersionCheck implements Listener {

    private static final String MODRINTH_PROJECT_SLUG = "clearlaggenhanced";
    private static final String MODRINTH_VERSIONS_API =
            "https://api.modrinth.com/v2/project/" + MODRINTH_PROJECT_SLUG + "/version";
    private static final String MODRINTH_PROJECT_URL =
            "https://modrinth.com/plugin/" + MODRINTH_PROJECT_SLUG;

    private final ClearLaggEnhanced plugin;
    private volatile String latestVersion;

    public VersionCheck(@NotNull ClearLaggEnhanced plugin) {
        this.plugin = plugin;
        fetchLatestVersion();
    }

    private void fetchLatestVersion() {
        CompletableFuture.runAsync(() -> {
            try {
                URL url = new URL(MODRINTH_VERSIONS_API);
                try (InputStreamReader reader = new InputStreamReader(url.openStream())) {
                    JsonArray versionList = JsonParser.parseReader(reader).getAsJsonArray();
                    if (versionList != null && !versionList.isEmpty()) {
                        JsonObject selected = null;
                        String maxDate = null;
                        for (JsonElement el : versionList) {
                            if (el == null || !el.isJsonObject()) {
                                continue;
                            }

                            JsonObject obj = el.getAsJsonObject();
                            if (!obj.has("date_published") || obj.get("date_published").isJsonNull()) {
                                continue;
                            }

                            String date = obj.get("date_published").getAsString();
                            if (maxDate == null || (date != null && date.compareTo(maxDate) > 0)) {
                                maxDate = date;
                                selected = obj;
                            }
                        }

                        if (selected == null && !versionList.isEmpty()) {
                            selected = versionList.get(0).getAsJsonObject();
                        }

                        if (selected != null && selected.has("version_number") && !selected.get("version_number").isJsonNull()) {
                            this.latestVersion = selected.get("version_number").getAsString();
                        }
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Could not check for Modrinth updates: " + e.getMessage());
            }
        });
    }

    @EventHandler
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (latestVersion == null || !player.hasPermission("CLE.admin")) {
            return;
        }

        String currentVersion = plugin.getDescription().getVersion();
        if (isNewerVersion(latestVersion, currentVersion)) {
            List<String> lines = plugin.getMessageManager().getConfig().getStringList("update-notifier");
            String prefix = plugin.getMessageManager().getConfig().getString("prefix", "<gray>[</gray><gold>ClearLaggEnhanced</gold><gray>]</gray>");
            MiniMessage mm = MiniMessage.miniMessage();

            if (lines.isEmpty()) {
                Component fallback = mm.deserialize(
                        prefix + " <yellow>Update available!</yellow> <gray>(Current: </gray><red><current></red><gray>, New: </gray><green><latest></green><gray>)</gray> <aqua><u>Click to open</u></aqua>",
                        Placeholder.unparsed("current", currentVersion),
                        Placeholder.unparsed("latest", latestVersion)
                ).clickEvent(ClickEvent.openUrl(MODRINTH_PROJECT_URL));
                player.sendMessage(fallback);
                return;
            }

            lines.set(0, prefix + " " + lines.get(0));

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);

                Component component = mm.deserialize(line,
                        Placeholder.unparsed("current_version", currentVersion),
                        Placeholder.unparsed("new_version", latestVersion)
                );

                if (i == lines.size() - 1) {
                    component = component.clickEvent(ClickEvent.openUrl(MODRINTH_PROJECT_URL));
                }

                player.sendMessage(component);
            }
        }
    }

    private boolean isNewerVersion(@NotNull String version1, @NotNull String version2) {

        String v1 = version1.replaceAll("[^\\d.]", "");
        String v2 = version2.replaceAll("[^\\d.]", "");

        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        int length = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < length; i++) {
            int num1 = (i < parts1.length) ? parseSafe(parts1[i]) : 0;
            int num2 = (i < parts2.length) ? parseSafe(parts2[i]) : 0;
            if (num1 > num2) {
                return true;
            }

            if (num1 < num2) {
                return false;
            }
        }

        return false;
    }

    private int parseSafe(@NotNull String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
