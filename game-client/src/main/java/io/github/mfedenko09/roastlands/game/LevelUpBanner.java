package io.github.mfedenko09.roastlands.game;

import io.github.mfedenko09.roastlands.core.progress.Reward;
import io.github.mfedenko09.roastlands.core.progress.RewardKind;
import io.github.mfedenko09.roastlands.core.progress.RewardTable;
import io.github.mfedenko09.roastlands.core.progress.Titles;
import java.util.ArrayList;
import java.util.List;

final class LevelUpBanner {

    private static final RewardTable REWARDS = new RewardTable();
    private static final Titles TITLES = new Titles();

    private LevelUpBanner() {
    }

    static String text(List<Integer> levelUps) {
        List<String> lines = new ArrayList<>();
        for (int level : levelUps) {
            lines.add("Level " + level + " reached!");
            REWARDS.forLevels(level - 1, level).stream().map(LevelUpBanner::rewardLine).forEach(lines::add);
        }
        return String.join("\n", lines);
    }

    private static String rewardLine(Reward reward) {
        return reward.kind() == RewardKind.TITLE
                ? "New title: " + TITLES.of(reward.level())
                : "New look: " + reward.displayName();
    }
}
