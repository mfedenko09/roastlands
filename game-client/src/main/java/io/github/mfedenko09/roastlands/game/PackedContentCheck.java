package io.github.mfedenko09.roastlands.game;

import io.github.mfedenko09.roastlands.core.content.ContentLoader;
import io.github.mfedenko09.roastlands.core.content.InvalidContentException;
import io.github.mfedenko09.roastlands.core.content.Question;
import java.nio.file.Path;
import java.util.List;

public final class PackedContentCheck {

    private PackedContentCheck() {
    }

    public static void main(String[] args) {
        verify(Path.of(args[0]));
    }

    static void verify(Path contentDir) {
        ContentLoader loader = new ContentLoader();
        List<Question> packed = loader.loadPacked(PackedContentCheck.class.getClassLoader()).pack().questions();
        if (!withoutSource(packed).equals(withoutSource(loader.load(contentDir).pack().questions()))) {
            throw new InvalidContentException(List.of("Packed content differs from " + contentDir));
        }
    }

    private static List<Question> withoutSource(List<Question> questions) {
        return questions.stream().map(question -> question.toBuilder().source(null).build()).toList();
    }
}
