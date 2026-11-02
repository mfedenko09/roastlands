package io.github.mfedenko09.roastlands.game;

import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import io.github.mfedenko09.roastlands.core.progress.Reward;
import io.github.mfedenko09.roastlands.core.progress.RewardKind;
import io.github.mfedenko09.roastlands.core.progress.RewardTable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

final class CosmeticLook {

    private static final String DIFFUSE = "Diffuse";
    private static final String AMBIENT = "Ambient";
    private static final float AMBIENT_SHARE = 0.5f;

    private static final Map<String, MaterialTint> TINTS = Map.of(
            "cosmetic.scarf", new MaterialTint("shirt", new ColorRGBA(0.8f, 0.15f, 0.15f, 1f)),
            "cosmetic.cap", new MaterialTint("hair", new ColorRGBA(0.15f, 0.3f, 0.8f, 1f)),
            "cosmetic.backpack", new MaterialTint("pants", new ColorRGBA(0.2f, 0.5f, 0.25f, 1f)),
            "cosmetic.cape", new MaterialTint("shirt", new ColorRGBA(0.5f, 0.2f, 0.7f, 1f)));

    private CosmeticLook() {
    }

    static Map<String, ColorRGBA> tintsFor(Set<String> unlockedRewards) {
        Map<String, ColorRGBA> colorByMaterial = new LinkedHashMap<>();
        for (Reward reward : new RewardTable().all()) {
            MaterialTint tint = TINTS.get(reward.id());
            if (reward.kind() == RewardKind.COSMETIC && tint != null && unlockedRewards.contains(reward.id())) {
                colorByMaterial.put(tint.material(), tint.color());
            }
        }
        return colorByMaterial;
    }

    static void apply(Spatial model, Set<String> unlockedRewards) {
        paint(model, tintsFor(unlockedRewards));
    }

    private static void paint(Spatial spatial, Map<String, ColorRGBA> colorByMaterial) {
        if (spatial instanceof Node node) {
            node.getChildren().forEach(child -> paint(child, colorByMaterial));
        } else if (spatial instanceof Geometry geometry) {
            Material material = geometry.getMaterial();
            ColorRGBA color = colorByMaterial.get(material.getName());
            if (color != null) {
                material.setColor(DIFFUSE, color);
                material.setColor(AMBIENT, color.mult(AMBIENT_SHARE));
            }
        }
    }
}
