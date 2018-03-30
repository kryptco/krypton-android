package co.krypt.krypton.team.home;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
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

import java.util.List;

import co.krypt.krypton.R;
import co.krypt.krypton.team.Sigchain;
import co.krypt.krypton.team.TeamService;
import co.krypt.krypton.team.TeamService.C;

/**
 * Created by Kevin King on 2/14/18.
 * Copyright 2018. KryptCo, Inc.
 */

public class BlocksFragment extends Fragment {
    private static final String TAG = "BlocksFragment";

    private ListViewCompat blocksList;
    private ArrayAdapter<Sigchain.FormattedBlock> blocksAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_teams_activity_list, container, false);

        blocksAdapter = new BlockAdapter(getContext());
        blocksList = v.findViewById(R.id.blocksList);
        blocksList.setAdapter(blocksAdapter);

        EventBus.getDefault().register(this);
        EventBus.getDefault().post(
                new TeamService.FormatBlocks(
                        C.background(getContext())
                )
        );
        return v;
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void updateUI(TeamService.FormatBlocksResult result) {
        if (result.r.success == null) {
            return;
        }
        List<Sigchain.FormattedBlock> blocks = result.r.success;
        blocksAdapter.clear();
        blocksAdapter.addAll(blocks);
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

    private class BlockAdapter extends ArrayAdapter<Sigchain.FormattedBlock> {

        public BlockAdapter(@NonNull Context context) {
            super(context, R.layout.teams_activity_item, R.id.header);
        }

        @NonNull
        public View getView(int position, View convertView, @NonNull ViewGroup container) {
            View v = super.getView(position, convertView, container);

            Sigchain.FormattedBlock block = getItem(position);

            AppCompatTextView header = v.findViewById(R.id.header);
            header.setText(block.header);

            AppCompatTextView author = v.findViewById(R.id.author);
            author.setText("by " + block.author);

            AppCompatTextView time = v.findViewById(R.id.time);
            time.setText(block.time);

            AppCompatTextView body = v.findViewById(R.id.body);
            ConstraintLayout.LayoutParams layout = (ConstraintLayout.LayoutParams) body.getLayoutParams();
            if (block.body != null) {
                body.setText(block.body);
                layout.height = ConstraintLayout.LayoutParams.WRAP_CONTENT;
                body.setLayoutParams(layout);
            } else {
                body.setText("");
                layout.height = 1;
                body.setLayoutParams(layout);
            }

            View upperEdge = v.findViewById(R.id.upperEdge);
            if (block.last) {
                upperEdge.setVisibility(View.INVISIBLE);
            } else {
                upperEdge.setVisibility(View.VISIBLE);
            }
            View lowerEdge = v.findViewById(R.id.lowerEdge);
            if (block.first) {
                lowerEdge.setVisibility(View.INVISIBLE);
            } else {
                lowerEdge.setVisibility(View.VISIBLE);
            }

            return v;
        }
    }
}
