package io.github.mfedenko09.roastlands.core.content;

import java.util.List;

public record ContentPack(String id, List<Question> questions) {

    public ContentPack {
        questions = List.copyOf(questions);
    }
}
