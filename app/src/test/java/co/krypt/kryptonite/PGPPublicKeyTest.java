package co.krypt.kryptonite;

import com.amazonaws.util.Base16;

import junit.framework.Assert;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Arrays;

import co.krypt.kryptonite.pgp.asciiarmor.AsciiArmor;
import co.krypt.kryptonite.pgp.publickey.CertifiedPublicKey;
import co.krypt.kryptonite.pgp.publickey.PublicKeyAlgorithm;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class PGPPublicKeyTest {
    public static final String testPubKey1 =
            "-----BEGIN PGP PUBLIC KEY BLOCK-----\n" +
                    "Comment: Some test hello\n" +
                    "\n" +
                    "mQENBFNdmhIBCAC5E6OdPAUvG15JHMST4gl+O1w5FCOoEa6zuO/032xpa6B58eR8\n" +
                    "xrS5ac4l9TUqQViJEKiL140IxLe3KyiiA9TckTSzbbjHbpmIuAebUqaquVx9wi20\n" +
                    "iSoaLIrqdNQKrDn+ByhZv7TAMAqK3kfg3Ofi/BQ1NJMnLrZ9VMr20EHtb+vXB7b/\n" +
                    "pdiYLPexeY03ewGZf7tZwMt5f/PsRngtH1ryIWDDXWsFprReuImBuuXVS+K3mEgQ\n" +
                    "REbBUudNChxoVvK4FbguR6dnQLKCkwX1uNPkjpA4RhROPB6dzjYFgRE3tcIZUZGf\n" +
                    "ofY+Xgcw6qnc4zQN9ED0eVbbkOm/aFgyTZN/ABEBAAG0IUtldmluIEtpbmcgPDRr\n" +
                    "ZXZpbmtpbmdAZ21haWwuY29tPokBHAQTAQIABgUCU2g3zQAKCRCYNjQs8yzl7APx\n" +
                    "CACAlHHvIix/SvPRGQSNOMpuC07TiI9TRkCt87w+2Ye9VRhpQcOXswTFAVpzloGC\n" +
                    "MW0hbcBF+iciVC4GB2+u7bgQxwBk8ifGZEfbmhh1b2NJkn3w9wlFk8A0b7/f5+O3\n" +
                    "0XW7Aj+sNewanVGLoFzZkW/R2p52b+2/IwXkeBrX3hzRw7lRUPATsDdR+IwBYQ/G\n" +
                    "aDXhK2MZkQgzFzkZi2V6uC5Gv/vCdwP5wjTUGAqeTHLB5zVoF1cMwmYetQ8mFXu6\n" +
                    "5RAeNOMCt9fz97lnz7t0KGziTFreB8Iukh0re+br6O6WakEdEHuAwUMR7bxWxZdI\n" +
                    "aDmUeA4trOEHG9xBiLV4xyquiQE9BBMBCgAnBQJTXZoSAhsDBQkHhh+ABQsJCAcD\n" +
                    "BRUKCQgLBRYCAwEAAh4BAheAAAoJEHoQN/XvB4kep7wH/iAOMmjEbbj96QDwjKNN\n" +
                    "x9VHcp5rW8VAgtNIOTALRKpzHgE4eniLcweVcWvHnsk2/sIGhy0PUVnMCX4qMm3A\n" +
                    "bnRGD8KD3dw2WPWy+uoMIMPuyASXWujwUMh4GjzjppPa7jIv0QwH4gEv7+ro0WHu\n" +
                    "R47EP1PEO3ZRpY7trV+YQs/pjGtSWewI+G3WfBuYGcmoQf5fR4fx/UfMqFx9/OFq\n" +
                    "GMnb8fEDA3LU0nCdfWDIOjgE84RRf9YpSSB4wspSLyR0iJiZ+/Cep0ot/ztiLiCw\n" +
                    "cldIDIntLxk7Ti2o9uz/sEEtnTI9vJEnz9/fyC4TM963T5Q7qkHR/5gZ8mAfAnlr\n" +
                    "gXm5AQ0EU12aEgEIAMBKbgMgbFjVdYVpSwAmXHEblHxs4aVviNmseKNH4mssGxp+\n" +
                    "yVurH4/sV0u13hnuT/GfMDa+T+CqA08i75iFBb9UmqTqVODwPmz919eiNb5rkyLu\n" +
                    "7WcZMavBTX8Lf+cN2MKMhRISTSkxDZDWZJz5lqMxO+c1QtyTAmdR883JgRh8jdWP\n" +
                    "rTQLO4mQY2t2/vBZP2LFdZ/R81hXGkreSgYlE8qC5VnMQB5UjqUOHdAWC0fZY3PR\n" +
                    "PCib5mcufqAY6PgOlTkOEKdlnGx36AAwyHSo7OlXkzJt3vH2EujAyZG3c90ducH+\n" +
                    "UH+ai3oHDQeBugAgsyf64Em6RnGEzJf8TigBEckAEQEAAYkBJQQYAQoADwUCU12a\n" +
                    "EgIbDAUJB4YfgAAKCRB6EDf17weJHmCBCACxxyhSdcM136daMOvXDByn9kSJcCOT\n" +
                    "iT4Hd6v0vgzh5ynz4zBRO1hzRXRNiiJtLMCZza9ftG1O2G0EyBhMHkKT8J3i3RCw\n" +
                    "8HF8Fd3yB2KYs0kaFGsYzZkzXCXaqjFLXJJqmLVTOUGScX8x2nrYWJXn4fBJUA8N\n" +
                    "d3EUC/3fSqqZTjq3BEE01gzy6wruGKVqoSziXWKm4PcKO32zRoMi2Hh7hIjYqzWq\n" +
                    "aZ5Pg/NzdwlX65sUVar1GzKbuHmxMgC8JtZvidqh/qoJk45tzIwj4GdIBbJbiSw9\n" +
                    "N0haqhW7w51HixXuh9SGaJTGfGccg1Zkw7fYZkUzusTtsFXhfcx/9S3Y\n" +
                    "=m4zw\n" +
                    "-----END PGP PUBLIC KEY BLOCK-----";

    public static final String testPubKey2 = "-----BEGIN PGP PUBLIC KEY BLOCK-----\n" +
            "Comment: Created by swift-pgp\n" +
            "\n" +
            "mQINBFkf1JQDEADeo0zoD6v3Fm04AEigtP2FzH0Kce1LMiTGxvTBLiFeaXmBNmKJ\n" +
            "J2h4DGYUTwCcS/5OCGNZVfUxrgW8TuOo/4nKm5IH2klWpT0wzMEK2VJ2fcrUmacI\n" +
            "MaVYxXRw8IEqZBX7Lr9QqPyxluJB11haiYEsaGPzRTdnXo1evoTH1uuvUkdi8uEF\n" +
            "QYGWl4dvafupdoetnPSCZu0r/g8SLHDCFwtypTj255tXd2iQmq3Lwk6/saISgpJV\n" +
            "qZOoKtofuWQTgcEcmy8HfvIMCYbNK3rHq85r04nBcwj7opf6Kd9SJa30SpOq+U0g\n" +
            "mBX0452HHR8+APd7W+gNEIfTJMyQblF/7kaFWEdPkHPogHmagH+RM0NUA9DZ7La4\n" +
            "tdUnFr7Bazsk/MMG+TK5zmtsBF15CkaCjNyGnAWxFnJurn4gsIXWOW00oJqKTmlI\n" +
            "PnpMW9sLJJLpH9xD12RNY/qMe+LcNT6LaM1MKfY9zyKP7gqEGhxH7cHfeBRcJ6Rt\n" +
            "Xrz3ZPtesW/9HpshI5oAPrMK275EfiR2wXN7e7mrLwqfth3K06O9XV9+WkZAjIgz\n" +
            "16AARbcy6qeRhYW9s8SqySrsbF8KResSLo+EbB8BL+P9UUjzWO/nc919MwVBXeqM\n" +
            "2R4h0SLmRodBzv+iU2go60+TlYH6bFrTfEE3G5n5l+6onlc0TAkvuNmhowARAQAB\n" +
            "tBthbGV4IGdyaW5tYW4gPG1lQGFsZXhnci5pbj6JAhwEEwMCAAYFAlkf1JQACgkQ\n" +
            "zTc/IxsJXFIhsw//Y8yBJGtdV0WbBIP5de5AKhYb7qB4F2LSmRK6z3RY4UbP7UW2\n" +
            "5iM0jUQOtqIqeYxeZLaCAUKOQ1oMUhnopSAv/JKC3afidmJScToiYGq7jVwYrFsS\n" +
            "69drv1M0CTMU5TvsMFSBotSFMl3MxrC1E6V5FezqYsHk73h80KdFogq/0n9z9Frv\n" +
            "BYsgvIhPZGikmj7AsmIuQENSrSX9pdVUAXTSmRwz+FYUneykqEyxGG13E7+A/YAv\n" +
            "kN2Wbv+DQ224iq61YhazfCL3amUjh+xSL6Oj0ksAS70PAXMGQoJTul4mHDVxlO7+\n" +
            "pD4MT51JjlvfKutMHPP1w2mJFbkUzvOIo0jGPq2XS/CB6T1Cu99sCis7XreTx8q4\n" +
            "nvbi/CNnvlSy99mhqzYTbNUKJEmisXfQkiAoRQWbzB7FAS0LtBdo5okYI4vFS64Z\n" +
            "VE8DeWz7cKhRi2FkZzNVvyIM5R86WUtnewu8SWaXywBEXydyeIqvX8Cexa2wwPlD\n" +
            "1NbQyj8XgFEIgASBwmefM8CANpCIVC2BCvSxekkRKz71Y1C/i+yck9sLCabEWbGd\n" +
            "lBji3fC2sCqqJyWepDym9YmpoWDCNb+1zcbOsO+N2OW24njmv7YcEE4SwWCOoll8\n" +
            "+PdoQROiBkM0Pab2sxT4ptymNA4XwXIczORqlJMFkc5j7QKroXG5v/TvdUk=\n" +
            "=HOsV\n" +
            "-----END PGP PUBLIC KEY BLOCK-----";

    public static final String testPubKey3 = "-----BEGIN PGP PUBLIC KEY BLOCK-----\n" +
            "Comment: GPGTools - https://gpgtools.org\n" +
            "\n" +
            "mDMEWR/vcRYJKwYBBAHaRw8BAQdAczcZVkPEdUEYYLVr6sr8ruEaGPo0N2w/wHSB\n" +
            "UBpof2q0J0FsZXggRWQyNTUxOSAodGVzdCkgPGVkMjU1MTlAYWxleGdyLmluPoiQ\n" +
            "BBMWCgA4FiEEIXCAaBWccFrnOgoBmxZiGt3JRJAFAlkf73ECGwMFCwkIBwMFFQoJ\n" +
            "CAsFFgIDAQACHgECF4AACgkQmxZiGt3JRJBdLQD/WVW+E+JYEapG8svSjk/vZZC+\n" +
            "jeSNLN1I97n8M3mueGkBAOmxBkmErMVnAPGNw9giZsaOmtSpZ0ceIpNMTE8CTB0L\n" +
            "=Wqf1\n" +
            "-----END PGP PUBLIC KEY BLOCK-----\n";

    public static final String testPubKey4 = "-----BEGIN PGP PUBLIC KEY BLOCK-----\n" +
            "Comment: Created by swift-pgp\n" +
            "\n" +
            "mDMEWR/+LBYJKwYBBAHaRw8BAQdAiue7CdTkwV9Y4IGc0YyknwAlj/vtP4Kjrgw/\n" +
            "F4nxsDG0G2FsZXggZ3Jpbm1hbiA8bWVAYWxleGdyLmluPohfBBMWCgAJBQJZH/4s\n" +
            "AhsCAAoJEOmnHqgxWly7mQAB/if3BAU2UMyuI6F7f7nPyj44S3WnpPV8Ae4QLT2j\n" +
            "oVXZ3qkdyA43+dTUXyJ81tkrD4zAGw7p+JZLjxgec+AiZwM=\n" +
            "=z7A0\n" +
            "-----END PGP PUBLIC KEY BLOCK-----\n";

    @Test
    public void publicKey1_parses() throws Exception {
        CertifiedPublicKey pk = CertifiedPublicKey.parse(new DataInputStream(new ByteArrayInputStream(AsciiArmor.parse(testPubKey1).data)));
        Assert.assertTrue(pk.publicKeyPacket.attributes.algorithm == PublicKeyAlgorithm.RSA_ENCRYPT_OR_SIGN);
        Assert.assertTrue(pk.identities.size() == 1);
        Assert.assertTrue(pk.identities.get(0).second.size() == 2);

        Assert.assertTrue(Arrays.equals(pk.publicKeyPacket.fingerprint(), Base16.decode("F7A83D5CE65C42817A4AB7647A1037F5EF07891E")));
        long keyID = pk.publicKeyPacket.keyID();
        ByteArrayOutputStream keyIDBuf = new ByteArrayOutputStream();
        DataOutputStream keyIDOut = new DataOutputStream(keyIDBuf);
        keyIDOut.writeLong(keyID);
        keyIDOut.close();
        Assert.assertTrue(Arrays.equals(keyIDBuf.toByteArray(), Base16.decode("7A1037F5EF07891E")));
    }

    @Test
    public void publicKey2_parses() throws Exception {
        CertifiedPublicKey pk = CertifiedPublicKey.parse(new DataInputStream(new ByteArrayInputStream(AsciiArmor.parse(testPubKey2).data)));
        Assert.assertTrue(pk.publicKeyPacket.attributes.algorithm == PublicKeyAlgorithm.RSA_SIGN_ONLY);
        Assert.assertTrue(pk.identities.size() == 1);
        Assert.assertTrue(pk.identities.get(0).second.size() == 1);
    }

    @Test
    public void publicKey3_parses() throws Exception {
        CertifiedPublicKey pk = CertifiedPublicKey.parse(new DataInputStream(new ByteArrayInputStream(AsciiArmor.parse(testPubKey3).data)));
        Assert.assertTrue(pk.publicKeyPacket.attributes.algorithm == PublicKeyAlgorithm.ED25519);
        Assert.assertTrue(pk.identities.size() == 1);
        Assert.assertTrue(pk.identities.get(0).second.size() == 1);
    }

    @Test
    public void publicKey4_pkUserIDAndIncompleteSignature_parsesPartialIdentity() throws Exception {
        CertifiedPublicKey pk = CertifiedPublicKey.parse(new DataInputStream(new ByteArrayInputStream(AsciiArmor.parse(testPubKey4).data)));
        Assert.assertTrue(pk.publicKeyPacket.attributes.algorithm == PublicKeyAlgorithm.ED25519);
        Assert.assertTrue(pk.identities.size() == 1);
        Assert.assertTrue(pk.identities.get(0).second.size() == 0);
    }
}