package io.github.mfedenko09.roastlands.game;

import static org.assertj.core.api.Assertions.assertThat;

import com.jme3.math.Vector3f;
import org.junit.jupiter.api.Test;

class RespawnRuleTests {

    @Test
    void respawnsOnlyBelowMinusTwenty() {
        assertThat(RespawnRule.shouldRespawn(new Vector3f(5f, -20.01f, 3f))).isTrue();
        assertThat(RespawnRule.shouldRespawn(new Vector3f(5f, -20f, 3f))).isFalse();
        assertThat(RespawnRule.shouldRespawn(new Vector3f(0f, 2f, 0f))).isFalse();
    }

    @Test
    void spawnPointIsAboveTheIslandCentre() {
        assertThat(RespawnRule.SPAWN_POINT).isEqualTo(new Vector3f(0f, 3f, 0f));
    }
}
