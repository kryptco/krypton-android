package co.krypt.kryptonite.log;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.amazonaws.util.Base32;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.List;

/**
 * Created by Kevin King on 3/26/17.
 * Copyright 2016. KryptCo, Inc.
 */

public class AuditLogContentProvider extends ContentProvider {
    public static final String AUTHORITY = "co.krypt.kryptonite.log.AuditLogContentProvider";

    private static String token = null;
    private final static String AUDIT_LOG_FILE_NAME = "audit_log.json";

    public static synchronized void setToken(String token) {
        AuditLogContentProvider.token = token;
    }

    public static synchronized String getToken() {
        return AuditLogContentProvider.token;
    }

    public static synchronized Uri getAuditLogURIWithToken() {
        if (token == null) {
            return null;
        }
        return Uri.parse("content://" + AUTHORITY + "/" + token + "/" + AUDIT_LOG_FILE_NAME);
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        return null;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Nullable
    @Override
    public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode) throws FileNotFoundException {
        UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, "*/*", 1);

        switch (uriMatcher.match(uri)) {
            case 1:
                List<String> segments = uri.getPathSegments();
                Log.i("AuditLogContentProvider", "segments: " + segments.toString());
                if (segments.size() < 2) {
                    return null;
                }
                String token = segments.get(segments.size() - 2);
                String lastToken = AuditLogContentProvider.getToken();

                if (lastToken != null && token.equals(lastToken)) {
                    try {
                        Log.i("AuditLogContentProvider", "exporting audit log");
                        return ParcelFileDescriptor.open(getContext().getFileStreamPath(AUDIT_LOG_FILE_NAME), ParcelFileDescriptor.MODE_READ_ONLY);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
        }
        return null;
    }

    public static String writeAuditLogReturningToken(Context context, String auditLogJson) throws IOException {
        FileOutputStream outputStream = context.openFileOutput(AUDIT_LOG_FILE_NAME, Context.MODE_PRIVATE);
        outputStream.write(auditLogJson.getBytes());
        outputStream.close();

        String token = Base32.encodeAsString(SecureRandom.getSeed(16));
        setToken(token);
        return token;
    }
}
