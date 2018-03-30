package co.krypt.krypton.approval;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import co.krypt.krypton.R;
import co.krypt.krypton.pairing.Pairing;
import co.krypt.krypton.policy.Approval;
import co.krypt.krypton.policy.Policy;
import co.krypt.krypton.silo.Silo;

/**
 * Created by Kevin King on 12/21/17.
 * Copyright 2017. KryptCo, Inc.
 */

public class ApprovalsFragment extends Fragment{

    private ApprovalRecyclerViewAdapter approvedRequestTypesAdapter = new ApprovalRecyclerViewAdapter(new ArrayList<>());
    private LinearLayout approvedRequestTypesContainer;

    private ApprovalRecyclerViewAdapter approvedSSHHostsAdapter = new ApprovalRecyclerViewAdapter(new ArrayList<>());
    private LinearLayout approvedSSHHostsContainer;

    private UUID pairingUUID;

    public static ApprovalsFragment newInstance(UUID pairingUUID) {
        ApprovalsFragment f = new ApprovalsFragment();

        Bundle args = new Bundle();
        args.putString("pairingUUID", pairingUUID.toString());
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            pairingUUID = UUID.fromString(getArguments().getString("pairingUUID"));
        }
    }

    private Dao<Approval, Long> getApprovalDao() throws SQLException {
        return Silo.shared(getContext()).pairings().dbHelper.getApprovalDao();
    }

    private Dao.DaoObserver observer = new Dao.DaoObserver() {
        @Override
        public void onChange() {
            refreshAdapters();
        }
    };

    private void refreshAdapters() {
        try {
            List<Approval> approvedRequestTypes = Approval.getRequestTypeApprovals(getApprovalDao(), Policy.temporaryApprovalSeconds(getContext(), Approval.ApprovalType.SSH_USER_HOST), pairingUUID);
            approvedRequestTypesAdapter.setItems(approvedRequestTypes);

            List<Approval> approvedSSHHosts = Approval.getSSHHostApprovals(getApprovalDao(), Policy.temporaryApprovalSeconds(getContext(), Approval.ApprovalType.SSH_USER_HOST), pairingUUID);
            approvedSSHHostsAdapter.setItems(approvedSSHHosts);

            new Handler(Looper.getMainLooper()).post(() -> {
                if (approvedRequestTypes.size() == 0) {
                    approvedRequestTypesContainer.setVisibility(View.GONE);
                } else {
                    approvedRequestTypesContainer.setVisibility(View.VISIBLE);
                }

                if (approvedSSHHosts.size() == 0) {
                    approvedSSHHostsContainer.setVisibility(View.GONE);
                } else {
                    approvedSSHHostsContainer.setVisibility(View.VISIBLE);
                }
            });

            if (approvedRequestTypes.isEmpty() && approvedSSHHosts.isEmpty()) {
                getFragmentManager().popBackStack();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            getApprovalDao().registerObserver(observer);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.approvals_list, container, false);

        RecyclerView approvedRequestTypes = v.findViewById(R.id.approvedRequestTypes);
        approvedRequestTypes.setAdapter(approvedRequestTypesAdapter);
        approvedRequestTypes.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));
        approvedRequestTypesContainer = v.findViewById(R.id.requestTypesContainer);


        RecyclerView approvedSSHHosts = v.findViewById(R.id.approvedSSHHosts);
        approvedSSHHosts.setAdapter(approvedSSHHostsAdapter);
        approvedSSHHosts.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));
        approvedSSHHostsContainer = v.findViewById(R.id.sshHostsContainer);

        TextView workstationName = v.findViewById(R.id.workstationName);
        Pairing pairing = Silo.shared(getContext()).pairings().getPairing(pairingUUID);
        if (pairing != null) {
            workstationName.setText(pairing.workstationName);
        } else {
            workstationName.setText("");
        }

        refreshAdapters();

        return v;
    }

    @Override
    public void onDetach() {
        try {
            getApprovalDao().unregisterObserver(observer);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        super.onDetach();
    }
}
