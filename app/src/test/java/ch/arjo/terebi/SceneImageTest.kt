package ch.arjo.terebi

import ch.arjo.terebi.scene.SceneResolver
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class SceneImageTest {
    // Sonnenzeiten vom 18.11.2026 (Guide 9.4): 07:34 und 16:47
    private val rise = 7 * 60 + 34
    private val set = 16 * 60 + 47
    private fun at(h: Int, m: Int) = SceneResolver.phase(h * 60 + m, rise, set)

    @Test fun phaseBoundariesFollowGuide94() {
        assertEquals("night", at(6, 48)); assertEquals("dawn", at(6, 49)); assertEquals("dawn", at(8, 19)); assertEquals("day", at(8, 20))
        assertEquals("day", at(16, 1)); assertEquals("dusk", at(16, 2)); assertEquals("dusk", at(17, 32)); assertEquals("night", at(17, 33))
    }

    @Test fun weatherScenesUseTheComputedPhaseClearScenesTheirOwn() {
        assertEquals("scene_wet_dusk_1", SceneResolver.imageName("rain", "dusk"))
        assertEquals("scene_wet_night_1", SceneResolver.imageName("storm", "night"))
        assertEquals("scene_winter_day_1", SceneResolver.imageName("snow", "day"))
        assertEquals("scene_clear_dawn_1", SceneResolver.imageName("sunrise", "night"))
        assertEquals("scene_clear_night_1", SceneResolver.imageName("evening", "dusk"))
    }

    @Test fun globalScrimTableOfGuide95() {
        assertEquals(0.28, SceneResolver.baseScrim("scene_winter_day_1"), 1e-9)
        assertEquals(0.16, SceneResolver.baseScrim("scene_wet_night_1"), 1e-9)
        assertEquals(0.22, SceneResolver.baseScrim("scene_fog_day_1"), 1e-9)
    }

    @Test fun allTwentyImagesExistInAssets() {
        for (g in listOf("clear", "cloudy", "fog", "wet", "winter")) for (p in listOf("dawn", "day", "dusk", "night"))
            assert(File("../assets/backgrounds/scene_${g}_${p}_1.webp").exists()) { "fehlt: scene_${g}_${p}_1.webp" }
    }
}
