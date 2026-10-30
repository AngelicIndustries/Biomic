package industries.angelic.biomic.content;

import java.awt.Color;

import org.junit.Assert;
import org.junit.Test;

public class RuVegetationColorsTest {

    @Test
    public void silverBirchUsesTheExactModernFloatSineTintIncludingDistantCoordinates() {
        int[][] points = { { 352, 4864 }, { 137, -493 }, { -30000000, 30000000 }, { 1234567, -8765432 } };
        for (int[] point : points) for (boolean enchanted : new boolean[] { false, true }) {
            float wave = modernSin((float) point[0] / 10 + modernSin(((float) point[1] + (float) point[0]) / 50) * 3);
            int expected = Color
                .getHSBColor(
                    enchanted ? wave / 50 + .58F : wave / 75 + .15F,
                    enchanted ? .8F : .75F,
                    enchanted ? 1F : .8F)
                .getRGB() & 0xFFFFFF;
            Assert.assertEquals(expected, RuVegetationColors.aspen(point[0], point[1], enchanted));
        }
    }

    private static float modernSin(float angle) {
        int index = (int) (angle * 10430.378F) & 65535;
        return (float) Math.sin(index * Math.PI * 2 / 65536);
    }

}
