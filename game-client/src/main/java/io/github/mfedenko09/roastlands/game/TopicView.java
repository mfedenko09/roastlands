package io.github.mfedenko09.roastlands.game;

import io.github.mfedenko09.roastlands.core.mastery.MasteryLevel;
import lombok.Builder;

@Builder
record TopicView(String title, MasteryLevel level, String progress, boolean locked, String needs) {
}
