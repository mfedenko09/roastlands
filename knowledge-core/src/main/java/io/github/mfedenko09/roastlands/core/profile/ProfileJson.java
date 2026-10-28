package io.github.mfedenko09.roastlands.core.profile;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

final class ProfileJson {

    private static final String SCHEMA_VERSION_FIELD = "schemaVersion";

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.INDENT_OUTPUT);

    byte[] write(PlayerProfile profile) throws JsonProcessingException {
        return (mapper.writeValueAsString(ProfileFile.of(profile)) + System.lineSeparator()).getBytes(UTF_8);
    }

    Optional<PlayerProfile> read(Path file) {
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        JsonNode root;
        try {
            root = mapper.readTree(file.toFile());
        } catch (IOException e) {
            return Optional.empty();
        }
        if (!root.isObject() || !root.path(SCHEMA_VERSION_FIELD).isInt()) {
            return Optional.empty();
        }
        int version = root.get(SCHEMA_VERSION_FIELD).intValue();
        if (version > PlayerProfile.CURRENT_SCHEMA_VERSION) {
            throw new UnsupportedProfileVersionException(version, PlayerProfile.CURRENT_SCHEMA_VERSION);
        }
        return version < 1 ? Optional.empty() : bind(root);
    }

    private Optional<PlayerProfile> bind(JsonNode root) {
        try {
            ProfileFile parsed = mapper.treeToValue(root, ProfileFile.class);
            return parsed.isComplete() ? Optional.of(parsed.toProfile()) : Optional.empty();
        } catch (JsonProcessingException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
