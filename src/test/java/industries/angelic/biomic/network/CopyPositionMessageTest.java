package industries.angelic.biomic.network;

import org.junit.Assert;
import org.junit.Test;

public class CopyPositionMessageTest {

    @Test
    public void formatsBareClipboardCoordinates() {
        Assert.assertEquals("-5603 221 4356", CopyPositionMessage.coordinateString(-5603, 221, 4356));
    }
}
