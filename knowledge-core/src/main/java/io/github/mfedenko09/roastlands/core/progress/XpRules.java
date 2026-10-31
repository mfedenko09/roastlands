package io.github.mfedenko09.roastlands.core.progress;

public final class XpRules {

    public static final long QUEST_BONUS = 50;

    private static final long XP_PER_DIFFICULTY = 10;
    private static final long WRONG_ANSWER_XP = 2;

    public long forAnswer(boolean correct, int difficulty) {
        return correct ? XP_PER_DIFFICULTY * difficulty : WRONG_ANSWER_XP;
    }
}
