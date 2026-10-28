package io.github.mfedenko09.roastlands.game;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import java.util.List;

final class CharacterModel {

    private static final String LIGHTING = "Common/MatDefs/Light/Lighting.j3md";
    private static final List<BodyPart> PARTS = List.of(
            new BodyPart("body", new Vector3f(0.35f, 0.6f, 0.2f), 0.6f, new ColorRGBA(0.45f, 0.28f, 0.16f, 1f)),
            new BodyPart("head", new Vector3f(0.25f, 0.25f, 0.25f), 1.55f, new ColorRGBA(0.85f, 0.68f, 0.5f, 1f)));

    private CharacterModel() {
    }

    static Node build(AssetManager assets) {
        Node model = new Node("character-model");
        PARTS.forEach(part -> model.attachChild(geometry(assets, part)));
        return model;
    }

    private static Geometry geometry(AssetManager assets, BodyPart part) {
        Vector3f half = part.halfSize();
        Geometry geometry = new Geometry(part.name(), new Box(half.x, half.y, half.z));
        Material material = new Material(assets, LIGHTING);
        material.setBoolean("UseMaterialColors", true);
        material.setColor("Diffuse", part.color());
        material.setColor("Ambient", part.color());
        geometry.setMaterial(material);
        geometry.setLocalTranslation(0f, part.centerY(), 0f);
        return geometry;
    }
}
