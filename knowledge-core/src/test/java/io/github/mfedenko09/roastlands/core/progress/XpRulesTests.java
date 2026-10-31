package io.github.mfedenko09.roastlands.core.progress;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class XpRulesTests {

    private final XpRules rules = new XpRules();

    @Test
    void correctAnswerOnDifficultyTwoGivesTwenty() {
        assertThat(rules.forAnswer(true, 2)).isEqualTo(20);
    }

    @Test
    void correctAnswerScalesWithDifficulty() {
        assertThat(rules.forAnswer(true, 1)).isEqualTo(10);
        assertThat(rules.forAnswer(true, 3)).isEqualTo(30);
    }

    @Test
    void wrongAnswerGivesTwoOnAnyDifficulty() {
        assertThat(rules.forAnswer(false, 1)).isEqualTo(2);
        assertThat(rules.forAnswer(false, 2)).isEqualTo(2);
        assertThat(rules.forAnswer(false, 3)).isEqualTo(2);
    }

    @Test
    void questBonusIsFifty() {
        assertThat(XpRules.QUEST_BONUS).isEqualTo(50);
    }
}
