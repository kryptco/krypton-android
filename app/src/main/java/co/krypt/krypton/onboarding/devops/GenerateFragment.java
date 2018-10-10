package co.krypt.krypton.onboarding.devops;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.security.InvalidKeyException;

import co.krypt.krypton.R;
import co.krypt.krypton.analytics.Analytics;
import co.krypt.krypton.crypto.KeyManager;
import co.krypt.krypton.crypto.KeyType;
import co.krypt.krypton.crypto.SSHKeyPairI;
import co.krypt.krypton.crypto.U2F;
import co.krypt.krypton.exception.CryptoException;
import co.krypt.krypton.me.MeStorage;
import co.krypt.krypton.protocol.Profile;

/**
 * A simple {@link Fragment} subclass.
 */
public class GenerateFragment extends Fragment {
    private static final String TAG = "GenerateFragment";
    private static final String DEFAULT_KEY_TYPE_KEY = "default_key_type";
    private Button keyTypeButton;
    private String defaultKeyType = null;

    public GenerateFragment() {
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        defaultKeyType = args.getString(DEFAULT_KEY_TYPE_KEY);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_generate, container, false);
        Button nextButton = (Button) root.findViewById(R.id.generateButton);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                next();
            }
        });

        keyTypeButton = (Button) root.findViewById(R.id.keyTypeButton);
        keyTypeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (keyTypeButton.getText().equals(getString(R.string.rsa_key_type))) {
                    keyTypeButton.setText(R.string.ed25519_key_type);
                } else {
                    keyTypeButton.setText(R.string.rsa_key_type);
                }
            }
        });
        if (defaultKeyType != null) {
            keyTypeButton.setText(defaultKeyType);
        }
        return root;
    }

    private void next() {
        KeyType keyType = null;
        if (keyTypeButton.getText().equals(getString(R.string.ed25519_key_type))) {
            keyType = KeyType.Ed25519;
        } else if (keyTypeButton.getText().equals(getString(R.string.rsa_key_type))) {
            keyType = KeyType.RSA;
        } else {
            keyType = KeyType.RSA;
        }
        final KeyType finalKeyType = keyType;

        final FragmentActivity context = getActivity();
        final DevopsOnboardingProgress progress = new DevopsOnboardingProgress(getContext());
        progress.setStage(DevopsOnboardingStage.GENERATING);
        new Analytics(context).postEvent("onboard", "generate tapped", null, null, false);
        final long startMillis = System.currentTimeMillis();
        final GeneratingFragment generatingFragment = new GeneratingFragment();
        getActivity().getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left)
                .replace(R.id.activity_onboarding, generatingFragment).commit();
        final Fragment self = this;

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final long start = System.currentTimeMillis();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    SSHKeyPairI pair = KeyManager.loadOrGenerateKeyPair(context, finalKeyType, KeyManager.ME_TAG);
                    byte[] deviceIdentifier = new byte[]{};
                    try {
                        deviceIdentifier = U2F.loadOrGenerateDeviceIdentifier();
                    } catch (CryptoException e){
                        e.printStackTrace();
                    }

                    new MeStorage(context).set(new Profile(MeStorage.getDeviceName(), pair.publicKeySSHWireFormat(), deviceIdentifier, null, null));

                    final long genTime = System.currentTimeMillis() - start;
                    new Analytics(context).postEvent("keypair", "generate", null, (int) (genTime / 1000), false);
                    final long minAnimTime = 2000;
                    if (genTime < minAnimTime) {
                        try {
                            Thread.sleep(minAnimTime - genTime);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    generatingFragment.onGenerate();
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    progress.setStage(DevopsOnboardingStage.FIRST_PAIR_CLI);
                    final FragmentActivity activity = context;
                    if (activity != null && !activity.isDestroyed() && !activity.isFinishing()) {
                        FirstPairCliFragment firstPairCliFragment = new FirstPairCliFragment();
                        FragmentTransaction fragmentTransaction = activity.getSupportFragmentManager().beginTransaction();
                        fragmentTransaction
                                .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left)
                                .add(R.id.activity_onboarding, firstPairCliFragment)
                                .hide(generatingFragment).show(firstPairCliFragment).commitAllowingStateLoss();
                    }
                } catch (InvalidKeyException | IOException | CryptoException | UnsupportedOperationException | IllegalArgumentException e) {
                    e.printStackTrace();
                    progress.reset();
                    final FragmentActivity activity = context;
                    if (activity != null && !activity.isDestroyed() && !activity.isFinishing()) {
                        GenerateFragment generateFragment = new GenerateFragment();
                        if (finalKeyType == KeyType.RSA) {
                            Bundle args = new Bundle();
                            args.putString(DEFAULT_KEY_TYPE_KEY, activity.getString(R.string.ed25519_key_type));
                            generateFragment.setArguments(args);
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(activity, "Error generating rsa key, try again to generate an ed25519 key.", Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                        activity.getSupportFragmentManager().beginTransaction()
                                .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left)
                                .replace(R.id.activity_onboarding, generateFragment).commitAllowingStateLoss();
                    }
                }

            }
        }).start();
    }

}
