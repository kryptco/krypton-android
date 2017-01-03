package co.krypt.kryptonite.onboarding;


import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.support.v4.app.Fragment;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.security.InvalidKeyException;

import co.krypt.kryptonite.R;
import co.krypt.kryptonite.crypto.KeyManager;
import co.krypt.kryptonite.crypto.SSHKeyPair;
import co.krypt.kryptonite.exception.CryptoException;
import co.krypt.kryptonite.me.MeStorage;
import co.krypt.kryptonite.protocol.Profile;

/**
 * A simple {@link Fragment} subclass.
 */
public class GenerateFragment extends Fragment {


    public GenerateFragment() {
        // Required empty public constructor
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
        new Thread(new Runnable() {
            @Override
            public void run() {
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.activity_onboarding, new GeneratingFragment()).commit();
                try {
                    SSHKeyPair pair = KeyManager.loadOrGenerateKeyPair(KeyManager.MY_RSA_KEY_TAG);
                    new MeStorage(context).set(new Profile("enter email", pair.publicKeySSHWireFormat()));

                    context.getSupportFragmentManager().beginTransaction()
                            .replace(R.id.activity_onboarding, new EnterEmailFragment()).commit();
                } catch (CryptoException | InvalidKeyException | IOException e) {
                    context.getSupportFragmentManager().beginTransaction()
                            .replace(R.id.activity_onboarding, new GenerateFragment()).commit();
                    e.printStackTrace();
                }
            }
        }).start();
    }

}
