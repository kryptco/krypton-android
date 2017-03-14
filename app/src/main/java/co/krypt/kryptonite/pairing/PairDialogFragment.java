package co.krypt.kryptonite.pairing;

import android.app.Activity;
import android.app.Dialog;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import co.krypt.kryptonite.MainActivity;
import co.krypt.kryptonite.analytics.Analytics;
import co.krypt.kryptonite.onboarding.OnboardingActivity;
import co.krypt.kryptonite.policy.LocalAuthentication;
import co.krypt.kryptonite.protocol.PairingQR;

public class PairDialogFragment extends DialogFragment {
    private static final String TAG = "PairDialogFragment";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (!(getTargetFragment() instanceof PairFragment)) {
            Log.e(TAG, "targetFragment !instanceof PairFragment");
            return null;
        }
        final PairFragment pairFragment = (PairFragment) getTargetFragment();
        final PairingQR pendingPairingQR = pairFragment.getPendingPairingQR();
        if (pendingPairingQR == null) {
            Log.e(TAG, "pendingPairingQR null");
            return null;
        }
        if (pendingPairingQR.version == null) {
            return createOutdatedPairingDialog(pairFragment);
        }
        // Create the AlertDialog object and return it
        return createPairingDialog(pairFragment);
    }

    private Dialog createOutdatedPairingDialog(final PairFragment pairFragment) {
        final Analytics analytics = new Analytics(getActivity());

        AlertDialog.Builder builder = new AlertDialog.Builder(getTargetFragment().getContext());
        builder.setMessage(pairFragment.getPendingPairingQR().workstationName + " is running an old version of kr. Please update kr by running \"curl https://krypt.co/kr-beta | sh\" and pair again.")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, int id) {
                        if (getTargetFragment() instanceof PairListener) {
                            final PairListener listener = (PairListener) getTargetFragment();
                            listener.cancel();
                        }
                    }
                });
        return builder.create();
    }
    private Dialog createPairingDialog(final PairFragment pairFragment) {
        final Analytics analytics = new Analytics(getActivity());

        AlertDialog.Builder builder = new AlertDialog.Builder(getTargetFragment().getContext());
        builder.setMessage("Pair with " + pairFragment.getPendingPairingQR().workstationName + "?")
                .setPositiveButton("Pair", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, int id) {
                        if (getTargetFragment() instanceof PairListener) {
                            Runnable onPair =
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            final PairListener listener = (PairListener) getTargetFragment();
                                            new Thread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    //TODO: detect existing pairings
                                                    analytics.postEvent("device", "pair", "new", null, false);
                                                    listener.pair();
                                                }
                                            }).start();
                                        }
                                    };
                            Activity activity = getActivity();
                            if (activity instanceof OnboardingActivity) {
                                onPair.run();
                            } else {
                                LocalAuthentication.requestAuthentication(
                                        getActivity(),
                                        "Pair Device Confirmation",
                                        "Pair with " + pairFragment.getPendingPairingQR().workstationName + "?\nThis device will be able to request SSH operations.",
                                        onPair);
                            }
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (getTargetFragment() instanceof PairListener) {
                            final PairListener listener = (PairListener) getTargetFragment();
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    analytics.postEvent("device", "pair", "reject", null, false);
                                    listener.cancel();
                                }
                            }).start();
                        }
                    }
                });
        return builder.create();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    public interface PairListener {
        void pair();
        void cancel();
    }
}
