package co.krypt.krypton.protocol;

/**
 * Created by Kevin King on 12/3/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class UnpairRequest extends RequestBody {
    public static final String FIELD_NAME = "unpair_request";

    @Override
    public <T, E extends Throwable> T visit(Visitor<T, E> visitor) throws E {
        return visitor.visit(this);
    }
}
