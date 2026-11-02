package io.github.mfedenko09.roastlands.core.profile;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.mfedenko09.roastlands.core.progress.Levels;
import io.github.mfedenko09.roastlands.core.progress.RewardTable;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

final class ProfileJson {

    private static final String SCHEMA_VERSION_FIELD = "schemaVersion";
    private static final String XP_FIELD = "xp";
    private static final String UNLOCKED_REWARDS_FIELD = "unlockedRewards";
    private static final int FIRST_REWARDS_VERSION = 2;

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
        return version < 1 ? Optional.empty() : bind(migrated(root, version));
    }

    private JsonNode migrated(JsonNode root, int version) {
        if (version >= FIRST_REWARDS_VERSION) {
            return root;
        }
        ObjectNode upgraded = root.deepCopy();
        upgraded.set(UNLOCKED_REWARDS_FIELD, earnedRewards(root.path(XP_FIELD)));
        upgraded.put(SCHEMA_VERSION_FIELD, FIRST_REWARDS_VERSION);
        return upgraded;
    }

    private ArrayNode earnedRewards(JsonNode xp) {
        ArrayNode ids = mapper.createArrayNode();
        if (xp.isIntegralNumber()) {
            int level = new Levels().levelOf(xp.longValue());
            new RewardTable().forLevels(0, level).forEach(reward -> ids.add(reward.id()));
        }
        return ids;
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
