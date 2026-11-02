package io.github.mfedenko09.roastlands.core.progress;

import java.util.List;

public final class RewardTable {

    private static final List<Reward> REWARDS = List.of(
            new Reward("cosmetic.scarf", RewardKind.COSMETIC, 3),
            new Reward("cosmetic.cap", RewardKind.COSMETIC, 4),
            new Reward("title.middle", RewardKind.TITLE, 5),
            new Reward("cosmetic.backpack", RewardKind.COSMETIC, 7),
            new Reward("title.senior", RewardKind.TITLE, 10),
            new Reward("cosmetic.cape", RewardKind.COSMETIC, 12),
            new Reward("title.staff", RewardKind.TITLE, 20));

    public List<Reward> all() {
        return REWARDS;
    }

    public List<Reward> forLevels(int from, int to) {
        return REWARDS.stream().filter(reward -> reward.level() > from && reward.level() <= to).toList();
    }
}
