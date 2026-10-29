package io.github.mfedenko09.roastlands.game;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.collision.CollisionResults;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

class IslandState extends PhysicsAwareState {

    private static final float RAY_START_HEIGHT = 50f;
    private static final String ISLAND_MODEL = "Models/core-shore.obj";
    private static final ColorRGBA SKY = new ColorRGBA(0.55f, 0.72f, 0.85f, 1f);
    private static final ColorRGBA SUN = ColorRGBA.White.mult(0.9f);
    private static final ColorRGBA AMBIENT = ColorRGBA.White.mult(0.45f);
    private static final Vector3f SUN_DIRECTION = new Vector3f(-0.5f, -1f, -0.3f).normalizeLocal();

    private final Node island = new Node("island");
    private final DirectionalLight sun = new DirectionalLight(SUN_DIRECTION, SUN);
    private final AmbientLight ambient = new AmbientLight(AMBIENT);

    @Override
    protected void initialize(Application app) {
        AssetManager assets = app.getAssetManager();
        Spatial model = assets.loadModel(ISLAND_MODEL);
        island.attachChild(model);
        CollisionShape shape = CollisionShapeFactory.createMeshShape(island);
        island.addControl(new RigidBodyControl(shape, 0f));
        app.getViewPort().setBackgroundColor(SKY);
    }

    @Override
    protected void onEnable() {
        SimpleApplication app = (SimpleApplication) getApplication();
        app.getRootNode().attachChild(island);
        app.getRootNode().addLight(sun);
        app.getRootNode().addLight(ambient);
        addToPhysics(island);
    }

    @Override
    protected void onDisable() {
        SimpleApplication app = (SimpleApplication) getApplication();
        removeFromPhysics(island);
        app.getRootNode().removeLight(ambient);
        app.getRootNode().removeLight(sun);
        island.removeFromParent();
    }

    float groundHeightAt(float x, float z) {
        CollisionResults hits = new CollisionResults();
        island.collideWith(new Ray(new Vector3f(x, RAY_START_HEIGHT, z), Vector3f.UNIT_Y.negate()), hits);
        if (hits.size() == 0) {
            throw new IllegalStateException("The island has no ground at x=" + x + ", z=" + z);
        }
        return hits.getClosestCollision().getContactPoint().y;
    }

    @Override
    protected void cleanup(Application app) {
        island.detachAllChildren();
    }
}
