package co.krypt.krypton.crypto;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.crypto.tink.subtle.Bytes;
import com.google.crypto.tink.subtle.EllipticCurves;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.UpdateBuilder;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.greenrobot.eventbus.EventBus;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

import javax.security.auth.x500.X500Principal;

import co.krypt.krypton.db.OpenDatabaseHelper;
import co.krypt.krypton.exception.CryptoException;
import co.krypt.krypton.protocol.U2FAuthenticateRequest;
import co.krypt.krypton.protocol.U2FAuthenticateResponse;
import co.krypt.krypton.protocol.U2FRegisterRequest;
import co.krypt.krypton.protocol.U2FRegisterResponse;
import co.krypt.krypton.silo.IdentityService;
import co.krypt.krypton.u2f.KnownAppIds;
import co.krypt.krypton.u2f.RegisteredAccount;

/**
 * Created by Kevin King on 5/23/18.
 * Copyright 2018. KryptCo, Inc.
 */
public class U2F {
    private static final String LOG_TAG = "U2F";
    private static final String U2F_ACCOUNT_ALIAS_PREFIX = "U2F.ACCOUNT.";

    private static byte[] KRYPTON_U2F_MAGIC = new byte[]{
            (byte) 0x2c, (byte) 0xe5, (byte) 0xc8, (byte) 0xdf, (byte) 0x17, (byte) 0xe2, (byte) 0x2e, (byte) 0xf2,
            (byte) 0x0f, (byte) 0xd3, (byte) 0x83, (byte) 0x03, (byte) 0xfd, (byte) 0x2d, (byte) 0x99, (byte) 0x98,
    };

    private static final String DEVICE_IDENTIFIER_TAG = "U2F.DEVICE_IDENTIFIER";

    public static synchronized byte[] loadOrGenerateDeviceIdentifier() throws CryptoException {
        KeyStore.PrivateKeyEntry sk = KeyManager.loadOrGenerateKeyPair(DEVICE_IDENTIFIER_TAG, DEVICE_IDENTIFIER_TAG);
        return SHA256.digest(rawPublicKey(sk));
    }

    public static List<KeyManager.Account> getAccounts(Context context) throws CryptoException {
        List<KeyManager.Account> accounts = KeyManager.getAccounts(context);
        Collections.sort(accounts, (a,b) -> {
            if (b.added != null) {
                return b.added.compareTo(a.added);
            }
            return a.name.compareTo(b.name);
        });
        return accounts;
    }

    public static class KeyManager {
        public static class Account {
            public Account(String name, int logo, boolean secured, @Nullable Date added, @Nullable Date lastUsed, String keyHandleHash, @Nullable String shortName) {
                this.name = name;
                this.logo = logo;
                this.secured = secured;
                this.added = added;
                this.lastUsed = lastUsed;
                this.keyHandleHash = keyHandleHash;
                this.shortName = shortName;
            }

            public final String name;
            public final int logo;
            public final boolean secured;
            @Nullable public final Date added;
            @Nullable public final Date lastUsed;
            public final String keyHandleHash;
            @Nullable public final String shortName;
        }

        private static void migrateAccountsToDbOnce(Context context) throws SQLException, CryptoException, CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException {
            final String MIGRATED_KEY = "U2F.MIGRATED_ACCOUNTS_TO_DB";

            SharedPreferences prefs = u2fPrefs(context);
            if (!prefs.getBoolean(MIGRATED_KEY, false)) {
                Dao<RegisteredAccount, String> db = new OpenDatabaseHelper(context).getRegisteredAccountDao();

                KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
                keyStore.load(null);
                Enumeration<String> storedAliases = keyStore.aliases();


                final CertificateFactory certFactory;

                certFactory = CertificateFactory.getInstance("X509");
                if (certFactory == null) {
                    return;
                }

                while (storedAliases.hasMoreElements()) {
                    String alias = storedAliases.nextElement();
                    if (alias.startsWith(U2F_ACCOUNT_ALIAS_PREFIX)) {
                        KeyStore.PrivateKeyEntry kp = loadKeyPair(alias);

                        final X509Certificate x509Cert = (X509Certificate) certFactory.generateCertificate(new ByteArrayInputStream(kp.getCertificate().getEncoded()));
                        //  extract CN=<appid>
                        String appId = x509Cert.getSubjectX500Principal().getName().substring(3);
                        String keyHandleHash = alias.substring(U2F_ACCOUNT_ALIAS_PREFIX.length());
                        try {
                            db.create(new RegisteredAccount(
                                    keyHandleHash,
                                    appId,
                                    x509Cert.getNotBefore().getTime() / 1000
                            ));
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                }

                prefs.edit().putBoolean(MIGRATED_KEY, true).commit();
            }
        }

        public static List<Account> getAccounts(Context context) throws CryptoException {
            synchronized (U2F.class) {
                try {
                    try {
                        migrateAccountsToDbOnce(context);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    List<Account> accounts = new ArrayList<>();

                    Dao<RegisteredAccount, String> db = new OpenDatabaseHelper(context).getRegisteredAccountDao();

                    for (RegisteredAccount registeredAccount: db.queryForAll()) {
                        Date added = new Date(registeredAccount.added * 1000);
                        accounts.add(
                                new Account(
                                       KnownAppIds.displayAppId(registeredAccount.appId),
                                        KnownAppIds.displayAppLogo(registeredAccount.appId),
                                        true,
                                        added,
                                        registeredAccount.lastUsed != null ? new Date(registeredAccount.lastUsed * 1000) : null,
                                        registeredAccount.keyHandleHash,
                                        KnownAppIds.shortName(registeredAccount.appId)
                                )
                        );
                    }
                    return accounts;
                } catch (NullPointerException | NoSuchAlgorithmException | KeyStoreException | CertificateException | IOException | SQLException e) {
                    e.printStackTrace();
                    throw new CryptoException(e);
                }
            }
        }

        public static KeyPair loadAccountKeyPair(Context context, byte[] keyHandle) throws CryptoException {
            return new KeyPair(
                    context,
                    loadKeyPair(keyHandleToTag(keyHandle)),
                    keyHandle);
        }

        public static KeyPair generateAccountKeyPair(Context context, String appId) throws CryptoException {
            byte[] keyHandle = newKeyHandle(appId);
            KeyPair kp = new KeyPair(
                    context,
                    loadOrGenerateKeyPair(keyHandleToTag(keyHandle), appId),
                    keyHandle
            );
            EventBus.getDefault().post(new IdentityService.AccountsUpdated());
            return kp;
        }

        private static byte[] newKeyHandle(String appId) throws CryptoException {
            return formatKeyHandle(loadOrGenerateDeviceIdentifier(), SecureRandom.getSeed(32), appId);
        }

        private static String keyHandleToTag(byte[] keyHandle) throws CryptoException {
            return U2F_ACCOUNT_ALIAS_PREFIX + Base64.encode(SHA256.digest(keyHandle));
        }

        // KeyHandle: 80 bytes
        // M + R + H(H(D) + H(S) + H(R))
        // where
        // M = [16 Magic Bytes]
        // R = [32 bytes of random]
        // S = appId or rpId
        // D = device_identifier
        // H = SHA-256
        public static byte[] formatKeyHandle(byte[] d, byte[] r, String s) throws CryptoException {
            try {
                ByteArrayOutputStream innerHash = new ByteArrayOutputStream();
                innerHash.write(SHA256.digest(d));
                innerHash.write(SHA256.digest(s.getBytes()));
                innerHash.write(SHA256.digest(r));
                innerHash.close();

                ByteArrayOutputStream keyHandleOut = new ByteArrayOutputStream();
                keyHandleOut.write(KRYPTON_U2F_MAGIC);
                keyHandleOut.write(r);
                keyHandleOut.write(SHA256.digest(innerHash.toByteArray()));
                keyHandleOut.close();

                return keyHandleOut.toByteArray();
            } catch (IOException e) {
                throw new CryptoException(e);
            }
        }

        private static KeyStore.PrivateKeyEntry loadKeyPair(String tag) throws CryptoException {
            try {
                KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
                keyStore.load(null);
                KeyStore.Entry privateKeyEntry;
                privateKeyEntry = keyStore.getEntry(tag, null);

                if (privateKeyEntry instanceof KeyStore.PrivateKeyEntry) {
                    return (KeyStore.PrivateKeyEntry) privateKeyEntry;
                } else {
                    throw new CryptoException("key not found");
                }
            } catch (NullPointerException | NoSuchAlgorithmException | KeyStoreException | UnrecoverableEntryException | CertificateException | IOException e) {
                e.printStackTrace();
                throw new CryptoException(e);
            }
        }

        private static KeyStore.PrivateKeyEntry loadOrGenerateKeyPair(String tag, String commonName) throws CryptoException {
            try {
                KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
                keyStore.load(null);
                KeyStore.Entry privateKeyEntry;
                privateKeyEntry = keyStore.getEntry(tag, null);

                if (privateKeyEntry instanceof KeyStore.PrivateKeyEntry) {
                    //  return existing if present

                    return (KeyStore.PrivateKeyEntry) privateKeyEntry;
                } else {
                    Log.w(LOG_TAG, "Not an instance of a PrivateKeyEntry... generating new key");
                }


                KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore");
                keyPairGenerator.initialize(
                        new KeyGenParameterSpec.Builder(
                                tag, KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                                .setDigests(KeyProperties.DIGEST_SHA1, KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                                .setUserAuthenticationRequired(false)
                                .setCertificateNotBefore(new Date())
                                .setCertificateSubject(new X500Principal("CN=" + commonName))
                                .build());
                long genStart = System.currentTimeMillis();
                keyPairGenerator.generateKeyPair();
                long genStop = System.currentTimeMillis();

                privateKeyEntry = keyStore.getEntry(tag, null);

                Log.i(LOG_TAG, "KeyGen took " + String.valueOf((genStop - genStart)));
                return (KeyStore.PrivateKeyEntry) privateKeyEntry;
            } catch (InvalidAlgorithmParameterException | NoSuchProviderException | NullPointerException | NoSuchAlgorithmException | KeyStoreException | UnrecoverableEntryException | CertificateException | IOException e) {
                e.printStackTrace();
                throw new CryptoException(e);
            }
        }
    }

    public static boolean keyHandleMatches(byte[] keyHandle, String appId) throws CryptoException {
        byte[] r = Arrays.copyOfRange(keyHandle, KRYPTON_U2F_MAGIC.length, KRYPTON_U2F_MAGIC.length + 32);
        byte[] correctKeyHandle = KeyManager.formatKeyHandle(loadOrGenerateDeviceIdentifier(), r, appId);
        return Bytes.equal(correctKeyHandle, keyHandle);
    }



    //  synchronize access with U2F.class
    private static SharedPreferences u2fPrefs(Context context) {
        return context.getSharedPreferences("U2F_SHARED_PREFERENCES", Context.MODE_PRIVATE);
    }

    public static class KeyPair {
        private final SharedPreferences prefs;
        final KeyStore.PrivateKeyEntry keyPair;
        final byte[] keyHandle;
        final Context context;

        public KeyPair(Context context, KeyStore.PrivateKeyEntry keyPair, byte[] keyHandle) {
            prefs = u2fPrefs(context);
            this.keyPair = keyPair;
            this.keyHandle = keyHandle;
            this.context = context;
        }

        private long getAndIncrementCounter() throws CryptoException {
            synchronized(U2F.class) {
                String counterKey = Base64.encode(SHA256.digest(keyHandle)) + ".COUNTER";
                long current = prefs.getLong(counterKey, 1);
                prefs.edit().putLong(counterKey, current + 1).commit();
                return current;
            }
        }

        X509Certificate attestationCertificate() throws CryptoException {
            try {
                //https://stackoverflow.com/questions/29852290/self-signed-x509-certificate-with-bouncy-castle-in-java
                long now = System.currentTimeMillis();
                Date startDate = new Date(now);

                X500Name subject = new X500NameBuilder(BCStyle.INSTANCE)
                        .addRDN(BCStyle.CN, "Krypton Key")
                        .build();
                X500Principal commonName = new X500Principal(subject.getEncoded("DER"));
                BigInteger certSerialNumber = new BigInteger(Long.toString(0));

                Calendar calendar = Calendar.getInstance();
                calendar.setTime(startDate);
                calendar.add(Calendar.YEAR, 10);

                Date endDate = calendar.getTime();

                String signatureAlgorithm = "SHA256WithECDSA";

                ContentSigner contentSigner = new JcaContentSignerBuilder(signatureAlgorithm).build(keyPair.getPrivateKey());

                JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(commonName, certSerialNumber, startDate, endDate, commonName, keyPair.getCertificate().getPublicKey());

                BasicConstraints basicConstraints = new BasicConstraints(false);
                certBuilder.addExtension(new ASN1ObjectIdentifier("2.5.29.19"), false, basicConstraints);

                return new JcaX509CertificateConverter().setProvider(new BouncyCastleProvider()).getCertificate(certBuilder.build(contentSigner));
            } catch (OperatorCreationException | CertificateException |IOException e) {
                e.printStackTrace();
                throw new CryptoException(e);
            }
        }

        byte[] signDigest(byte[] payload) throws CryptoException {
            try {
                Signature signer = Signature.getInstance("SHA256withECDSA");
                signer.initSign(keyPair.getPrivateKey());
                signer.update(payload);
                return signer.sign();
            } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
                throw new CryptoException(e);
            }
        }

        public U2FRegisterResponse signU2FRegisterRequest(U2FRegisterRequest u2fRegisterRequest) throws CryptoException {
            ByteArrayOutputStream payloadOutput = new ByteArrayOutputStream();
            DataOutputStream payload = new DataOutputStream(payloadOutput);
            try {

                X509Certificate attestationCert = attestationCertificate();

                payload.writeByte(0x00);
                payload.write(SHA256.digest(u2fRegisterRequest.appId.getBytes()));
                payload.write(u2fRegisterRequest.challenge);
                payload.write(keyHandle);

                byte[] rawKeyBytes = rawPublicKey(this.keyPair);
                payload.write(rawKeyBytes);

                payload.close();
                byte[] toSignData = payloadOutput.toByteArray();

                byte[] signature = signDigest(toSignData);

                byte[] attestationCertificateBytes = attestationCert.getEncoded();

                try {
                    Dao<RegisteredAccount, String> db = new OpenDatabaseHelper(context).getRegisteredAccountDao();
                    db.create(
                            new RegisteredAccount(
                                    Base64.encode(SHA256.digest(keyHandle)),
                                    Base64.encode(keyHandle),
                                    u2fRegisterRequest.appId,
                                    System.currentTimeMillis() / 1000
                            ));
                    EventBus.getDefault().post(new IdentityService.AccountsUpdated());
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                return new U2FRegisterResponse(
                        rawKeyBytes,
                        attestationCertificateBytes,
                        keyHandle,
                        signature
                );
            } catch (IOException | GeneralSecurityException e) {
                throw new CryptoException(e);
            }
        }

        public U2FAuthenticateResponse signU2FAuthenticateRequest(U2FAuthenticateRequest u2FAuthenticateRequest) throws CryptoException {

            if (!keyHandleMatches(u2FAuthenticateRequest.keyHandle, u2FAuthenticateRequest.appId)) {
                throw new CryptoException("incorrect appId");
            }

            ByteArrayOutputStream payloadOutput = new ByteArrayOutputStream();
            DataOutputStream payload = new DataOutputStream(payloadOutput);
            try {
                long counter = getAndIncrementCounter();

                payload.write(SHA256.digest(u2FAuthenticateRequest.appId.getBytes()));
                payload.writeByte(0x01);
                payload.writeInt((int) counter);
                payload.write(u2FAuthenticateRequest.challenge);
                payload.close();

                byte[] toSignData = payloadOutput.toByteArray();

                byte[] signature = signDigest(toSignData);

                try {
                    Dao<RegisteredAccount, String> db = new OpenDatabaseHelper(context).getRegisteredAccountDao();
                    UpdateBuilder<RegisteredAccount, String> update = db.updateBuilder();
                    update.where().eq("key_handle", Base64.encode(u2FAuthenticateRequest.keyHandle));
                    update.updateColumnValue("last_used", System.currentTimeMillis() / 1000);
                    update.update();
                    EventBus.getDefault().post(new IdentityService.AccountsUpdated());
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                return new U2FAuthenticateResponse(
                        rawPublicKey(this.keyPair),
                        signature,
                        counter
                );
            } catch (IOException e) {
                throw new CryptoException(e);
            }
        }

    }

    static byte[] rawPublicKey(KeyStore.PrivateKeyEntry sk) throws CryptoException {
        ECPublicKey pk = (ECPublicKey) sk.getCertificate().getPublicKey();
        try {
            return EllipticCurves.pointEncode(EllipticCurves.CurveType.NIST_P256, EllipticCurves.PointFormatType.UNCOMPRESSED, pk.getW());
        } catch (GeneralSecurityException e) {
            throw new CryptoException(e);
        }
    }


}
