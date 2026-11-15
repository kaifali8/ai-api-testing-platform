package com.kaif.apitestplatform.execution;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;

final class SecretMasker {

    private static final Set<String> SENSITIVE_HEADERS = Set.of(
            "authorization", "proxy-authorization", "cookie", "set-cookie",
            "x-api-key", "api-key", "apikey", "x-auth-token");

    private SecretMasker() {}

    static boolean isSensitiveHeader(String name) {
        return name != null && SENSITIVE_HEADERS.contains(name.toLowerCase(Locale.ROOT));
    }

    /** Replaces every occurrence of any known secret with ***. */
    static String mask(String text, Collection<String> secrets) {
        if (text == null) return null;
        String out = text;
        for (String s : secrets) {
            if (s != null && s.length() >= 4) {
                out = out.replace(s, "***");
            }
        }
        return out;
    }
}