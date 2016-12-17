package co.krypt.kryptonite.devices;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import co.krypt.kryptonite.R;
import co.krypt.kryptonite.log.SignatureLog;
import co.krypt.kryptonite.pairing.Pairing;
import co.krypt.kryptonite.silo.Silo;

public class DeviceDetailFragment extends Fragment {

    private static final String ARG_COLUMN_COUNT = "column-count";
    private static final String ARG_PAIRING_UUID = "pairing-uuid";
    private int mColumnCount = 1;
    private String pairingUUID;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public DeviceDetailFragment() {
    }

    @SuppressWarnings("unused")
    public static DeviceDetailFragment newInstance(int columnCount, String pairingUUID) {
        DeviceDetailFragment fragment = new DeviceDetailFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        args.putString(ARG_PAIRING_UUID, pairingUUID);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
            pairingUUID = getArguments().getString(ARG_PAIRING_UUID);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_device_detail, container, false);
        Pairing pairing = Silo.shared(getContext()).pairings().getPairing(pairingUUID);
        if (pairing == null) {
            return view;
        }

        TextView deviceName = (TextView) view.findViewById(R.id.deviceName);
        deviceName.setText(pairing.workstationName);

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.list);
        Context context = recyclerView.getContext();
        if (mColumnCount <= 1) {
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
        } else {
            recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
        }
        List<SignatureLog> signatureLogs = SignatureLog.sortByTimeDescending(
                Silo.shared(getContext()).pairings().getLogs(pairingUUID));
        recyclerView.setAdapter(new SignatureLogRecyclerViewAdapter(signatureLogs));

        return view;
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
