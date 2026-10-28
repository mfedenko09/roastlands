package io.github.mfedenko09.roastlands.core.content;

import java.util.List;

record ParsedQuestion(Question question, List<String> reasons) {

    boolean isValid() {
        return reasons.isEmpty();
    }
}
