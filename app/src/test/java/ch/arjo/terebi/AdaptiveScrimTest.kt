package ch.arjo.terebi

import ch.arjo.terebi.scene.AdaptiveScrim
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveScrimTest {
    @Test fun guideScrimsAreEnoughEvenForPureWhite() {
        // Kontrasttabelle 3.4: mit globalem Scrim 0.16 liegen alle Textzonen bei mindestens 0.69
        AdaptiveScrim.TEXT_ZONES.forEach { assertTrue(it.name, AdaptiveScrim.effective(AdaptiveScrim.BASE, it.localAlpha) >= 0.69) }
        assertEquals(0.16, AdaptiveScrim.globalAlpha(mapOf("rail" to 1.0, "header" to 1.0, "context" to 1.0)), 1e-9)
    }

    @Test fun weakerLocalScrimsRaiseGlobalInSteps() {
        val weak = AdaptiveScrim.Zone("rail", 0.50)
        assertTrue(AdaptiveScrim.effective(AdaptiveScrim.BASE, weak.localAlpha) < 0.69)
        // gleiche Rechnung wie globalAlpha, mit einer schwächeren Zone: 1-(1-g)*0.5 >= 0.69 => g >= 0.38
        var g = AdaptiveScrim.BASE
        while (AdaptiveScrim.effective(g, weak.localAlpha) < 0.69 && g < 0.40) g += 0.04
        assertEquals(0.40, g, 1e-9)
    }

    @Test fun darkPhotoNeedsNoExtraScrim() {
        assertEquals(0.0, AdaptiveScrim.required(0.2), 1e-9)
        assertEquals(0.69, AdaptiveScrim.required(1.0), 1e-9)
        assertEquals(1.0, AdaptiveScrim.luminance(0xFFFFFFFF.toInt()), 1e-6)
        assertEquals(1.0, AdaptiveScrim.percentile95(emptyList()), 1e-9)
    }
}
