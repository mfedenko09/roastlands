package io.github.mfedenko09.roastlands.game;

import java.util.List;

record DaySummary(int answered, int correct, int correctPercent, List<String> topicsUnlocked) {
}
