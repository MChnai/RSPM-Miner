package web;

import java.util.Collection;
import java.util.Locale;

/** Minimal JSON serialization for our fixed DTOs (no extra dependencies). */
public final class Json {
    private Json() {}

    public static String q(String value) {
        if (value == null) return "null";
        StringBuilder b = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"': b.append("\\\""); break;
                case '\\': b.append("\\\\"); break;
                case '\n': b.append("\\n"); break;
                case '\r': b.append("\\r"); break;
                case '\t': b.append("\\t"); break;
                default:
                    if (c < 32) b.append(String.format("\\u%04x", (int)c));
                    else b.append(c);
            }
        }
        return b.append('"').toString();
    }

    public static String n(Double value) {
        return value != null && Double.isFinite(value)
                ? String.format(Locale.ROOT, "%.3f", value) : "null";
    }

    public static String array(Collection<String> items) {
        return "[" + String.join(",", items) + "]";
    }
}
