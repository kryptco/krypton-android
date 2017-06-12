package co.krypt.kryptonite;

import junit.framework.Assert;

import org.junit.Test;

import co.krypt.kryptonite.pgp.asciiarmor.AsciiArmor;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class AsciiArmorTest {
    @Test
    public void publicKey_parses() throws Exception {
        AsciiArmor aa = AsciiArmor.parse(PGPPublicKeyTest.testPubKey1);
        Assert.assertTrue(aa.headerLine.equals(AsciiArmor.HeaderLine.PUBLIC_KEY));
        Assert.assertTrue(aa.headers.size() == 1);
        Assert.assertTrue(aa.headers.get(0).first.equals("Comment"));
        Assert.assertTrue(aa.headers.get(0).second.equals("Some test hello"));
    }
}