package co.krypt.krypton.pgp.asciiarmor;

import android.support.v4.util.Pair;

import com.github.zafarkhaja.semver.Version;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import co.krypt.krypton.crypto.Base64;
import co.krypt.krypton.crypto.CRC24;
import co.krypt.krypton.exception.CryptoException;
import co.krypt.krypton.pgp.packet.UnsupportedHeaderLineException;

/**
 * Created by Kevin King on 6/13/17.
 * Copyright 2017. KryptCo, Inc.
 */

public class AsciiArmor {
    public enum HeaderLine {
        PUBLIC_KEY("PGP PUBLIC KEY BLOCK"),
        SIGNATURE("PGP SIGNATURE"),
        ;
        public final String s;
        HeaderLine(String s) {
            this.s = s;
        }

        public static HeaderLine fromString(String s) throws UnsupportedHeaderLineException {
            switch (s) {
                case "PGP PUBLIC KEY BLOCK":
                    return PUBLIC_KEY;
                case "PGP SIGNATURE":
                    return SIGNATURE;
            }
            throw new UnsupportedHeaderLineException();
        }
    }

    private static final String HEADER_LINE_PREFIX = "-----BEGIN ";
    private static final String HEADER_LINE_SUFFIX = "-----";
    private static final String LAST_LINE_PREFIX = "-----END ";
    private static final String LAST_LINE_SUFFIX = "-----";

    public static List<Pair<String, String>> backwardsCompatibleHeaders(Version requestVersion) {
        if (requestVersion.greaterThanOrEqualTo(Version.valueOf("2.3.1"))) {
            return AsciiArmor.KRYPTON_DEFAULT_HEADERS;
        } else {
            return AsciiArmor.KRYPTONITE_DEFAULT_HEADERS;
        }
    }

    public static final List<Pair<String, String>> KRYPTONITE_DEFAULT_HEADERS = Collections.singletonList(new Pair<String, String>(
            "Comment", "Created with Kryptonite"
    ));
    public static final List<Pair<String, String>> KRYPTON_DEFAULT_HEADERS = Collections.singletonList(new Pair<String, String>(
            "Comment", "Created with Kryptonite"
    ));

    public final HeaderLine headerLine;
    public final List<Pair<String, String>> headers;
    public final byte[] data;

    public AsciiArmor(HeaderLine headerLine, List<Pair<String, String>> headers, byte[] data) {
        this.headerLine = headerLine;
        this.headers = headers;
        this.data = data;
    }

    public static AsciiArmor parse(String text) throws InvalidAsciiArmorException, UnsupportedHeaderLineException, CryptoException {
        List<String> lines = new LinkedList<>();
        Scanner scanner = new Scanner(text);
        int dataStart = 1;
        boolean foundEmptyLine = false;
        for (int idx = 0; scanner.hasNextLine(); idx++) {
            String line = scanner.nextLine().trim();
            if (line.equals("")) {
                if (foundEmptyLine) {
                    break;
                }
                foundEmptyLine = true;
                dataStart = idx + 1;
            }
            lines.add(line);
        }
        scanner.close();

        final int dataEnd = lines.size() - 2;

        if (dataStart > dataEnd) {
            throw new InvalidAsciiArmorException();
        }

        if (lines.size() < 4) {
            throw new InvalidAsciiArmorException("not enough lines");
        }

        String startHeaderLine = lines.get(0).replace(HEADER_LINE_PREFIX, "").replace(HEADER_LINE_SUFFIX, "");
        String endHeaderLine = lines.get(lines.size() - 1).replace(LAST_LINE_PREFIX, "").replace(LAST_LINE_SUFFIX, "");
        if (!startHeaderLine.equals(endHeaderLine)) {
            throw new InvalidAsciiArmorException("start and end header lines do not match");
        }

        HeaderLine headerLine = HeaderLine.fromString(startHeaderLine);

        String b64Data = "";
        for (int i = dataStart; i < dataEnd; i++) {
            b64Data += lines.get(i).trim();
        }

        final byte[] data = Base64.decode(b64Data);
        final int computedCRC = CRC24.compute(data);

        String crcLine = lines.get(lines.size() - 2);
        byte[] crcData = Base64.decode(crcLine.replaceFirst("=", ""));
        if (crcData.length != 3) {
            throw new InvalidAsciiArmorException("crc wrong length");
        }
        ByteBuffer givenCRCBuf = ByteBuffer.allocate(4).put((byte) 0).put(crcData);
        givenCRCBuf.flip();
        int givenCRC = givenCRCBuf.getInt();

        if (givenCRC != computedCRC) {
            throw new InvalidAsciiArmorException("invalid CRC");
        }

        List<Pair<String, String>> headers = new LinkedList<>();
        for (int i = 1; i < dataStart - 1; i++) {
            String header = lines.get(i);
            String[] split = header.split(": ");
            if (split.length != 2) {
                throw new InvalidAsciiArmorException("invalid header");
            }
            headers.add(new Pair<>(split[0], split[1]));
        }

        return new AsciiArmor(
                headerLine,
                headers,
                data
        );
    }

    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append(HEADER_LINE_PREFIX).append(headerLine.s).append(HEADER_LINE_SUFFIX).append("\n");

        for (Pair<String, String> header: headers) {
            s.append(header.first).append(": ").append(header.second).append("\n");
        }
        if (headers.size() > 0) {
            s.append("\n");
        }


        String b64Data = Base64.encode(data);
        //  https://stackoverflow.com/questions/10530102/java-parse-string-and-add-line-break-every-100-characters
        String splitLines = b64Data.replaceAll("(.{64})", "$1\n").trim();
        s.append(splitLines).append("\n");

        s.append("=").append(Base64.encode(CRC24.computeBytes(data)));
        s.append("\n");

        s.append(LAST_LINE_PREFIX).append(headerLine.s).append(LAST_LINE_SUFFIX);

        return s.toString();
    }
}
