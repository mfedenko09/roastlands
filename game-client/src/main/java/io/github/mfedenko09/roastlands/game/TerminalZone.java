package io.github.mfedenko09.roastlands.game;

import com.jme3.math.Vector3f;

final class TerminalZone {

    static final float RADIUS = 2f;

    private TerminalZone() {
    }

    static boolean contains(Vector3f terminal, Vector3f player) {
        float dx = player.x - terminal.x;
        float dz = player.z - terminal.z;
        return dx * dx + dz * dz <= RADIUS * RADIUS;
    }
}
