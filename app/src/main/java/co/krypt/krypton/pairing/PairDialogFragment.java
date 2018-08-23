package co.krypt.krypton.pairing;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import java.util.Arrays;
import java.util.HashSet;

import co.krypt.krypton.analytics.Analytics;
import co.krypt.krypton.onboarding.OnboardingActivity;
import co.krypt.krypton.policy.LocalAuthentication;
import co.krypt.krypton.protocol.PairingQR;
import co.krypt.krypton.silo.Silo;

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
        Dialog dialog = builder.create();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }
    private Dialog createPairingDialog(final PairFragment pairFragment) {
        final Analytics analytics = new Analytics(getActivity());

        final PairingQR incomingPairingQR = pairFragment.getPendingPairingQR();
        final String workstationName = incomingPairingQR.workstationName;
        AlertDialog.Builder builder = new AlertDialog.Builder(getTargetFragment().getContext());
        builder.setMessage("Pair with " + workstationName + "?")
                .setPositiveButton("Pair", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, int id) {
                        if (getTargetFragment() instanceof PairListener) {
                            Runnable onPair = new Runnable() {
                                @Override
                                public void run() {
                                    HashSet<Pairing> existingPairings = Silo.shared(getContext()).pairings().loadAll();
                                    for (Pairing existingPairing: existingPairings) {
                                        //  dedup by workstationDeviceIdentifier, falling back to workstation name
                                        if (incomingPairingQR.deviceId != null) {
                                            if (Arrays.equals(existingPairing.workstationDeviceIdentifier, incomingPairingQR.deviceId)) {
                                                Silo.shared(getContext()).pairings().unpair(existingPairing);
                                            }
                                        } else if (existingPairing.workstationName.equals(workstationName)) {
                                            Silo.shared(getContext()).pairings().unpair(existingPairing);
                                        }
                                    }

                                    final PairListener listener = (PairListener) getTargetFragment();
                                    analytics.postEvent("device", "pair", "new", null, false);
                                    listener.pair();
                                }
                            };
                            Activity activity = getActivity();
                            if (activity instanceof OnboardingActivity) {
                                onPair.run();
                            } else {
                                LocalAuthentication.requestAuthentication(
                                        getActivity(),
                                        "Pair Device Confirmation",
                                        "Pair with " + pairFragment.getPendingPairingQR().workstationName + "?\nThis device will be able to request logins.",
                                        onPair);
                            }
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (getTargetFragment() instanceof PairListener) {
                            final PairListener listener = (PairListener) getTargetFragment();
                            analytics.postEvent("device", "pair", "reject", null, false);
                            listener.cancel();
                        }
                    }
                });
        Dialog dialog = builder.create();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
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
