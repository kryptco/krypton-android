package co.krypt.kryptonite.onboarding;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import co.krypt.kryptonite.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class GeneratingFragment extends Fragment {


    public GeneratingFragment() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_generating, container, false);
    }

}
