package io.github.mfedenko09.roastlands.core.progress;

import io.github.mfedenko09.roastlands.core.profile.Streak;
import java.time.LocalDate;

public final class StreakRules {

    public Streak onQuestCompleted(Streak streak, LocalDate today) {
        LocalDate last = streak.lastCompletedDate();
        if (last != null && !today.isAfter(last)) {
            return streak;
        }
        int current = today.minusDays(1).equals(last) ? streak.current() + 1 : 1;
        return new Streak(current, Math.max(current, streak.best()), today);
    }

    public int currentOn(Streak streak, LocalDate today) {
        LocalDate last = streak.lastCompletedDate();
        return last != null && last.isBefore(today.minusDays(1)) ? 0 : streak.current();
    }
}
