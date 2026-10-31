package io.github.mfedenko09.roastlands.core.progress;

public final class Levels {

    private static final long XP_SCALE = 100;
    private static final int FIRST_LEVEL = 1;

    public int levelOf(long xp) {
        return (int) Math.floor(Math.sqrt((double) xp / XP_SCALE)) + FIRST_LEVEL;
    }

    public long xpForLevel(int level) {
        if (level < FIRST_LEVEL) {
            throw new IllegalArgumentException("level must be at least 1: " + level);
        }
        long stepsAboveFirst = level - FIRST_LEVEL;
        return XP_SCALE * stepsAboveFirst * stepsAboveFirst;
    }
}
