package learnpay.email.client;

import java.util.LinkedHashMap;
import java.util.Map;

final class Json {
    private Json() {}

    static String object(Map<String, ?> values) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            if (!first) json.append(',');
            first = false;
            json.append(quote(entry.getKey())).append(':').append(value(entry.getValue()));
        }
        return json.append('}').toString();
    }

    static String quote(String value) {
        StringBuilder escaped = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            switch (c) {
                case '\"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (c < 0x20) escaped.append(String.format("\\u%04x", (int) c));
                    else escaped.append(c);
                }
            }
        }
        return escaped.append('\"').toString();
    }

    private static String value(Object value) {
        if (value instanceof String text) return quote(text);
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> typed = new LinkedHashMap<>();
            map.forEach((key, item) -> typed.put(String.valueOf(key), item));
            return object(typed);
        }
        if (value instanceof Boolean || value instanceof Number) return value.toString();
        throw new IllegalArgumentException("Unsupported JSON value");
    }

    static boolean booleanField(String json, String name) {
        int valueStart = fieldValueStart(json, name);
        if (json.startsWith("true", valueStart)) return true;
        if (json.startsWith("false", valueStart)) return false;
        throw new IllegalArgumentException("Expected boolean field: " + name);
    }

    static String stringField(String json, String name) {
        int cursor = fieldValueStart(json, name);
        if (cursor >= json.length() || json.charAt(cursor) != '\"') {
            throw new IllegalArgumentException("Expected string field: " + name);
        }
        StringBuilder value = new StringBuilder();
        for (cursor++; cursor < json.length(); cursor++) {
            char c = json.charAt(cursor);
            if (c == '\"') return value.toString();
            if (c != '\\') {
                value.append(c);
                continue;
            }
            if (++cursor >= json.length()) throw new IllegalArgumentException("Invalid JSON escape");
            char escaped = json.charAt(cursor);
            switch (escaped) {
                case '\"', '\\', '/' -> value.append(escaped);
                case 'b' -> value.append('\b');
                case 'f' -> value.append('\f');
                case 'n' -> value.append('\n');
                case 'r' -> value.append('\r');
                case 't' -> value.append('\t');
                case 'u' -> {
                    if (cursor + 4 >= json.length()) throw new IllegalArgumentException("Invalid Unicode escape");
                    value.append((char) Integer.parseInt(json.substring(cursor + 1, cursor + 5), 16));
                    cursor += 4;
                }
                default -> throw new IllegalArgumentException("Invalid JSON escape");
            }
        }
        throw new IllegalArgumentException("Unterminated JSON string");
    }

    private static int fieldValueStart(String json, String name) {
        String key = quote(name);
        int field = json.indexOf(key);
        if (field < 0) throw new IllegalArgumentException("Missing response field: " + name);
        int colon = json.indexOf(':', field + key.length());
        if (colon < 0) throw new IllegalArgumentException("Invalid JSON response");
        int cursor = colon + 1;
        while (cursor < json.length() && Character.isWhitespace(json.charAt(cursor))) cursor++;
        return cursor;
    }
}
