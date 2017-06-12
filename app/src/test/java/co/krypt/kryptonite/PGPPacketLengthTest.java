package co.krypt.kryptonite;

import junit.framework.Assert;

import org.junit.Test;

import co.krypt.kryptonite.pgp.packet.OldPacketLengthType;
import co.krypt.kryptonite.pgp.packet.PacketTag;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class PGPPacketLengthTest {
    @Test
    public void oldFormatPacketLengthType_works() throws Exception {
        Assert.assertTrue(PacketTag.parse((byte) 0b10000000).lengthType == OldPacketLengthType.ONE_OCTET);
        Assert.assertTrue(PacketTag.parse((byte) 0b10000001).lengthType == OldPacketLengthType.TWO_OCTET);
        Assert.assertTrue(PacketTag.parse((byte) 0b10000010).lengthType == OldPacketLengthType.FOUR_OCTET);
    }
}