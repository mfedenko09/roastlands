package io.github.mfedenko09.roastlands.game;

import com.jme3.math.Vector3f;

final class RespawnRule {

    static final float FALL_LIMIT_Y = -20f;
    static final Vector3f SPAWN_POINT = new Vector3f(0f, 3f, 0f);

    private RespawnRule() {
    }

    static boolean shouldRespawn(Vector3f position) {
        return position.y < FALL_LIMIT_Y;
    }
}
