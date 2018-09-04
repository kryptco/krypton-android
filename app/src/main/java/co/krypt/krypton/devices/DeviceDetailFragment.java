package co.krypt.krypton.devices;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;
import java.util.UUID;

import co.krypt.krypton.R;
import co.krypt.krypton.analytics.Analytics;
import co.krypt.krypton.approval.ApprovalsFragment;
import co.krypt.krypton.pairing.DeviceType;
import co.krypt.krypton.pairing.Pairing;
import co.krypt.krypton.pairing.Pairings;
import co.krypt.krypton.policy.Approval;
import co.krypt.krypton.policy.Policy;
import co.krypt.krypton.silo.Silo;

public class DeviceDetailFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener, CompoundButton.OnCheckedChangeListener {

    private static final String ARG_PAIRING_UUID = "pairing-uuid";
    private int mColumnCount = 1;
    private String pairingUUID;

    private SignatureLogRecyclerViewAdapter signatureLogAdapter;
    private RadioButton manualButton;
    private RadioButton automaticButton;

    private TextView deviceName;
    private TextView originalNameLabel;

    private Button viewTemporaryApprovalsButton;
    private Button resetTemporaryApprovalsButton;
    private ViewGroup temporaryApprovalsContainer;

    private Button unpairButton;

    private BroadcastReceiver onDeviceLogReceiver;
    private Context attachedContext;

    private Dao.DaoObserver daoObserver = this::updateApprovalButtons;

    public DeviceDetailFragment() {
    }

    public static DeviceDetailFragment newInstance(String pairingUUID) {
        DeviceDetailFragment fragment = new DeviceDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PAIRING_UUID, pairingUUID);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            pairingUUID = getArguments().getString(ARG_PAIRING_UUID);
        }

        signatureLogAdapter = new SignatureLogRecyclerViewAdapter(
                Silo.shared(getContext()).pairings().getAllLogsTimeDescending(pairingUUID)
        );

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_device_detail, container, false);
        final Pairing pairing = Silo.shared(getContext()).pairings().getPairing(pairingUUID);
        if (pairing == null) {
            return view;
        }

        View deviceCardView;
        if (pairing.isBrowser() && pairing.deviceType == DeviceType.CHROME) {
            deviceCardView = createExtDeviceCardView(inflater, container, savedInstanceState, pairing);
        }
        else {
            deviceCardView = createCliDeviceCardView(inflater, container, savedInstanceState, pairing);
        }

        signatureLogAdapter.deviceCardView.set(deviceCardView);

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.list);
        Context context = recyclerView.getContext();
        if (mColumnCount <= 1) {
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
        } else {
            recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
        }
        recyclerView.setAdapter(signatureLogAdapter);


        onDeviceLogReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                signatureLogAdapter.setLogs(
                        Silo.shared(getContext()).pairings().getAllLogsTimeDescending(pairingUUID)
                );
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(Pairings.ON_DEVICE_LOG_ACTION);
        LocalBroadcastManager.getInstance(context).registerReceiver(onDeviceLogReceiver, filter);
        Silo.shared(getContext()).pairings().registerOnSharedPreferenceChangedListener(this);

        return view;
    }

    public View createCliDeviceCardView(LayoutInflater inflater, ViewGroup container,
                                Bundle savedInstanceState, Pairing pairing) {
        View deviceCardView = inflater.inflate(R.layout.cli_device_card, container, false);

        deviceName = (TextView) deviceCardView.findViewById(R.id.deviceName);
        deviceName.setOnEditorActionListener((v12, keyCode, event) -> {
            v12.clearFocus();
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v12.getWindowToken(), 0);
            onDeviceNameChanged(v12.getText().toString());
            return false;
        });
        deviceName.setOnFocusChangeListener((v1, hasFocus) -> {
            if (!hasFocus) {
                EditText editText = (EditText) v1;
                onDeviceNameChanged(editText.getText().toString());
            }
        });
        originalNameLabel = deviceCardView.findViewById(R.id.originalNameLabel);
        updateDisplayNameViews();

        AppCompatImageView deviceIcon = deviceCardView.findViewById(R.id.deviceIcon);
        deviceIcon.setImageResource(R.drawable.terminal_icon);

        manualButton = (RadioButton) deviceCardView.findViewById(R.id.alwaysAsk);
        automaticButton = (RadioButton) deviceCardView.findViewById(R.id.automaticApprovalButton);

        viewTemporaryApprovalsButton = deviceCardView.findViewById(R.id.temporaryApprovalsViewButton);
        viewTemporaryApprovalsButton.setOnClickListener(v -> {
            ApprovalsFragment f = ApprovalsFragment.newInstance(pairing.uuid);
            getFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.enter_from_right_fast, R.anim.exit_to_right_fast, R.anim.enter_from_right_fast, R.anim.exit_to_right_fast)
                    .addToBackStack(null)
                    .add(R.id.childFragmentContainer, f)
                    .commit();
        });
        resetTemporaryApprovalsButton = deviceCardView.findViewById(R.id.temporaryApprovalsResetButton);
        resetTemporaryApprovalsButton.setOnClickListener(v -> {
            try {
                Approval.deleteApprovalsForPairing(
                        Silo.shared(v.getContext()).pairings().dbHelper.getApprovalDao(),
                        pairing.uuid
                );
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        temporaryApprovalsContainer = deviceCardView.findViewById(R.id.temporaryApprovalsContainer);

        updateApprovalButtons();

        unpairButton = (Button) deviceCardView.findViewById(R.id.unpairButton);
        unpairButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Silo.shared(v.getContext()).unpair(pairing, true);
                new Analytics(getContext()).postEvent("device", "unpair", null, null, false);
                getFragmentManager().popBackStackImmediate();
            }
        });

        final SwitchCompat askUnknownHostsSwitch = (SwitchCompat) deviceCardView.findViewById(R.id.requireUnknownHostApprovalSwitch);
        askUnknownHostsSwitch.setChecked(new Pairings(getContext()).requireUnknownHostManualApproval(pairing));
        askUnknownHostsSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                new Pairings(getContext()).setRequireUnknownHostManualApproval(pairing, isChecked);
            }
        });
        return deviceCardView;
    }

    public View createExtDeviceCardView(LayoutInflater inflater, ViewGroup container,
                                Bundle savedInstanceState, Pairing pairing) {
        View deviceCardView = inflater.inflate(R.layout.ext_device_card, container, false);

        deviceName = (TextView) deviceCardView.findViewById(R.id.deviceName);
        deviceName.setOnEditorActionListener((v12, keyCode, event) -> {
            v12.clearFocus();
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v12.getWindowToken(), 0);
            onDeviceNameChanged(v12.getText().toString());
            return false;
        });
        deviceName.setOnFocusChangeListener((v1, hasFocus) -> {
            if (!hasFocus) {
                EditText editText = (EditText) v1;
                onDeviceNameChanged(editText.getText().toString());
            }
        });
        originalNameLabel = deviceCardView.findViewById(R.id.originalNameLabel);
        updateDisplayNameViews();

        AppCompatImageView deviceIcon = deviceCardView.findViewById(R.id.deviceIcon);
        if (pairing.deviceType == null) {
            deviceIcon.setImageResource(R.drawable.user_icon);
        } else switch (pairing.deviceType) {
            case FIREFOX:
                deviceIcon.setImageResource(R.drawable.firefox);
                break;
            case CHROME:
                deviceIcon.setImageResource(R.drawable.chrome);
                break;
            case SAFARI:
                deviceIcon.setImageResource(R.drawable.safari);
                break;
            default:
                deviceIcon.setImageResource(R.drawable.user_icon);
                break;
        }

        final SwitchCompat allowU2FZeroTouchSwitch = (SwitchCompat) deviceCardView.findViewById(R.id.allowU2FZeroTouchSwitch);
        allowU2FZeroTouchSwitch.setChecked(new Pairings(getContext()).getU2FZeroTouchAllowed(pairing));
        allowU2FZeroTouchSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                new Pairings(getContext()).setU2FZeroTouchAllowed(pairing.getUUIDString(), isChecked);
            }
        });

        unpairButton = (Button) deviceCardView.findViewById(R.id.unpairButton);
        unpairButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Silo.shared(v.getContext()).unpair(pairing, true);
                new Analytics(getContext()).postEvent("device", "unpair", null, null, false);
                getFragmentManager().popBackStackImmediate();
            }
        });

        return deviceCardView;
    }

    @Override
    public void onDestroyView() {
        Silo.shared(getContext()).pairings().unregisterOnSharedPreferenceChangedListener(this);
        if (attachedContext != null && onDeviceLogReceiver != null) {
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(onDeviceLogReceiver);
        }
        onDeviceLogReceiver = null;
        super.onDestroyView();
    }

    private synchronized void updateApprovalButtons() {
        new Handler(Looper.getMainLooper()).post(() -> {
            manualButton.setOnCheckedChangeListener(null);
            automaticButton.setOnCheckedChangeListener(null);
            Pairings pairings = Silo.shared(getContext()).pairings();
            if (pairings.getApproved(pairingUUID)) {
                automaticButton.setChecked(true);
                manualButton.setChecked(false);
            } else {
                manualButton.setChecked(true);
                automaticButton.setChecked(false);
            }
            manualButton.setOnCheckedChangeListener(this);
            automaticButton.setOnCheckedChangeListener(this);
        });

        if (attachedContext != null) {
            try {
                boolean hasApprovals = Approval.hasTemporaryApprovals(Silo.shared(attachedContext).pairings().dbHelper.getApprovalDao(), Policy.temporaryApprovalSeconds(getContext(), Approval.ApprovalType.SSH_USER_HOST), UUID.fromString(pairingUUID));
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (hasApprovals) {
                        for (int i = 0; i < temporaryApprovalsContainer.getChildCount(); i++) {
                            temporaryApprovalsContainer.getChildAt(i).setVisibility(View.VISIBLE);
                        }
                    } else {
                        //  views below do not adjust correctly if container is GONE
                        for (int i = 0; i < temporaryApprovalsContainer.getChildCount(); i++) {
                            temporaryApprovalsContainer.getChildAt(i).setVisibility(View.GONE);
                        }
                    }
                });
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateDisplayNameViews() {
        final Pairing pairing = Silo.shared(getContext()).pairings().getPairing(pairingUUID);
        if (pairing == null) {
            return;
        }
        deviceName.setText(pairing.getDisplayName());
        if (pairing.getDisplayName().equals(pairing.workstationName)) {
            originalNameLabel.setText("");
        }
        else {
            originalNameLabel.setText("Originally \"" + pairing.workstationName + "\"");
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        attachedContext = context;
        try {
            Silo.shared(context).pairings().dbHelper.getApprovalDao().registerObserver(daoObserver);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDetach() {
        try {
            Silo.shared(attachedContext).pairings().dbHelper.getApprovalDao().unregisterObserver(daoObserver);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        attachedContext = null;
        super.onDetach();
    }

    private void onDeviceNameChanged(String newName) {
        Silo.shared(getContext()).pairings().renamePairing(pairingUUID, newName);
        updateDisplayNameViews();
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
            if (buttonView == automaticButton) {
                pairings.setApproved(pairingUUID, true);
                analytics.postEvent("manual approval", String.valueOf(false), null, null, false);
            }
            updateApprovalButtons();
        }
    }
}
