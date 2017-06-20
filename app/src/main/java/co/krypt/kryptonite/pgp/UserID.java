package co.krypt.kryptonite.pgp;

import java.nio.charset.Charset;

/**
 * Created by Kevin King on 6/13/17.
 * Copyright 2016. KryptCo, Inc.
 */

public class UserID {
    final String contents;
    final String name;
    final String email;

    public UserID(String name, String email) {
        this.name = name;
        this.email = email;
        this.contents = name + " <" + email + ">";
    }

    public byte[] utf8() {
        return contents.getBytes(Charset.forName("UTF-8"));
    }

    public static UserID parse(String contents) {
        if (contents.contains("<") && contents.contains(">") && contents.indexOf('<') < contents.indexOf('>')) {
            return new UserID(
                    contents.substring(0, contents.indexOf('<')).trim(),
                    contents.substring(contents.indexOf('<') + 1, contents.indexOf('>'))
                    );
        } else {
            return new UserID(contents, "");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserID userID = (UserID) o;

        if (!contents.equals(userID.contents)) return false;
        if (!name.equals(userID.name)) return false;
        return email.equals(userID.email);

    }

    @Override
    public int hashCode() {
        int result = contents.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + email.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return contents;
    }
}
