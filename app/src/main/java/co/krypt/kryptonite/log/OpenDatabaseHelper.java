package co.krypt.kryptonite.log;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;


/**
 * Created by Kevin King on 3/28/17.
 * Copyright 2016. KryptCo, Inc.
 */

public class OpenDatabaseHelper extends OrmLiteSqliteOpenHelper {

        private static final String DATABASE_NAME = "kryptonite";
        private static final int DATABASE_VERSION = 1;

        /**
         * The data access object used to interact with the Sqlite database to do C.R.U.D operations.
         */
        private Dao<SignatureLog, Long> signatureLogDao;

        public OpenDatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION) ;
        }

        @Override
        public void onCreate(SQLiteDatabase database, ConnectionSource connectionSource) {
            try {
                TableUtils.createTable(connectionSource, SignatureLog.class);

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase database, ConnectionSource connectionSource,
                              int oldVersion, int newVersion) {
            try {
                /**
                 * Recreates the database when onUpgrade is called by the framework
                 */
                TableUtils.dropTable(connectionSource, SignatureLog.class, false);
                onCreate(database, connectionSource);

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        /**
         * Returns an instance of the data access object
         * @return
         * @throws SQLException
         */
        public Dao<SignatureLog, Long> getDao() throws SQLException {
            if(signatureLogDao == null) {
                signatureLogDao = getDao(SignatureLog.class);
            }
            return signatureLogDao;
        }
}
