package io.github.mfedenko09.roastlands.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.jme3.math.Vector3f;
import org.junit.jupiter.api.Test;

class MovementCalculatorTests {

    private static final Vector3f NORTH = new Vector3f(0f, 0f, 1f);
    private static final Vector3f NORTH_LEFT = new Vector3f(1f, 0f, 0f);
    private static final MovementInput FORWARD = MovementInput.NONE.with(Action.FORWARD, true);

    @Test
    void noKeysMeansStanding() {
        assertThat(MovementCalculator.walkVelocity(NORTH, NORTH_LEFT, MovementInput.NONE)).isEqualTo(Vector3f.ZERO);
    }

    @Test
    void forwardWalksAlongTheCameraDirection() {
        Vector3f velocity = MovementCalculator.walkVelocity(NORTH, NORTH_LEFT, FORWARD);

        assertThat(velocity).isEqualTo(new Vector3f(0f, 0f, 4f));
    }

    @Test
    void forwardFollowsACameraTurnedTowardsPlusX() {
        Vector3f east = new Vector3f(1f, 0f, 0f);
        Vector3f eastLeft = new Vector3f(0f, 0f, -1f);

        Vector3f velocity = MovementCalculator.walkVelocity(east, eastLeft, FORWARD);

        assertThat(velocity).isEqualTo(new Vector3f(4f, 0f, 0f));
    }

    @Test
    void lookingDownDoesNotSlowOrLiftTheCharacter() {
        Vector3f velocity = MovementCalculator.walkVelocity(new Vector3f(0f, -0.6f, 0.8f), NORTH_LEFT, FORWARD);

        assertThat(velocity).isEqualTo(new Vector3f(0f, 0f, 4f));
    }

    @Test
    void lookingStraightDownGivesNoForwardMovement() {
        Vector3f velocity = MovementCalculator.walkVelocity(new Vector3f(0f, -1f, 0f), NORTH_LEFT, FORWARD);

        assertThat(velocity).isEqualTo(Vector3f.ZERO);
    }

    @Test
    void backwardLeftAndRightUseTheOppositeAndSideDirections() {
        assertThat(velocity(Action.BACK)).isEqualTo(new Vector3f(0f, 0f, -4f));
        assertThat(velocity(Action.LEFT)).isEqualTo(new Vector3f(4f, 0f, 0f));
        assertThat(velocity(Action.RIGHT)).isEqualTo(new Vector3f(-4f, 0f, 0f));
    }

    @Test
    void diagonalMovementIsNotFasterThanStraight() {
        MovementInput forwardRight = FORWARD.with(Action.RIGHT, true);

        Vector3f velocity = MovementCalculator.walkVelocity(NORTH, NORTH_LEFT, forwardRight);

        assertThat(velocity.x).isCloseTo(-2.8284f, within(0.0001f));
        assertThat(velocity.z).isCloseTo(2.8284f, within(0.0001f));
    }

    @Test
    void oppositeKeysCancelOut() {
        MovementInput both = FORWARD.with(Action.BACK, true);

        assertThat(MovementCalculator.walkVelocity(NORTH, NORTH_LEFT, both)).isEqualTo(Vector3f.ZERO);
    }

    @Test
    void shiftRunsFasterThanWalking() {
        Vector3f velocity = MovementCalculator.walkVelocity(NORTH, NORTH_LEFT, FORWARD.with(Action.RUN, true));

        assertThat(velocity).isEqualTo(new Vector3f(0f, 0f, 7f));
    }

    @Test
    void releasingAKeyStopsThatDirection() {
        MovementInput released = FORWARD.with(Action.FORWARD, false);

        assertThat(MovementCalculator.walkVelocity(NORTH, NORTH_LEFT, released)).isEqualTo(Vector3f.ZERO);
    }

    @Test
    void jumpAndFullscreenDoNotChangeMovementInput() {
        assertThat(FORWARD.with(Action.JUMP, true)).isEqualTo(FORWARD);
        assertThat(FORWARD.with(Action.FULLSCREEN, true)).isEqualTo(FORWARD);
    }

    private static Vector3f velocity(Action action) {
        return MovementCalculator.walkVelocity(NORTH, NORTH_LEFT, MovementInput.NONE.with(action, true));
    }
}
