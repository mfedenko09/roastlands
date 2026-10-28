package io.github.mfedenko09.roastlands.core.answer;

import io.github.mfedenko09.roastlands.core.srs.Grade;
import java.util.Objects;

public final class GradeResolver {

    public Grade resolve(boolean correct, Confidence confidence) {
        if (!correct) {
            return Grade.AGAIN;
        }
        Objects.requireNonNull(confidence, "confidence is required for a correct answer");
        return confidence == Confidence.HIGH ? Grade.GOOD : Grade.HARD;
    }
}
