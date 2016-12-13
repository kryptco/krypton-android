package co.krypt.kryptonite.pairing;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;

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

        AlertDialog.Builder builder = new AlertDialog.Builder(getTargetFragment().getContext());
        builder.setMessage("Pair with " + pairFragment.getPendingPairingQR().workstationName + "?")
                .setPositiveButton("Pair", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (getTargetFragment() instanceof PairListener) {
                            final PairListener listener = (PairListener) getTargetFragment();
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    listener.pair();
                                }
                            }).start();
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
                                    listener.cancel();
                                }
                            }).start();
                        }
                    }
                });
        // Create the AlertDialog object and return it
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
