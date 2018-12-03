package co.krypt.krypton.crypto;

import android.content.Context;

import com.j256.ormlite.dao.Dao;

import org.greenrobot.eventbus.EventBus;

import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import co.krypt.krypton.db.OpenDatabaseHelper;
import co.krypt.krypton.exception.CryptoException;
import co.krypt.krypton.silo.IdentityService;
import co.krypt.krypton.totp.TOTPAccount;

public class TOTP {

    public static final int DEFAULT_PERIOD = 30;
    public static final int DEFAULT_OTP_LENGTH = 6;

    public static String generateOtp(byte[] k, int period, int n) throws CryptoException {
        try {
            long counter = (long) Math.floor( Math.floor(System.currentTimeMillis() / 1000) / period );
            byte[] data = ByteBuffer.allocate(8).putLong(counter).array();
            byte[] hash = hmacSha1(k, data);


            int offset = hash[hash.length - 1] & 0xf;

            int i = ((hash[offset] & 0x7f) << 24)
                    |((hash[offset + 1] & 0xff) << 16)
                    |((hash[offset + 2] & 0xff) << 8)
                    |(hash[offset + 3] & 0xff);

            int otp;
            System.out.println(i);
            if (n == 8) {
                otp = i % 100000000;
            } else {
                otp = i % 1000000;
            }
            String result = Integer.toString(otp);

            while(result.length() < n){
                result = "0" + result;
            }

            return result;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
            throw new CryptoException(e);
        }
    }

    private static byte[] hmacSha1(byte[] key, byte[] data) throws NoSuchAlgorithmException, InvalidKeyException{
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init( new SecretKeySpec(key, "HmacSHA1"));
        return mac.doFinal(data);
    }

    public static boolean checkAccountExists(Context context, String totpUri) throws CryptoException {
        List<TOTPAccount> accounts = getAccounts(context);
        for (TOTPAccount account : accounts) {
            if (account.uri.equals(totpUri)) {
                return true;
            }
        }
        return false;
    }

    public static void deleteTOTPAccount(Context context, TOTPAccount account) throws SQLException {
        Dao<TOTPAccount, String> db = new OpenDatabaseHelper(context).getTOTPAccountDao();
        db.delete(account);
        EventBus.getDefault().post(new IdentityService.TOTPAccountsUpdated());
    }

    public static List<TOTPAccount> getAccounts(Context context) throws CryptoException {
        synchronized (TOTP.class) {
            try {
                Dao<TOTPAccount, String> db = new OpenDatabaseHelper(context).getTOTPAccountDao();
                return db.queryForAll();
            } catch (NullPointerException | SQLException e) {
                e.printStackTrace();
                throw new CryptoException(e);
            }
        }
    }

    public static void registerTOTPAccount(Context context, String totpUri) throws SQLException, URISyntaxException {
        Dao<TOTPAccount, String> db = new OpenDatabaseHelper(context).getTOTPAccountDao();
        db.create(new TOTPAccount(totpUri));
        EventBus.getDefault().post(new IdentityService.TOTPAccountsUpdated());
    }
}
