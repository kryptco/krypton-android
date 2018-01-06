package co.krypt.krypton;

import android.support.v4.util.Pair;

import com.amazonaws.util.Base16;

import junit.framework.Assert;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import co.krypt.krypton.crypto.Base64;
import co.krypt.krypton.git.CommitInfo;
import co.krypt.krypton.pgp.asciiarmor.AsciiArmor;

/**
 * Created by Kevin King on 6/20/17.
 * Copyright 2017. KryptCo, Inc.
 */

public class GitTest {
    @Test
    public void testCommitHash() throws Exception {
        byte[] sigMessage = Base64.decode("iF4EABYKAAYFAlkkmD8ACgkQ4eT0x9ceFp1gNQD+LWiJFax8iQqgr0yJ1P7JFGvMwuZc8r05h6U+X+lyKYEBAK939lEX1rvBmcetftVbRlOMX5oQZwBLt/NJh+nQ3ssC");
        CommitInfo commit = new CommitInfo(
                "2c4df4a89ac5b0b8b21fd2aad4d9b19cd91e7049",
                "1cd97d0545a25c578e3f4da5283106606276eadf",
                null,
                "Alex Grinman <alex@krypt.co> 1495570495 -0400",
                "Alex Grinman <alex@krypt.co> 1495570495 -0400",
                "\ntest1234\n".getBytes("UTF-8")
        );

        AsciiArmor aa = new AsciiArmor(
                AsciiArmor.HeaderLine.SIGNATURE,
                Collections.singletonList(
                        new Pair<String, String>("Comment", "Created With Kryptonite")
                ),
                sigMessage
        );

        Assert.assertTrue(
                Arrays.equals(
                        commit.commitHash(aa.toString()),
                        Base16.decode("84e09dac58d81b1f3fc4806b1b4cb18af3cca0ea")
                )
        );
    }
}
