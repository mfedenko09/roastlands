package io.github.mfedenko09.roastlands.cli;

import io.github.mfedenko09.roastlands.core.content.ContentLoader;
import io.github.mfedenko09.roastlands.core.content.GameContent;
import io.github.mfedenko09.roastlands.core.content.InvalidContentException;
import io.github.mfedenko09.roastlands.core.profile.ProfileCorruptedException;
import io.github.mfedenko09.roastlands.core.profile.ProfileStore;
import io.github.mfedenko09.roastlands.core.profile.UnsupportedProfileVersionException;
import io.github.mfedenko09.roastlands.core.topic.InvalidTopicMapException;
import java.io.UncheckedIOException;
import java.time.LocalDate;

public final class RoastlandsCli {

    static final int OK = 0;
    static final int FAILED = 1;
    static final int USAGE_ERROR = 2;

    private static final String ERROR_PREFIX = "roastlands-cli: ";

    private RoastlandsCli() {
    }

    public static void main(String[] args) {
        System.exit(run(args, Environment.system()));
    }

    static int run(String[] args, Environment environment) {
        CliArguments arguments;
        try {
            arguments = new ArgumentParser(environment.defaultContentDir(), environment.defaultProfileDir())
                    .parse(args);
        } catch (UsageException e) {
            environment.err().println(ERROR_PREFIX + e.getMessage());
            environment.err().println(ArgumentParser.USAGE);
            return USAGE_ERROR;
        }
        try {
            return execute(arguments, environment);
        } catch (InvalidContentException | InvalidTopicMapException | ProfileCorruptedException
                | UnsupportedProfileVersionException | UncheckedIOException e) {
            environment.err().println(ERROR_PREFIX + e.getMessage());
            return FAILED;
        }
    }

    private static int execute(CliArguments arguments, Environment environment) {
        GameContent content = new ContentLoader().load(arguments.contentDir());
        ProfileStore store = new ProfileStore(arguments.profileDir(), content.topics().rootTopicIds());
        Terminal terminal = new Terminal(environment.in(), environment.out(), environment.colors());
        LocalDate today = LocalDate.now(environment.clock());
        return switch (arguments.command()) {
            case REVIEW -> new ReviewCommand(content, store, terminal, today).run();
            case STATUS -> new StatusCommand(content, store, terminal, today).run();
        };
    }
}
