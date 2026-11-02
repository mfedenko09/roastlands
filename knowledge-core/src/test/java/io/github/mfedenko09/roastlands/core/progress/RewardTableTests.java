package io.github.mfedenko09.roastlands.core.progress;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class RewardTableTests {

    private final RewardTable table = new RewardTable();

    @Test
    void levelThreeGivesTheScarf() {
        assertThat(table.forLevels(2, 3)).containsExactly(new Reward("cosmetic.scarf", RewardKind.COSMETIC, 3));
    }

    @Test
    void twoLevelsAtOnceGiveBothRewardsInLevelOrder() {
        assertThat(table.forLevels(2, 4)).extracting(Reward::id).containsExactly("cosmetic.scarf", "cosmetic.cap");
    }

    @Test
    void fromLevelIsExcludedAndToLevelIsIncluded() {
        assertThat(table.forLevels(3, 4)).extracting(Reward::id).containsExactly("cosmetic.cap");
        assertThat(table.forLevels(4, 4)).isEmpty();
    }

    @Test
    void levelWithoutRewardGivesNothing() {
        assertThat(table.forLevels(1, 2)).isEmpty();
    }

    @Test
    void reversedRangeGivesNothing() {
        assertThat(table.forLevels(5, 1)).isEmpty();
    }

    @Test
    void jumpAcrossAllLevelsGivesEveryReward() {
        assertThat(table.forLevels(1, 20)).hasSameSizeAs(table.all());
    }

    @Test
    void tableIsSortedByLevelAndIdsAreUnique() {
        List<Reward> all = table.all();

        assertThat(all).extracting(Reward::level).isSorted().doesNotHaveDuplicates();
        assertThat(all).extracting(Reward::id).doesNotHaveDuplicates();
    }

    @Test
    void displayNameIsTheCapitalisedIdSuffix() {
        assertThat(new Reward("cosmetic.backpack", RewardKind.COSMETIC, 7).displayName()).isEqualTo("Backpack");
        assertThat(new Reward("title.staff", RewardKind.TITLE, 20).displayName()).isEqualTo("Staff");
    }
}
