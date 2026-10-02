package net.busybee.clearlaggenhanced.core.updater;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;

/**
 * Adds settings that a newer plugin version introduced to an existing config file, at every depth.
 * Nothing the server owner wrote is changed: the missing keys are inserted into the file's text with
 * their default comments, so the owner's values, quotes, comments and layout stay exactly as they were.
 * A backup of the old file is written to a {@code backups/} folder next to it first, and nothing is
 * touched when there is nothing to add.
 *
 * <p>Sections listed in {@link #OWNER_COLLECTIONS} hold the owner's own entries (per-type limits,
 * optional overrides). They are left exactly as they are: entries the owner removed are not put
 * back, and nothing is added to the entries they kept.
 */
public class ConfigMigrator {

    static final String QUOTE_HINT = "Hint: put text in double quotes, especially text containing & % : # or starting with { [ * ! @";
    private static final int MAX_LOGGED_KEYS = 8;

    private static final Map<String, Set<String>> OWNER_COLLECTIONS = Map.ofEntries(
            Map.entry("module/entity-clearing/config.yml", Set.of("notifications.clear-complete")),
            Map.entry("module/misc-entity-limiter/config.yml", Set.of("limits-per-chunk")),
            Map.entry("module/mob-limiter/config.yml", Set.of("per-type-limits.limits")));

    private final Plugin plugin;

    public ConfigMigrator(Plugin plugin) {
        this.plugin = plugin;
    }

    static Set<String> ownerCollections(String relativePath) {
        return OWNER_COLLECTIONS.getOrDefault(relativePath, Set.of());
    }

    /**
     * @param relativePath path of the packaged default inside the jar, e.g. {@code module/afk/config.yml}
     * @param file         the server's copy of that file
     * @return true if the file was updated
     */
    public boolean migrate(String relativePath, File file) {
        if (!file.exists()) return false;

        String defaultsText = loadDefaults(relativePath);
        if (defaultsText == null) return false;

        String userText;
        YamlConfiguration user = new YamlConfiguration();
        YamlConfiguration defaults = new YamlConfiguration();
        try {
            userText = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            user.loadFromString(userText);
        } catch (IOException | InvalidConfigurationException e) {
            // Never overwrite something we could not parse. ConfigFiles.load reports it to the owner.
            return false;
        }
        try {
            defaults.loadFromString(defaultsText);
        } catch (InvalidConfigurationException e) {
            plugin.getLogger().log(Level.WARNING, "Packaged defaults for " + relativePath + " are not valid YAML", e);
            return false;
        }

        YamlText userDoc = new YamlText(userText);
        YamlText defaultsDoc = new YamlText(defaultsText);
        List<String> added = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        addMissing(defaults, defaultsDoc, defaultsDoc.root, user, userDoc, userDoc.root, "",
                ownerCollections(relativePath), added, skipped);

        if (!skipped.isEmpty()) {
            plugin.getLogger().warning("Could not add " + skipped.size() + " new setting(s) to " + relativePath
                    + " automatically; copy them from the default file if you need them: " + list(skipped));
        }
        if (added.isEmpty()) return false;

        String merged = userDoc.render();
        if (!keepsOwnerValues(user, merged, added)) {
            plugin.getLogger().warning("Could not add new settings to " + relativePath
                    + " without changing existing values, so the file was left as it is. New settings: " + list(added));
            return false;
        }

        backup(file, relativePath);
        try {
            Files.writeString(file.toPath(), merged, StandardCharsets.UTF_8);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save updated " + relativePath + "; the old file was kept.", e);
            return false;
        }

        plugin.getLogger().info("Updated " + relativePath + ": added " + added.size() + " new key(s): " + list(added));
        return true;
    }

    private void addMissing(ConfigurationSection defaults, YamlText defaultsDoc, YamlText.Node defaultsNode,
                            ConfigurationSection user, YamlText userDoc, YamlText.Node userNode,
                            String path, Set<String> collections, List<String> added, List<String> skipped) {
        // The owner's own entries: nothing is put back and nothing is added inside them.
        if (collections.contains(path)) return;
        List<String> keys = new ArrayList<>(defaults.getKeys(false));
        for (int index = 0; index < keys.size(); index++) {
            String key = keys.get(index);
            String fullPath = path.isEmpty() ? key : path + "." + key;
            YamlText.Node defaultChild = defaultsNode.children.get(key);

            if (user.contains(key, true)) {
                // Owner's value wins, including when they changed the shape of the key.
                if (defaults.isConfigurationSection(key) && user.isConfigurationSection(key)) {
                    YamlText.Node userChild = userNode.children.get(key);
                    if (defaultChild != null && userChild != null) {
                        addMissing(defaults.getConfigurationSection(key), defaultsDoc, defaultChild,
                                user.getConfigurationSection(key), userDoc, userChild, fullPath, collections, added, skipped);
                    }
                }
                continue;
            }
            if (!userDoc.openForChildren(userNode)) {
                skipped.add(fullPath);
                continue;
            }

            int position = insertPosition(keys, index, userNode);
            int indent = userNode == userDoc.root ? 0
                    : !userNode.children.isEmpty() ? userNode.children.values().iterator().next().indent
                    : defaultChild != null ? userNode.indent + (defaultChild.indent - defaultsNode.indent)
                    : userNode.indent + 2;
            List<String> block;
            if (defaultChild != null) {
                block = defaultsDoc.block(defaultChild, indent - defaultChild.indent);
                if (defaultsDoc.spacedAbove(defaultChild) && !userDoc.blankAt(position)) block.add(0, "");
            } else {
                // The default is written inline, e.g. filler: {material: X, name: ' '}, so there is no line to copy.
                block = dump(key, defaults.get(key), indent);
            }
            userDoc.insertBefore(position, block);
            added.add(fullPath);
        }
    }

    private List<String> dump(String key, Object value, int indent) {
        YamlConfiguration single = new YamlConfiguration();
        single.set(key, value);
        String prefix = " ".repeat(indent);
        return single.saveToString().lines().map(line -> prefix + line).toList();
    }

    /** After the closest earlier default key the owner has, else before the closest later one, else at the end of the section. */
    private int insertPosition(List<String> keys, int index, YamlText.Node userNode) {
        for (int i = index - 1; i >= 0; i--) {
            YamlText.Node sibling = userNode.children.get(keys.get(i));
            if (sibling != null) return sibling.end;
        }
        for (int i = index + 1; i < keys.size(); i++) {
            YamlText.Node sibling = userNode.children.get(keys.get(i));
            if (sibling != null) return sibling.commentStart;
        }
        return userNode.end;
    }

    /** The merged text must parse, contain every added key, and hold the owner's values unchanged. */
    private boolean keepsOwnerValues(YamlConfiguration user, String merged, List<String> added) {
        YamlConfiguration check = new YamlConfiguration();
        try {
            check.loadFromString(merged);
        } catch (InvalidConfigurationException e) {
            return false;
        }
        for (String path : added) {
            if (!check.contains(path, true)) return false;
        }
        for (String path : user.getKeys(true)) {
            if (user.isConfigurationSection(path)) continue;
            if (!Objects.equals(user.get(path), check.get(path))) return false;
        }
        return true;
    }

    private String list(List<String> keys) {
        int shown = Math.min(keys.size(), MAX_LOGGED_KEYS);
        String listed = String.join(", ", keys.subList(0, shown));
        if (keys.size() > shown) listed += ", ... (+" + (keys.size() - shown) + " more)";
        return listed;
    }

    private String loadDefaults(String relativePath) {
        try (InputStream stream = plugin.getResource(relativePath)) {
            if (stream == null) return null;
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to read packaged defaults for: " + relativePath, e);
            return null;
        }
    }

    private void backup(File file, String relativePath) {
        File dir = new File(file.getParentFile(), "backups");
        if (!dir.exists() && !dir.mkdirs()) return;

        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        File target = new File(dir, file.getName() + ".backup-" + timestamp);
        try {
            Files.copy(file.toPath(), target.toPath());
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to back up " + relativePath + " before updating it.", e);
        }
    }

    static String describeCause(Throwable throwable) {
        Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
        String message = cause.getMessage();
        return message == null ? cause.toString() : message;
    }
}
