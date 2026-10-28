package io.github.mfedenko09.roastlands.cli;

import java.nio.file.Path;
import java.util.Locale;

final class ArgumentParser {

    static final String USAGE = "usage: roastlands-cli <review|status> [--content <dir>] [--profile <dir>]";

    private static final String CONTENT_OPTION = "--content";
    private static final String PROFILE_OPTION = "--profile";

    private final Path defaultContentDir;
    private final Path defaultProfileDir;

    ArgumentParser(Path defaultContentDir, Path defaultProfileDir) {
        this.defaultContentDir = defaultContentDir;
        this.defaultProfileDir = defaultProfileDir;
    }

    CliArguments parse(String[] args) {
        if (args.length == 0) {
            throw new UsageException("command is missing");
        }
        CliCommand command = commandOf(args[0]);
        Path contentDir = defaultContentDir;
        Path profileDir = defaultProfileDir;
        for (int i = 1; i < args.length; i += 2) {
            String value = valueOf(args, i);
            switch (args[i]) {
                case CONTENT_OPTION -> contentDir = Path.of(value);
                case PROFILE_OPTION -> profileDir = Path.of(value);
                default -> throw new UsageException("unknown option " + args[i]);
            }
        }
        return new CliArguments(command, contentDir, profileDir);
    }

    private static CliCommand commandOf(String name) {
        try {
            return CliCommand.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new UsageException("unknown command " + name);
        }
    }

    private static String valueOf(String[] args, int optionIndex) {
        if (!args[optionIndex].startsWith("--")) {
            throw new UsageException("unexpected argument " + args[optionIndex]);
        }
        if (optionIndex + 1 >= args.length) {
            throw new UsageException("option " + args[optionIndex] + " needs a value");
        }
        return args[optionIndex + 1];
    }
}
