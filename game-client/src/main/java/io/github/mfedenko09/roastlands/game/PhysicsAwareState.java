package io.github.mfedenko09.roastlands.game;

import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.scene.Spatial;

abstract class PhysicsAwareState extends BaseAppState {

    void addToPhysics(Spatial spatial) {
        getStateManager().getState(BulletAppState.class).getPhysicsSpace().add(spatial);
    }

    void removeFromPhysics(Spatial spatial) {
        PhysicsSpace space = getStateManager().getState(BulletAppState.class).getPhysicsSpace();
        if (space != null) {
            space.remove(spatial);
        }
    }
}
