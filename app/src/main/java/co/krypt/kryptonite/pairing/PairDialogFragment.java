package co.krypt.kryptonite.pairing;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import co.krypt.kryptonite.protocol.PairingQR;

public class PairDialogFragment extends DialogFragment {
    private PairListener mListener;
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("Pair with device?")
                .setPositiveButton("Pair", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (mListener != null) {
                            mListener.pair();
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (mListener != null) {
                            mListener.cancel();
                        }
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof PairListener) {
            mListener = (PairListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement PairListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface PairListener {
        void pair();
        void cancel();
    }
}
