package io.github.mfedenko09.roastlands.core.profile;

import java.time.LocalDate;

public record Streak(int current, int best, LocalDate lastCompletedDate) {

    public static Streak none() {
        return new Streak(0, 0, null);
    }
}
