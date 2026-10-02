package net.busybee.clearlaggenhanced.core.updater;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Edits a YAML file as text, so everything already in it (quotes, comments, spacing, key order)
 * stays exactly as the owner wrote it. Saving through Bukkit would re-emit the whole file in
 * SnakeYAML's style instead, dropping or changing quotes.
 *
 * <p>Understands the block style the packaged files use: one key per line, nesting by indentation,
 * lists as {@code - item} lines. It only locates keys; callers decide what exists from a parsed
 * {@code YamlConfiguration} and re-parse the result to confirm an edit did what they expected.
 */
final class YamlText {

    /** {@code key:} or {@code key: value}; the key may be quoted. List items and flow values are not keys. */
    private static final Pattern KEY_LINE = Pattern.compile(
            "^( *)(\"(?:[^\"\\\\]|\\\\.)*\"|'(?:[^']|'')*'|[^\\s#\"'{\\[\\-][^#]*?)[ \\t]*:(?:[ \\t]+(.*))?$");
    private static final Pattern PLAIN_TEXT = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
    private static final Set<String> RESERVED = Set.of("true", "false", "yes", "no", "on", "off", "null", "y", "n");

    static final class Node {
        final String key;
        final int indent;
        final int line;
        boolean inlineValue;
        final Map<String, Node> children = new LinkedHashMap<>();
        /** First line of the comment block directly above the key. */
        int commentStart;
        /** Line after the last line of this key's block. */
        int end;

        private Node(String key, int indent, int line, boolean inlineValue) {
            this.key = key;
            this.indent = indent;
            this.line = line;
            this.inlineValue = inlineValue;
            this.commentStart = line;
            this.end = line + 1;
        }
    }

    private final List<String> lines;
    private final String newline;
    private final boolean trailingNewline;
    private final TreeMap<Integer, List<String>> pending = new TreeMap<>();
    final Node root;

    YamlText(String text) {
        newline = text.contains("\r\n") ? "\r\n" : "\n";
        trailingNewline = text.endsWith("\n");
        String body = trailingNewline ? text.substring(0, text.length() - (text.endsWith("\r\n") ? 2 : 1)) : text;
        lines = body.isEmpty() ? new ArrayList<>() : new ArrayList<>(Arrays.asList(body.split("\r?\n", -1)));
        root = parse();
    }

    private Node parse() {
        Node top = new Node("", -1, -1, false);
        top.end = lines.size();
        Deque<Node> open = new ArrayDeque<>();
        open.push(top);
        List<Node> all = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank() || isComment(line)) continue;
            Matcher matcher = KEY_LINE.matcher(line);
            if (!matcher.matches()) continue;
            int indent = matcher.group(1).length();
            while (open.peek().indent >= indent) open.pop();
            String value = matcher.group(3);
            boolean inline = value != null && !value.isBlank() && !value.startsWith("#");
            Node node = new Node(unquote(matcher.group(2).strip()), indent, i, inline);
            open.peek().children.putIfAbsent(node.key, node);
            open.push(node);
            all.add(node);
        }
        for (Node node : all) {
            node.commentStart = commentStart(node);
            node.end = blockEnd(node);
        }
        return top;
    }

    private int commentStart(Node node) {
        int start = node.line;
        while (start > 0) {
            String previous = lines.get(start - 1);
            if (previous.isBlank() || !isComment(previous) || indentOf(previous) != node.indent) break;
            start--;
        }
        return start;
    }

    private int blockEnd(Node node) {
        int end = node.line + 1;
        for (int i = node.line + 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) continue;
            int indent = indentOf(line);
            // Bukkit writes list items at the same indent as their key.
            boolean sameIndentListItem = indent == node.indent && !node.inlineValue && line.stripLeading().startsWith("-");
            if (indent <= node.indent && !sameIndentListItem) break;
            end = i + 1;
        }
        return end;
    }

    /** The node at a Bukkit path such as {@code settings.is-active}, or null. */
    Node find(String path) {
        Node node = root;
        for (String part : path.split("\\.")) {
            node = node.children.get(part);
            if (node == null) return null;
        }
        return node;
    }

    /** The lines of {@code node}'s block, including the comments above it, shifted right by {@code shift} spaces. */
    List<String> block(Node node, int shift) {
        List<String> out = new ArrayList<>();
        for (String line : lines.subList(node.commentStart, node.end)) {
            out.add(shift(line, shift));
        }
        return out;
    }

    /** True if the line above {@code node}'s comments is blank, i.e. the block is set apart from what precedes it. */
    boolean spacedAbove(Node node) {
        return node.commentStart > 0 && lines.get(node.commentStart - 1).isBlank();
    }

    /** True if the text that will end up directly above {@code index} is a blank line or the start of the file. */
    boolean blankAt(int index) {
        List<String> queued = pending.get(index);
        if (queued != null && !queued.isEmpty()) return queued.get(queued.size() - 1).isBlank();
        return index == 0 || lines.get(index - 1).isBlank();
    }

    /**
     * Makes room for child keys under {@code node}: turns {@code key: {}} (how Bukkit saves an empty
     * section) into {@code key:}. Returns false if the node holds any other inline value.
     */
    boolean openForChildren(Node node) {
        if (!node.inlineValue) return true;
        String line = lines.get(node.line);
        Matcher matcher = KEY_LINE.matcher(line);
        if (!matcher.matches()) return false;
        String value = matcher.group(3).strip();
        String comment = "";
        int hash = value.indexOf('#');
        if (hash >= 0) {
            comment = " " + value.substring(hash);
            value = value.substring(0, hash).strip();
        }
        if (!value.equals("{}")) return false;
        lines.set(node.line, line.substring(0, line.indexOf(':', matcher.end(2)) + 1) + comment);
        node.inlineValue = false;
        return true;
    }

    /** Queues lines to go in before line {@code index} (or at the end for {@code index == size}). */
    void insertBefore(int index, List<String> block) {
        pending.computeIfAbsent(index, ignored -> new ArrayList<>()).addAll(block);
    }

    /**
     * Replaces the value of an existing scalar or list key in place, keeping the key's own text and
     * any comment after a plain value. Returns false if the key is not in the text or holds a section.
     * Line positions of other nodes are stale afterwards, so call it once per instance.
     */
    boolean setValue(String path, Object value) {
        Node node = find(path);
        if (node == null || (!node.children.isEmpty() && !holdsList(node))) return false;
        String line = lines.get(node.line);
        Matcher matcher = KEY_LINE.matcher(line);
        if (!matcher.matches()) return false;
        String keyPart = line.substring(0, line.indexOf(':', matcher.end(2)) + 1);
        String oldValue = matcher.group(3);

        List<String> replacement = new ArrayList<>();
        if (value instanceof List<?> list) {
            if (list.isEmpty()) {
                replacement.add(keyPart + " []");
            } else {
                // Keep the owner's list layout: where their items sit and whether they quote them.
                int itemIndent = node.indent + 2;
                boolean quoted = false;
                for (String existing : lines.subList(node.line + 1, node.end)) {
                    String item = existing.stripLeading();
                    if (!item.startsWith("-")) continue;
                    if (!quoted && item.substring(1).stripLeading().startsWith("\"")) quoted = true;
                    itemIndent = Math.min(itemIndent, indentOf(existing));
                }

                replacement.add(keyPart);
                String prefix = " ".repeat(itemIndent);
                for (Object item : list) {
                    if (item instanceof Map<?, ?> map && !map.isEmpty()) {
                        String lead = prefix + "- ";
                        for (Map.Entry<?, ?> entry : map.entrySet()) {
                            replacement.add(lead + entry.getKey() + ": " + scalar(entry.getValue(), false));
                            lead = prefix + "  ";
                        }
                    } else {
                        replacement.add(prefix + "- " + scalar(item, quoted));
                    }
                }
            }
        } else {
            String comment = "";
            if (oldValue != null && !oldValue.startsWith("\"") && !oldValue.startsWith("'")) {
                int hash = oldValue.indexOf(" #");
                if (hash >= 0) comment = oldValue.substring(hash);
            }
            boolean quoted = oldValue != null && oldValue.startsWith("\"");
            replacement.add(keyPart + " " + scalar(value, quoted) + comment);
        }

        List<String> old = lines.subList(node.line, node.end);
        old.clear();
        old.addAll(replacement);
        return true;
    }

    /** True if the key's block starts with a list item; keys inside {@code - key: value} items parse as children. */
    private boolean holdsList(Node node) {
        for (String line : lines.subList(node.line + 1, node.end)) {
            if (line.isBlank() || isComment(line)) continue;
            return line.stripLeading().startsWith("-");
        }
        return false;
    }

    /**
     * Deletes a key, its block and the comments directly above it. Returns false if the key is not in
     * the text. Line positions of other nodes are stale afterwards, so call it once per instance.
     */
    boolean remove(String path) {
        Node node = find(path);
        if (node == null) return false;
        lines.subList(node.commentStart, node.end).clear();
        return true;
    }

    String render() {
        List<String> out = new ArrayList<>(lines.size());
        for (int i = 0; i < lines.size(); i++) {
            List<String> queued = pending.get(i);
            if (queued != null) out.addAll(queued);
            out.add(lines.get(i));
        }
        List<String> tail = pending.get(lines.size());
        if (tail != null) out.addAll(tail);
        String text = String.join(newline, out);
        return trailingNewline || lines.isEmpty() ? text + newline : text;
    }

    /** @param quoted write text in double quotes even where YAML does not need them, to match the owner's style */
    private static String scalar(Object value, boolean quoted) {
        if (value instanceof Boolean || value instanceof Number) return value.toString();
        String text = String.valueOf(value);
        if (!quoted && PLAIN_TEXT.matcher(text).matches() && !RESERVED.contains(text.toLowerCase())) return text;
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static String unquote(String key) {
        if (key.length() >= 2 && key.startsWith("\"") && key.endsWith("\"")) {
            return key.substring(1, key.length() - 1).replace("\\\"", "\"").replace("\\\\", "\\");
        }
        if (key.length() >= 2 && key.startsWith("'") && key.endsWith("'")) {
            return key.substring(1, key.length() - 1).replace("''", "'");
        }
        return key;
    }

    private static String shift(String line, int shift) {
        if (line.isBlank() || shift == 0) return line;
        if (shift > 0) return " ".repeat(shift) + line;
        return line.substring(Math.min(-shift, indentOf(line)));
    }

    private static boolean isComment(String line) {
        return line.stripLeading().startsWith("#");
    }

    private static int indentOf(String line) {
        int indent = 0;
        while (indent < line.length() && line.charAt(indent) == ' ') indent++;
        return indent;
    }
}
