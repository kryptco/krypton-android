package co.krypt.krypton.team.home;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.ListViewCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import co.krypt.krypton.R;
import co.krypt.krypton.crypto.Base64;
import co.krypt.krypton.crypto.SHA256;
import co.krypt.krypton.exception.CryptoException;
import co.krypt.krypton.knownhosts.KnownHost;
import co.krypt.krypton.silo.Silo;
import co.krypt.krypton.ssh.Wire;
import co.krypt.krypton.team.Sigchain;
import co.krypt.krypton.team.TeamService;
import co.krypt.krypton.team.TeamService.C;

/**
 * Created by Kevin King on 2/14/18.
 * Copyright 2018. KryptCo, Inc.
 */

public class PinHostsFragment extends Fragment {
    private static final String TAG = "PinHostsFragment";

    private ListViewCompat hostsList;
    private HostsAdapter hostsAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_teams_pinned_hosts, container, false);

        hostsAdapter = new HostsAdapter(getContext());
        hostsList = v.findViewById(R.id.hostsList);
        hostsList.setAdapter(hostsAdapter);

        EventBus.getDefault().register(this);
        return v;
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void updateUI(TeamService.GetTeamHomeDataResult result) {
        if (result.r.success == null) {
            return;
        }
        Sigchain.TeamHomeData teamHomeData = result.r.success;
        List<Sigchain.PinnedHost> pinnedHosts = new ArrayList<>();
        pinnedHosts.addAll(Arrays.asList(teamHomeData.pinnedHosts));

        List<Sigchain.PinnedHost> unpinnedHosts = new ArrayList<>();
        try {
            for (KnownHost knownHost: Silo.shared(getContext()).getKnownHosts()) {
                try {
                    Sigchain.PinnedHost localKnownHost = new Sigchain.PinnedHost(knownHost.hostName, Base64.decode(knownHost.publicKey));
                    boolean alreadyPinned = false;
                    for (Sigchain.PinnedHost pinnedHost: pinnedHosts) {
                        if (pinnedHost.equals(localKnownHost)) {
                            alreadyPinned = true;
                            break;
                        }
                    }
                    if (!alreadyPinned) {
                        unpinnedHosts.add(localKnownHost);
                    }
                } catch (CryptoException e) {
                    e.printStackTrace();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        hostsAdapter.clear();
        for (Sigchain.PinnedHost pinnedHost: pinnedHosts) {
            hostsAdapter.add(new Host(pinnedHost, true, teamHomeData.is_admin));
        }
        if (teamHomeData.is_admin) {
            for (Sigchain.PinnedHost unpinnedHost : unpinnedHosts) {
                hostsAdapter.add(new Host(unpinnedHost, false, teamHomeData.is_admin));
            }
        }
    }

    private void requestOp(Sigchain.RequestableTeamOperation op) {
        TeamService.RequestTeamOperation request = new TeamService.RequestTeamOperation(
                op,
                C.withConfirmStatusCallback(
                        getActivity(),
                        r -> EventBus.getDefault().post(new TeamService.UpdateTeamHomeData(getContext()))
                )
        );
        EventBus.getDefault().post(request);
    }

    @Override
    public void onDestroyView() {
        EventBus.getDefault().unregister(this);
        super.onDestroyView();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private static class Host {
        public Host(Sigchain.PinnedHost host, boolean pinned, boolean showPinUnpinButton) {
            this.host = host;
            this.pinned = pinned;
            this.showPinUnpinButton = showPinUnpinButton;
        }

        public final Sigchain.PinnedHost host;
        public final boolean pinned;
        public final boolean showPinUnpinButton;

    }


    private class HostsAdapter extends ArrayAdapter<Host> {

        public HostsAdapter(@NonNull Context context) {
            super(context, R.layout.teams_pinnedhost_item, R.id.hostname);
        }

        @NonNull
        public View getView(int position, View convertView, @NonNull ViewGroup container) {
            View v = super.getView(position, convertView, container);

            Host h = getItem(position);

            AppCompatTextView host = v.findViewById(R.id.hostname);
            host.setText(h.host.host);

            AppCompatTextView fullKey = v.findViewById(R.id.fullKey);
            try {
                fullKey.setText(Wire.publicKeyBytesToAuthorizedKeysFormat(h.host.publicKey));
            } catch (IOException e) {
                fullKey.setText("");
                e.printStackTrace();
            }

            AppCompatTextView fingerprint = v.findViewById(R.id.keyFingerprint);
            try {
                fingerprint.setText(
                        "SHA256:" + Base64.encode(SHA256.digest(h.host.publicKey))
                );
            } catch (CryptoException e) {
                fingerprint.setText("");
                e.printStackTrace();
            }

            ConstraintLayout detailsContainer = v.findViewById(R.id.detailsContainer);
            detailsContainer.setVisibility(View.GONE);
            AppCompatButton detailsButton = v.findViewById(R.id.detailsButton);
            detailsButton.setOnClickListener(v_ -> {
                if (detailsContainer.getVisibility() == View.VISIBLE) {
                    detailsContainer.setVisibility(View.GONE);
                } else {
                    detailsContainer.setVisibility(View.VISIBLE);
                }
            });

            AppCompatButton pinUnpinButton = v.findViewById(R.id.pinUnpinButton);
            if (h.pinned) {
                host.setTextColor(getResources().getColor(R.color.appGreen, null));
                pinUnpinButton.setText("Unpin");
                pinUnpinButton.setTextColor(getResources().getColor(R.color.appReject, null));
                pinUnpinButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.outline_button_reject, null));
                pinUnpinButton.setOnClickListener(v_ -> {
                    requestOp(Sigchain.RequestableTeamOperation.unpinHostKey(h.host));
                });
            } else {
                host.setTextColor(getResources().getColor(R.color.appWarning, null));
                pinUnpinButton.setText("Pin");
                pinUnpinButton.setTextColor(getResources().getColor(R.color.appGreen, null));
                pinUnpinButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.outline_button, null));
                pinUnpinButton.setOnClickListener(v_ -> {
                    requestOp(Sigchain.RequestableTeamOperation.pinHostKey(h.host));
                });
            }

            if (h.showPinUnpinButton) {
                pinUnpinButton.setAlpha(1f);
            } else {
                pinUnpinButton.setAlpha(0f);
            }
            pinUnpinButton.setEnabled(h.showPinUnpinButton);

            AppCompatButton copyButton = v.findViewById(R.id.copyButton);
            copyButton.setOnClickListener(v_ -> {
                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                try {
                    String knownHostsLine = h.host.host + " " + Wire.publicKeyBytesToAuthorizedKeysFormat(h.host.publicKey);
                    sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, knownHostsLine);
                    startActivity(Intent.createChooser(sharingIntent, "Share pinned SSH public key of " + h.host.host));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            return v;
        }
    }
}
