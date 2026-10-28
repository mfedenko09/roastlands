package io.github.mfedenko09.roastlands.core.content;

import java.util.List;
import lombok.Builder;

@Builder(toBuilder = true)
public record Question(
        String id,
        String topic,
        QuestionType type,
        int difficulty,
        String prompt,
        String code,
        List<Option> options,
        CorrectAnswer correct,
        String explanation,
        String reference,
        int minJavaVersion,
        String source) {

    public Question {
        options = List.copyOf(options);
    }
}
