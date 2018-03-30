package co.krypt.krypton.team.home;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatTextView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.util.Arrays;

import co.krypt.krypton.R;
import co.krypt.krypton.pgp.asciiarmor.AsciiArmor;
import co.krypt.krypton.ssh.Wire;
import co.krypt.krypton.team.Sigchain;
import co.krypt.krypton.team.TeamService;
import co.krypt.krypton.team.TeamService.C;
import co.krypt.krypton.uiutils.Error;

/**
 * Created by Kevin King on 2/14/18.
 * Copyright 2018. KryptCo, Inc.
 */

public class MemberDetailFragment extends Fragment {
    private static final String TAG = "MemberDetailFragment";

    byte[] publicKey;

    AppCompatTextView email;
    AppCompatTextView roleLabel;
    AppCompatButton sshButton;
    AppCompatButton pgpButton;
    AppCompatButton promoteDemoteButton;
    AppCompatButton removeButton;

    public MemberDetailFragment() {
    }

    public static MemberDetailFragment newInstance(byte[] publicKey) {
        MemberDetailFragment f = new MemberDetailFragment();
        Bundle args = new Bundle();
        args.putByteArray("publicKey", publicKey);
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_teams_member_detail, container, false);

        email = v.findViewById(R.id.email);
        email.setPaintFlags(email.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        roleLabel = v.findViewById(R.id.roleLabel);
        sshButton = v.findViewById(R.id.sshCopyButton);
        pgpButton = v.findViewById(R.id.pgpCopyButton);

        promoteDemoteButton = v.findViewById(R.id.promoteDemoteButton);
        promoteDemoteButton.setAlpha(0f);

        removeButton = v.findViewById(R.id.removeButton);
        removeButton.setAlpha(0f);

        EventBus.getDefault().register(this);
        return v;
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void updateUI(TeamService.GetTeamHomeDataResult result) {
        if (result.r.success == null) {
            return;
        }

        boolean i_am_admin = result.r.success.is_admin;

        Sigchain.Member selected = null;
        for (Sigchain.Member member: result.r.success.members) {
            if (Arrays.equals(publicKey, member.identity.publicKey)) {
                selected = member;
            }
        }
        if (selected == null) {
            getFragmentManager().popBackStack();
            return;
        }
        final Sigchain.Member member = selected;

        if (selected.is_removed) {
            roleLabel.setText("Removed");
        } else {
            if (selected.is_admin) {
                roleLabel.setText("Admin");
            } else {
                roleLabel.setText("Member");
            }
        }

        email.setText(selected.identity.email);
        if (i_am_admin) {
            if (selected.is_removed) {
                removeButton.animate().alpha(0f).setDuration(500).start();
                removeButton.setEnabled(false);

                promoteDemoteButton.animate().alpha(0f).setDuration(500).start();
                promoteDemoteButton.setEnabled(false);
            } else {
                removeButton.animate().alpha(1f).setDuration(500).start();
                removeButton.setEnabled(true);
                removeButton.setOnClickListener(v -> requestOp(Sigchain.RequestableTeamOperation.remove(publicKey)));

                promoteDemoteButton.setEnabled(true);
                promoteDemoteButton.animate().alpha(1f).setDuration(500).start();

                if (selected.is_admin) {
                    promoteDemoteButton.setText("Demote");
                    promoteDemoteButton.setOnClickListener(v -> requestOp(Sigchain.RequestableTeamOperation.demote(publicKey)));
                } else {
                    promoteDemoteButton.setText("Promote");
                    promoteDemoteButton.setOnClickListener(v -> requestOp(Sigchain.RequestableTeamOperation.promote(publicKey)));
                }
            }
        } else {
            promoteDemoteButton.setEnabled(false);
            promoteDemoteButton.animate().alpha(0f).setDuration(500).start();
            removeButton.setEnabled(false);
            removeButton.animate().alpha(0f).setDuration(500).start();
        }

        sshButton.setOnClickListener(v -> {
            Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            try {
                sharingIntent.putExtra(Intent.EXTRA_TEXT, Wire.publicKeyBytesToAuthorizedKeysFormat(member.identity.sshPublicKey));
                startActivity(Intent.createChooser(sharingIntent, "Share " + member.identity.email + "'s SSH public key"));
            } catch (IOException e) {
                Error.shortToast(getContext(), "Failed to export public key");
                e.printStackTrace();
            }
        });

        pgpButton.setOnClickListener(v -> {
            Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT,
                    new AsciiArmor(AsciiArmor.HeaderLine.PUBLIC_KEY, AsciiArmor.KRYPTON_DEFAULT_HEADERS, member.identity.pgpPublicKey).toString()
            );
            startActivity(Intent.createChooser(sharingIntent, "Share " + member.identity.email + "'s PGP public key"));
        });
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
        publicKey = getArguments().getByteArray("publicKey");
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

}
