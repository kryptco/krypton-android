package co.krypt.kryptonite.onboarding;


import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.AppCompatImageView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.docuverse.identicon.IdenticonUtil;
import com.docuverse.identicon.NineBlockIdenticonRenderer;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;

import co.krypt.kryptonite.R;
import co.krypt.kryptonite.crypto.KeyManager;
import co.krypt.kryptonite.crypto.SHA256;
import co.krypt.kryptonite.crypto.SSHKeyPair;
import co.krypt.kryptonite.exception.CryptoException;
import co.krypt.kryptonite.uiutils.MLRoundedImageView;

/**
 * A simple {@link Fragment} subclass.
 */
public class EnterEmailFragment extends Fragment {


    public EnterEmailFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_enter_email, container, false);
        Button nextButton = (Button) root.findViewById(R.id.nextButton);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                next();
            }
        });
        MLRoundedImageView identiconImage = (MLRoundedImageView) root.findViewById(R.id.identicon);
        try {
            NineBlockIdenticonRenderer renderer = new NineBlockIdenticonRenderer();
            renderer.setBackgroundColor(Color.TRANSPARENT);
            renderer.setPatchSize(80);
            BigInteger hash = new BigInteger(SHA256.digest(new byte[]{0}));
            Bitmap identicon = renderer.render(hash, 2000);
            identiconImage.setImageBitmap(identicon);
            SSHKeyPair keyPair = KeyManager.loadOrGenerateKeyPair(KeyManager.MY_RSA_KEY_TAG);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return root;
    }

    private void next() {
        FragmentTransaction fragmentTransaction = getActivity().getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.activity_onboarding, new FirstPairFragment()).commit();
    }

}
