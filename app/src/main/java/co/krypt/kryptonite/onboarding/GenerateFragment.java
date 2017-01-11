package co.krypt.kryptonite.onboarding;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.io.IOException;
import java.security.InvalidKeyException;

import co.krypt.kryptonite.R;
import co.krypt.kryptonite.analytics.Analytics;
import co.krypt.kryptonite.crypto.KeyManager;
import co.krypt.kryptonite.crypto.SSHKeyPair;
import co.krypt.kryptonite.exception.CryptoException;
import co.krypt.kryptonite.me.MeStorage;
import co.krypt.kryptonite.protocol.Profile;

/**
 * A simple {@link Fragment} subclass.
 */
public class GenerateFragment extends Fragment {
    private static final String TAG = "GenerateFragment";

    public GenerateFragment() {
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
        return root;
    }

    private void next() {
        final FragmentActivity context = getActivity();
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
                    SSHKeyPair pair = KeyManager.loadOrGenerateKeyPair(KeyManager.MY_RSA_KEY_TAG);
                    new MeStorage(context).set(new Profile("enter email", pair.publicKeySSHWireFormat()));

                    final long genTime = System.currentTimeMillis() - start;
                    new Analytics(context).postEvent("keypair", "generate", null, (int) (genTime / 1000), false);
                    if (genTime < 5000) {
                        try {
                            Thread.sleep(5000 - genTime);
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
                    EnterEmailFragment enterEmailFragment = new EnterEmailFragment();
                    context.getSupportFragmentManager().beginTransaction()
                            .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left)
                            .hide(generatingFragment)
                            .add(R.id.activity_onboarding, enterEmailFragment)
                            .show(enterEmailFragment)
                            .commit();
                } catch (InvalidKeyException | IOException | CryptoException e) {
                    e.printStackTrace();
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left)
                            .replace(R.id.activity_onboarding, new GenerateFragment()).commit();
                }

            }
        }).start();
    }

}
