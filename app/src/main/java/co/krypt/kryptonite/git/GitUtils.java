package co.krypt.kryptonite.git;

import android.text.format.DateUtils;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.MalformedInputException;

import javax.annotation.Nullable;

/**
 * Created by Kevin King on 6/18/17.
 * Copyright 2016. KryptCo, Inc.
 */

public class GitUtils {
    @Nullable
    public static String getNameAndEmail(String s) {
        int endOfEmail = s.indexOf('>');
        if (endOfEmail < 0) {
            return null;
        }
        return s.substring(0, endOfEmail + 1);
    }

    @Nullable
    public static String getTimeAfterEmail(String s) {
        Long time = getUnixSecondsAfterEmail(s);
        if (time != null) {
            return DateUtils.getRelativeTimeSpanString(time * 1000, System.currentTimeMillis(), 1000)
                    .toString().toLowerCase();
        }
        return null;
    }

    @Nullable
    public static Long getUnixSecondsAfterEmail(String s) {
        int endOfEmail = s.indexOf('>');
        if (endOfEmail < 0) {
            return null;
        }
        String timeString = s.substring(endOfEmail + 1, s.length()).trim();
        String[] toks = timeString.split(" ");
        if (toks.length < 1) {
            return null;
        }
        try {
            long time = Long.parseLong(toks[0]);
            return time;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String validatedStringOrPrefixError(byte[] message, String error) {
        try {
            return Charset.forName("UTF-8").newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(message)).toString();
        } catch (MalformedInputException e) {
            e.printStackTrace();
        } catch (CharacterCodingException e) {
            e.printStackTrace();
        }
        return error + ": " + new String(message);
    }
}
