package co.krypt.kryptonite.pgp.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.MalformedInputException;

import co.krypt.kryptonite.pgp.UserID;

/**
 * Created by Kevin King on 6/12/17.
 * Copyright 2017. KryptCo, Inc.
 */

public class UserIDPacket extends Serializable implements Signable {
    public final PacketHeader header;
    public final UserID userID;

    public UserIDPacket(PacketHeader header, UserID userID) {
        this.header = header;
        this.userID = userID;
    }

    public static UserIDPacket parse(PacketHeader header, DataInputStream in) throws IOException, InvalidUTF8Exception {
        byte[] utf8 = new byte[(int) header.length];
        in.readFully(utf8);
        try {
            String userID = Charset.forName("UTF-8").newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(utf8)).toString();
            return new UserIDPacket(
                    header,
                    UserID.parse(userID)
            );
        } catch (MalformedInputException e) {
            throw new InvalidUTF8Exception(e.getMessage(), e);
        }
    }

    public static UserIDPacket fromUserID(UserID userID) {
        return new UserIDPacket(
                PacketHeader.withTypeAndLength(PacketType.USER_ID, userID.utf8().length),
                userID
        );
    }

    @Override
    public void serialize(DataOutputStream out) throws IOException {
        header.serialize(out);
        out.write(userID.utf8());
    }

    @Override
    public void writeSignableData(DataOutputStream out) throws IOException {
        out.writeInt(userID.utf8().length);
        out.write(userID.utf8());
    }
}
