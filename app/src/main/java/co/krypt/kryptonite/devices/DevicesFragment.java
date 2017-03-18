package co.krypt.kryptonite.devices;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;

import co.krypt.kryptonite.MainActivity;
import co.krypt.kryptonite.R;
import co.krypt.kryptonite.pairing.Pairing;
import co.krypt.kryptonite.pairing.Session;
import co.krypt.kryptonite.silo.Silo;

public class DevicesFragment extends Fragment implements OnDeviceListInteractionListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String ARG_COLUMN_COUNT = "column-count";
    private int mColumnCount = 1;

    private View noPairedDevicesContainer = null;

    private DevicesRecyclerViewAdapter devicesAdapter;

    public DevicesFragment() {
    }

    @SuppressWarnings("unused")
    public static DevicesFragment newInstance(int columnCount) {
        DevicesFragment fragment = new DevicesFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        List<Session> sessions = new ArrayList<>(Silo.shared(getContext()).pairings().loadAllSessions());
        devicesAdapter = new DevicesRecyclerViewAdapter(sessions, this);

        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_devices_list, container, false);

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.list);
        // Set the adapter
        Context context = recyclerView.getContext();
        if (mColumnCount <= 1) {
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
        } else {
            recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
        }
        recyclerView.setAdapter(devicesAdapter);

        noPairedDevicesContainer = view.findViewById(R.id.devicesEmptyContainer);
        Button pairNewDeviceButton = (Button) view.findViewById(R.id.pairNewDeviceButton);
        pairNewDeviceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Activity activity = getActivity();
                if (activity instanceof MainActivity) {
                    ((MainActivity) activity).setActiveTab(MainActivity.PAIR_FRAGMENT_POSITION);
                }
            }
        });

        populateDevices();
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Silo.shared(getContext()).pairings().registerOnSharedPreferenceChangedListener(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Silo.shared(getContext()).pairings().unregisterOnSharedPreferenceChangedListener(this);
    }

    @Override
    public void onListFragmentInteraction(Pairing device) {
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction
                .setCustomAnimations(
                        R.anim.enter_from_right_fast, R.anim.exit_to_left,
                        R.anim.enter_from_left_fast, R.anim.exit_to_right_fast
                )
                .addToBackStack(null)
                .add(R.id.deviceDetail, DeviceDetailFragment.newInstance(1, device.getUUIDString()))
                .commit();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        populateDevices();
    }

    private void populateDevices() {
        List<Session> sessions = new ArrayList<>(Silo.shared(getContext()).pairings().loadAllSessions());
        devicesAdapter.setPairings(sessions);
        if (sessions != null && sessions.size() > 0) {
            noPairedDevicesContainer.setVisibility(View.GONE);
        } else {
            noPairedDevicesContainer.setVisibility(View.VISIBLE);
        }
    }

}
