package io.github.mfedenko09.roastlands.game;

import static org.assertj.core.api.Assertions.assertThat;

import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import io.github.mfedenko09.roastlands.core.progress.Reward;
import io.github.mfedenko09.roastlands.core.progress.RewardKind;
import io.github.mfedenko09.roastlands.core.progress.RewardTable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class CosmeticLookTests {

    private static final ColorRGBA RED = new ColorRGBA(0.8f, 0.15f, 0.15f, 1f);
    private static final ColorRGBA PURPLE = new ColorRGBA(0.5f, 0.2f, 0.7f, 1f);
    private static final ColorRGBA BLUE = new ColorRGBA(0.15f, 0.3f, 0.8f, 1f);

    @Test
    void noRewardsLeaveTheCharacterAsItIs() {
        assertThat(CosmeticLook.tintsFor(Set.of())).isEmpty();
    }

    @Test
    void scarfTintsTheShirtAndTitlesAndUnknownIdsAreIgnored() {
        assertThat(CosmeticLook.tintsFor(Set.of("cosmetic.scarf", "title.middle", "cosmetic.unknown")))
                .containsExactly(Map.entry("shirt", RED));
    }

    @Test
    void laterCosmeticWinsOnTheSameMaterial() {
        assertThat(CosmeticLook.tintsFor(Set.of("cosmetic.scarf", "cosmetic.cape", "cosmetic.cap")))
                .containsOnly(Map.entry("shirt", PURPLE), Map.entry("hair", BLUE));
    }

    @Test
    void everyCosmeticOfTheTableHasATint() {
        Set<String> cosmetics = new RewardTable().all().stream()
                .filter(reward -> reward.kind() == RewardKind.COSMETIC)
                .map(Reward::id)
                .collect(Collectors.toSet());

        assertThat(CosmeticLook.tintsFor(cosmetics).keySet()).containsExactlyInAnyOrder("shirt", "hair", "pants");
    }

    @Test
    void scarfPaintsTheShirtOfTheShippedCharacterModelOnly() {
        Spatial model = characterModel();

        CosmeticLook.apply(model, Set.of("cosmetic.scarf"));

        assertThat(diffuseOf(model, "shirt")).isEqualTo(RED);
        assertThat(diffuseOf(model, "pants")).isNotEqualTo(RED);
    }

    @Test
    void everyTintedMaterialExistsInTheShippedCharacterModel() {
        Spatial model = characterModel();
        Set<String> all = Set.of("cosmetic.scarf", "cosmetic.cap", "cosmetic.backpack", "cosmetic.cape");

        assertThat(materialNames(model)).containsAll(CosmeticLook.tintsFor(all).keySet());
    }

    private static Spatial characterModel() {
        DesktopAssetManager assets = new DesktopAssetManager(true);
        assets.registerLocator("/assets", ClasspathLocator.class);
        return assets.loadModel("Models/character.obj");
    }

    private static ColorRGBA diffuseOf(Spatial model, String materialName) {
        return materials(model).stream()
                .filter(material -> materialName.equals(material.getName()))
                .findFirst()
                .orElseThrow()
                .getParamValue("Diffuse");
    }

    private static List<String> materialNames(Spatial model) {
        return materials(model).stream().map(Material::getName).toList();
    }

    private static List<Material> materials(Spatial spatial) {
        List<Material> found = new ArrayList<>();
        if (spatial instanceof Node node) {
            node.getChildren().forEach(child -> found.addAll(materials(child)));
        } else if (spatial instanceof Geometry geometry) {
            found.add(geometry.getMaterial());
        }
        return found;
    }
}
