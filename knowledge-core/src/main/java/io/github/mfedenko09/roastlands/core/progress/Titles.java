package io.github.mfedenko09.roastlands.core.progress;

public final class Titles {

    private static final String FIRST_TITLE = "Junior";

    private final RewardTable rewards = new RewardTable();

    public String of(int level) {
        if (level < 1) {
            throw new IllegalArgumentException("level must be at least 1: " + level);
        }
        return rewards.all().stream()
                .filter(reward -> reward.kind() == RewardKind.TITLE && reward.level() <= level)
                .reduce((earlier, later) -> later)
                .map(Reward::displayName)
                .orElse(FIRST_TITLE);
    }
}
