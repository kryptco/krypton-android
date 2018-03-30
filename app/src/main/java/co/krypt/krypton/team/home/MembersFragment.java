package co.krypt.krypton.team.home;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.ListViewCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import co.krypt.krypton.R;
import co.krypt.krypton.team.Sigchain;
import co.krypt.krypton.team.TeamService;
import co.krypt.krypton.uiutils.Transitions;

/**
 * Created by Kevin King on 2/14/18.
 * Copyright 2018. KryptCo, Inc.
 */

public class MembersFragment extends Fragment {
    private static final String TAG = "MembersFragment";

    private ListViewCompat membersList;
    private ArrayAdapter<Sigchain.Member> membersAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_teams_members, container, false);

        membersAdapter = new MemberAdapter(getContext());
        membersList = v.findViewById(R.id.membersList);
        membersList.setAdapter(membersAdapter);

        TeamService.GetTeamHomeDataResult lastTeamHomeData = EventBus.getDefault().getStickyEvent(TeamService.GetTeamHomeDataResult.class);
        if (lastTeamHomeData != null) {
            updateUI(lastTeamHomeData);
        }

        EventBus.getDefault().register(this);
        return v;
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void updateUI(TeamService.GetTeamHomeDataResult result) {
        if (result.r.success == null) {
            return;
        }
        Sigchain.TeamHomeData teamHomeData = result.r.success;
        List<Sigchain.Member> activeMembers = new ArrayList<>();
        for (Sigchain.Member member: teamHomeData.members) {
            if (!member.is_removed) {
                activeMembers.add(member);
            }
        }
        membersAdapter.clear();
        membersAdapter.addAll(activeMembers);
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

    private class MemberAdapter extends ArrayAdapter<Sigchain.Member> {

        public MemberAdapter(@NonNull Context context) {
            super(context, R.layout.teams_member_item, R.id.memberEmail);
        }

        @NonNull
        public View getView(int position, View convertView, @NonNull ViewGroup container) {
            View v = super.getView(position, convertView, container);

            Sigchain.Member member = getItem(position);

            if (member == null) {
                return v;
            }

            AppCompatTextView email = v.findViewById(R.id.memberEmail);
            email.setText(member.identity.email);

            AppCompatTextView adminLabel = v.findViewById(R.id.adminLabel);
            if (member.is_admin) {
                adminLabel.setAlpha(1f);
                adminLabel.setText("ADMIN");
            } else {
                adminLabel.setAlpha(0f);
            }

            AppCompatTextView indexText = v.findViewById(R.id.memberIndex);
            indexText.setText(String.valueOf(position + 1) + ".");

            v.setOnClickListener(v_ -> {
                Transitions.beginSlide(MembersFragment.this)
                        .replace(R.id.fragmentOverlay, MemberDetailFragment.newInstance(
                                membersAdapter.getItem(position).identity.publicKey
                        ))
                        .addToBackStack(null)
                        .commitAllowingStateLoss();
            });

            return v;
        }
    }
}
