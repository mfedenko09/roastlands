package io.github.mfedenko09.roastlands.cli;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

final class AnswerParser {

    private static final String SEPARATORS = "[,\\s]+";

    Optional<Set<Integer>> parseSingle(String input, int optionCount) {
        return parseChoices(input, optionCount).filter(chosen -> chosen.size() == 1);
    }

    Optional<Set<Integer>> parseMulti(String input, int optionCount) {
        return parseChoices(input, optionCount);
    }

    Optional<Boolean> parseTrueFalse(String input) {
        return switch (input.toLowerCase(Locale.ROOT)) {
            case "t", "true" -> Optional.of(true);
            case "f", "false" -> Optional.of(false);
            default -> Optional.empty();
        };
    }

    Optional<Boolean> parseSure(String input) {
        return switch (input.toLowerCase(Locale.ROOT)) {
            case "y", "yes" -> Optional.of(true);
            case "n", "no" -> Optional.of(false);
            default -> Optional.empty();
        };
    }

    private static Optional<Set<Integer>> parseChoices(String input, int optionCount) {
        if (input.isBlank()) {
            return Optional.empty();
        }
        Set<Integer> chosen = new LinkedHashSet<>();
        for (String token : input.trim().split(SEPARATORS)) {
            Optional<Integer> index = indexOf(token, optionCount);
            if (index.isEmpty() || !chosen.add(index.get())) {
                return Optional.empty();
            }
        }
        return Optional.of(chosen);
    }

    private static Optional<Integer> indexOf(String token, int optionCount) {
        if (!token.matches("[1-9]\\d?")) {
            return Optional.empty();
        }
        int number = Integer.parseInt(token);
        return number <= optionCount ? Optional.of(number - 1) : Optional.empty();
    }
}
