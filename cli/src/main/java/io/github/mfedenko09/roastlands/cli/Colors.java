package io.github.mfedenko09.roastlands.cli;

import java.util.Map;

record Colors(boolean enabled) {

    private static final String GREEN = "\u001B[32m";
    private static final String RED = "\u001B[31m";
    private static final String RESET = "\u001B[0m";
    private static final String TERM = "TERM";
    private static final String WINDOWS_TERMINAL_SESSION = "WT_SESSION";

    static Colors detect(boolean consoleAttached, Map<String, String> environment) {
        boolean knownTerminal = isSet(environment.get(TERM)) || isSet(environment.get(WINDOWS_TERMINAL_SESSION));
        return new Colors(consoleAttached && knownTerminal);
    }

    String green(String text) {
        return paint(GREEN, text);
    }

    String red(String text) {
        return paint(RED, text);
    }

    private String paint(String color, String text) {
        return enabled ? color + text + RESET : text;
    }

    private static boolean isSet(String value) {
        return value != null && !value.isBlank();
    }
}
