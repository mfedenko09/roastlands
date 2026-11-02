package io.github.mfedenko09.roastlands.core.progress;

public record Reward(String id, RewardKind kind, int level) {

    public String displayName() {
        String name = id.substring(id.indexOf('.') + 1);
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
