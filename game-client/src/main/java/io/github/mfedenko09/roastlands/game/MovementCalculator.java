package io.github.mfedenko09.roastlands.game;

import com.jme3.math.Vector3f;

final class MovementCalculator {

    static final float WALK_SPEED = 4f;
    static final float RUN_SPEED = 7f;

    private MovementCalculator() {
    }

    static Vector3f walkVelocity(Vector3f cameraForward, Vector3f cameraLeft, MovementInput input) {
        Vector3f forward = flatten(cameraForward);
        Vector3f left = flatten(cameraLeft);
        Vector3f direction = new Vector3f();
        if (input.forward()) {
            direction.addLocal(forward);
        }
        if (input.back()) {
            direction.subtractLocal(forward);
        }
        if (input.left()) {
            direction.addLocal(left);
        }
        if (input.right()) {
            direction.subtractLocal(left);
        }
        if (direction.lengthSquared() == 0f) {
            return direction;
        }
        return direction.normalizeLocal().multLocal(input.run() ? RUN_SPEED : WALK_SPEED);
    }

    private static Vector3f flatten(Vector3f vector) {
        Vector3f flat = new Vector3f(vector.x, 0f, vector.z);
        return flat.lengthSquared() == 0f ? flat : flat.normalizeLocal();
    }
}
