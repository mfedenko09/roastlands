package io.github.mfedenko09.roastlands.core.session;

public record SessionLimits(int maxReviews, int maxNew) {

    public static final SessionLimits DEFAULT = new SessionLimits(15, 5);
}
