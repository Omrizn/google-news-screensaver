// SPDX-License-Identifier: Apache-2.0
package kiosk.googlenews;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Chooses which outlets' stories to show. Two comma-separated lists: the outlets to show (empty
 * means every outlet) and the outlets to hide. A name matches when it is part of the outlet's
 * name or of its website's address, ignoring case, so {@code ynet} matches both "Ynet" and
 * {@code www.ynet.co.il}. Hiding wins over showing.
 */
final class OutletFilter {
    static final OutletFilter NONE = new OutletFilter("", "");

    private final List<String> only;
    private final List<String> hide;
    /** The two lists as written once cleaned up, so equal filters have equal keys. */
    final String key;

    OutletFilter(String only, String hide) {
        this.only = tokens(only);
        this.hide = tokens(hide);
        this.key = String.join(",", this.only) + "|" + String.join(",", this.hide);
    }

    boolean active() {
        return !only.isEmpty() || !hide.isEmpty();
    }

    /** True when a story from this outlet and website should be shown. Either may be null. */
    boolean allows(String outlet, String site) {
        String name = outlet == null ? "" : outlet.toLowerCase(Locale.ROOT);
        String host = host(site);
        if (matches(hide, name, host)) return false;
        return only.isEmpty() || matches(only, name, host);
    }

    /** True when the outlet is on the hide list. Used for the names under "other coverage". */
    boolean hides(String outlet) {
        return outlet != null && matches(hide, outlet.toLowerCase(Locale.ROOT), "");
    }

    private static boolean matches(List<String> names, String outlet, String host) {
        for (String name : names) {
            if (outlet.contains(name) || host.contains(name)) return true;
        }
        return false;
    }

    /** The lowercase host of a website address, without {@code www.}. Empty when there is none. */
    static String host(String site) {
        if (site == null) return "";
        String s = site.trim().toLowerCase(Locale.ROOT);
        int scheme = s.indexOf("://");
        if (scheme >= 0) s = s.substring(scheme + 3);
        int end = s.length();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '/' || c == '?' || c == '#' || c == ':') { end = i; break; }
        }
        s = s.substring(0, end);
        return s.startsWith("www.") ? s.substring(4) : s;
    }

    /** Splits on commas, semicolons and line breaks, trims, lowercases and drops empty and repeated names. */
    static List<String> tokens(String list) {
        if (list == null) return Collections.emptyList();
        List<String> out = new ArrayList<>();
        for (String part : list.split("[,;\\n\\r]+")) {
            String name = part.trim().toLowerCase(Locale.ROOT);
            if (name.startsWith("www.")) name = name.substring(4);
            if (!name.isEmpty() && !out.contains(name)) out.add(name);
        }
        return out;
    }
}
