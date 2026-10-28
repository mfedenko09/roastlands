package io.github.mfedenko09.roastlands.core.profile;

import java.io.IOException;
import java.nio.file.Path;

@FunctionalInterface
interface FileMover {

    void move(Path from, Path to) throws IOException;
}
