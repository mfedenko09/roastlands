package io.github.mfedenko09.roastlands.core.content;

import com.fasterxml.jackson.databind.JsonNode;

record QuestionEntry(
        String id,
        String topic,
        String type,
        Integer difficulty,
        Integer keyPoint,
        String prompt,
        String code,
        JsonNode options,
        JsonNode correct,
        String explanation,
        String reference,
        Integer minJavaVersion) {
}
