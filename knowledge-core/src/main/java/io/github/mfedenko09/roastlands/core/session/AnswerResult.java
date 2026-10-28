package io.github.mfedenko09.roastlands.core.session;

import io.github.mfedenko09.roastlands.core.content.CorrectAnswer;
import java.util.List;
import lombok.Builder;

@Builder
public record AnswerResult(
        boolean correct,
        CorrectAnswer correctAnswer,
        String explanation,
        String reference,
        String topicId,
        double masteryBefore,
        double masteryAfter,
        List<String> topicsUnlocked) {

    public AnswerResult {
        topicsUnlocked = List.copyOf(topicsUnlocked);
    }
}
