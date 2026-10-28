package io.github.mfedenko09.roastlands.game;

import static org.assertj.core.api.Assertions.assertThat;

import com.jme3.system.AppSettings;
import org.junit.jupiter.api.Test;

class DisplaySettingsTests {

    @Test
    void windowedIsRoastlandsAt1280By720WithVsync() {
        AppSettings settings = DisplaySettings.windowed();

        assertThat(settings.getTitle()).isEqualTo("Roastlands");
        assertThat(settings.getWidth()).isEqualTo(1280);
        assertThat(settings.getHeight()).isEqualTo(720);
        assertThat(settings.isFullscreen()).isFalse();
        assertThat(settings.isVSync()).isTrue();
    }

    @Test
    void fullscreenUsesTheScreenSize() {
        AppSettings settings = DisplaySettings.fullscreen(1920, 1080);

        assertThat(settings.isFullscreen()).isTrue();
        assertThat(settings.getWidth()).isEqualTo(1920);
        assertThat(settings.getHeight()).isEqualTo(1080);
    }
}
