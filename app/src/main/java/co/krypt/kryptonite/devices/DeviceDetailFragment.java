package co.krypt.kryptonite.devices;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;

import java.util.List;

import co.krypt.kryptonite.R;
import co.krypt.kryptonite.analytics.Analytics;
import co.krypt.kryptonite.log.SSHSignatureLog;
import co.krypt.kryptonite.pairing.Pairing;
import co.krypt.kryptonite.pairing.Pairings;
import co.krypt.kryptonite.silo.Silo;

public class DeviceDetailFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener, CompoundButton.OnCheckedChangeListener {

    private static final String ARG_COLUMN_COUNT = "column-count";
    private static final String ARG_PAIRING_UUID = "pairing-uuid";
    private int mColumnCount = 1;
    private String pairingUUID;

    private SignatureLogRecyclerViewAdapter signatureLogAdapter;
    private RadioButton manualButton;
    private RadioButton temporaryButton;
    private RadioButton automaticButton;

    private Button unpairButton;

    private BroadcastReceiver onDeviceLogReceiver;
    private Context attachedContext;

    public DeviceDetailFragment() {
    }

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

        List<SSHSignatureLog> sshSignatureLogs = SSHSignatureLog.sortByTimeDescending(
                Silo.shared(getContext()).pairings().getLogs(pairingUUID));
        signatureLogAdapter = new SignatureLogRecyclerViewAdapter(sshSignatureLogs);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_device_detail, container, false);
        final Pairing pairing = Silo.shared(getContext()).pairings().getPairing(pairingUUID);
        if (pairing == null) {
            return view;
        }

        TextView deviceName = (TextView) view.findViewById(R.id.deviceName);
        deviceName.setText(pairing.workstationName);

        manualButton = (RadioButton) view.findViewById(R.id.manualApprovalButton);
        temporaryButton = (RadioButton) view.findViewById(R.id.temporaryApprovalButton);
        automaticButton = (RadioButton) view.findViewById(R.id.automaticApprovalButton);

        updateApprovalButtons();

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.list);
        Context context = recyclerView.getContext();
        if (mColumnCount <= 1) {
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
        } else {
            recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
        }
        recyclerView.setAdapter(signatureLogAdapter);

        unpairButton = (Button) view.findViewById(R.id.unpairButton);
        unpairButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Silo.shared(v.getContext()).unpair(pairing, true);
                new Analytics(getContext()).postEvent("device", "unpair", null, null, false);
                getFragmentManager().popBackStackImmediate();
            }
        });

        final Switch askUnknownHostsSwitch = (Switch) view.findViewById(R.id.requireUnknownHostApprovalSwitch);
        askUnknownHostsSwitch.setChecked(new Pairings(getContext()).requireUnknownHostManualApproval(pairing));
        askUnknownHostsSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                new Pairings(getContext()).setRequireUnknownHostManualApproval(pairing, isChecked);
            }
        });

        onDeviceLogReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                List<SSHSignatureLog> sshSignatureLogs = SSHSignatureLog.sortByTimeDescending(
                        Silo.shared(getContext()).pairings().getLogs(pairingUUID));
                signatureLogAdapter.setLogs(sshSignatureLogs);
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(Pairings.ON_DEVICE_LOG_ACTION);
        context.registerReceiver(onDeviceLogReceiver, filter);
        Silo.shared(getContext()).pairings().registerOnSharedPreferenceChangedListener(this);

        return view;
    }

    @Override
    public void onDestroyView() {
        Silo.shared(getContext()).pairings().unregisterOnSharedPreferenceChangedListener(this);
        if (attachedContext != null && onDeviceLogReceiver != null) {
            attachedContext.unregisterReceiver(onDeviceLogReceiver);
        }
        onDeviceLogReceiver = null;
        super.onDestroyView();
    }

    private synchronized void updateApprovalButtons() {
        manualButton.setOnCheckedChangeListener(null);
        temporaryButton.setOnCheckedChangeListener(null);
        automaticButton.setOnCheckedChangeListener(null);
        Pairings pairings = Silo.shared(getContext()).pairings();
        temporaryButton.setText("Don't ask for 1 hour");
        if (pairings.getApproved(pairingUUID)) {
            automaticButton.setChecked(true);
            manualButton.setChecked(false);
            temporaryButton.setChecked(false);
        } else {
            Long approvedUntil = pairings.getApprovedUntil(pairingUUID);
            if (approvedUntil == null || (approvedUntil * 1000 < System.currentTimeMillis())) {
                manualButton.setChecked(true);
                automaticButton.setChecked(false);
                temporaryButton.setChecked(false);
            } else {
                temporaryButton.setChecked(true);
                automaticButton.setChecked(false);
                manualButton.setChecked(false);

                String temporaryApprovalText = "Ask " +
                        DateUtils.getRelativeTimeSpanString(approvedUntil * 1000, System.currentTimeMillis(), 1000)
                                .toString().toLowerCase();
                temporaryButton.setText(temporaryApprovalText);
            }
        }
        manualButton.setOnCheckedChangeListener(this);
        temporaryButton.setOnCheckedChangeListener(this);
        automaticButton.setOnCheckedChangeListener(this);
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        attachedContext = context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        attachedContext = null;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(Pairings.pairingApprovedKey(pairingUUID)) || key.equals(Pairings.pairingApprovedUntilKey(pairingUUID))) {
            updateApprovalButtons();
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            Pairings pairings = Silo.shared(getContext()).pairings();
            Analytics analytics = new Analytics(getContext());
            if (buttonView == manualButton) {
                pairings.setApproved(pairingUUID, false);
                analytics.postEvent("manual approval", String.valueOf(true), null, null, false);
            }
            if (buttonView == temporaryButton) {
                pairings.setApprovedUntil(pairingUUID, (System.currentTimeMillis() / 1000) + 3600);
                analytics.postEvent("manual approval", "time", null, 3600, false);
            }
            if (buttonView == automaticButton) {
                pairings.setApproved(pairingUUID, true);
                analytics.postEvent("manual approval", String.valueOf(false), null, null, false);
            }
            updateApprovalButtons();
        }
    }
}
