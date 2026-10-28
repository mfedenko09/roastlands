package io.github.mfedenko09.roastlands.cli;

import java.nio.file.Path;

record CliArguments(CliCommand command, Path contentDir, Path profileDir) {
}
