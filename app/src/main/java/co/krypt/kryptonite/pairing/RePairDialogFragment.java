package co.krypt.kryptonite.pairing;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import co.krypt.kryptonite.R;
import co.krypt.kryptonite.silo.Silo;

public class RePairDialogFragment extends Fragment {
    private static final String TAG = "PairDialogFragment";

    private Button doneButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final RePairDialogFragment self = this;

        final View rootView = inflater.inflate(R.layout.fragment_re_pair_dialog, container, false);

        doneButton = ((Button)rootView.findViewById(R.id.doneButton));
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Silo.shared(self.getContext()).pairings().clearOldPairings();
                getActivity().getSupportFragmentManager().beginTransaction().hide(self).remove(self).commit();
            }
        });

        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }


    @Override
    public void onDetach() {
        super.onDetach();
    }
}
