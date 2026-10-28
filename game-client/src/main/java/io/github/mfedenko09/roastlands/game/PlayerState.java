package io.github.mfedenko09.roastlands.game;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.input.ChaseCamera;
import com.jme3.input.InputManager;
import com.jme3.scene.Node;

class PlayerState extends PhysicsAwareState {

    private static final float RADIUS = 0.4f;
    private static final float HEIGHT = 1.8f;
    private static final float MASS = 80f;
    private static final float JUMP_FORCE = 450f;
    private static final float CAMERA_DISTANCE = 6f;

    private final InputBindings inputBindings;
    private final Node player = new Node("player");
    private final BetterCharacterControl character = new BetterCharacterControl(RADIUS, HEIGHT, MASS);
    private MovementInput input = MovementInput.NONE;
    private ChaseCamera chaseCamera;

    PlayerState(InputBindings inputBindings) {
        this.inputBindings = inputBindings;
    }

    @Override
    protected void initialize(Application app) {
        player.attachChild(CharacterModel.build(app.getAssetManager()));
        character.setJumpForce(new Vector3f(0f, JUMP_FORCE, 0f));
        player.addControl(character);
        inputBindings.onPlayerAction(this::onAction);
        chaseCamera = new ChaseCamera(app.getCamera(), player, app.getInputManager());
        chaseCamera.setDragToRotate(false);
        chaseCamera.setInvertVerticalAxis(true);
        chaseCamera.setDefaultDistance(CAMERA_DISTANCE);
        chaseCamera.setMaxDistance(CAMERA_DISTANCE);
        chaseCamera.setMinDistance(CAMERA_DISTANCE);
        chaseCamera.setLookAtOffset(new Vector3f(0f, HEIGHT, 0f));
    }

    @Override
    protected void onEnable() {
        SimpleApplication app = (SimpleApplication) getApplication();
        app.getRootNode().attachChild(player);
        addToPhysics(player);
        character.warp(RespawnRule.SPAWN_POINT);
    }

    @Override
    protected void onDisable() {
        removeFromPhysics(player);
        player.removeFromParent();
    }

    @Override
    public void update(float tpf) {
        hideCursor();
        Camera camera = getApplication().getCamera();
        Vector3f velocity = MovementCalculator.walkVelocity(camera.getDirection(), camera.getLeft(), input);
        character.setWalkDirection(velocity);
        if (velocity.lengthSquared() > 0f) {
            character.setViewDirection(velocity);
        }
        if (RespawnRule.shouldRespawn(player.getWorldTranslation())) {
            character.warp(RespawnRule.SPAWN_POINT);
        }
    }

    @Override
    protected void cleanup(Application app) {
        chaseCamera.cleanupWithInput(app.getInputManager());
        player.detachAllChildren();
    }

    void releaseKeys() {
        input = MovementInput.NONE;
    }

    private void hideCursor() {
        InputManager inputManager = getApplication().getInputManager();
        if (inputManager.isCursorVisible()) {
            inputManager.setCursorVisible(false);
        }
    }

    private void onAction(Action action, boolean pressed) {
        if (action == Action.JUMP) {
            if (pressed) {
                character.jump();
            }
            return;
        }
        input = input.with(action, pressed);
    }
}
