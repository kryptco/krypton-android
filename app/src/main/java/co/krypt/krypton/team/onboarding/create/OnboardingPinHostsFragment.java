package co.krypt.krypton.team.onboarding.create;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.firebase.crash.FirebaseCrash;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nullable;

import co.krypt.krypton.R;
import co.krypt.krypton.crypto.Base64;
import co.krypt.krypton.exception.CryptoException;
import co.krypt.krypton.knownhosts.KnownHost;
import co.krypt.krypton.silo.Silo;
import co.krypt.krypton.team.Sigchain;

public class OnboardingPinHostsFragment extends Fragment implements OnboardingPinHostsRecyclerViewAdapter.OnSelectionChange {

    private final String TAG = "OnboardingPinHostsFragment";

    private final OnboardingPinHostsRecyclerViewAdapter adapter = new OnboardingPinHostsRecyclerViewAdapter(this);

    public OnboardingPinHostsFragment() {
    }


    public static OnboardingPinHostsFragment newInstance() {
        return new OnboardingPinHostsFragment();
    }

    @Nullable
    private AppCompatTextView selectedHostsCountText;

    @Nullable
    private AppCompatButton toggleAllButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_teams_onboarding_pinhosts, container, false);

        RecyclerView knownHostsView = rootView.findViewById(R.id.pinnedHostsList);
        knownHostsView.setLayoutManager(new LinearLayoutManager(getContext()));
        knownHostsView.setAdapter(adapter);

        Button nextButton = rootView.findViewById(R.id.nextButton);
        nextButton.setOnClickListener(v -> {
            ArrayList<Sigchain.PinnedHost> selectedHosts = new ArrayList<>();
            List<Pair<KnownHost, AtomicBoolean>> hosts = adapter.getKnownHosts();
            for (Pair<KnownHost, AtomicBoolean> pair: hosts) {
                if (pair.second.get()) {
                    try {
                        selectedHosts.add(
                                new Sigchain.PinnedHost(pair.first.hostName, Base64.decode(pair.first.publicKey))
                        );
                    } catch (CryptoException e) {
                        e.printStackTrace();
                        FirebaseCrash.report(e);
                    }
                }
            }

            CreateTeamProgress progress = new CreateTeamProgress(v.getContext());
            progress.updateTeamData((s, d) -> {
                d.pinnedHosts = selectedHosts.toArray(new Sigchain.PinnedHost[0]);
                return CreateStage.VERIFY_EMAIL;
            });

            FragmentManager fragmentManager = getFragmentManager();
            if (fragmentManager != null) {
                fragmentManager.beginTransaction()
                        .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_right, R.anim.exit_to_left)
                        .replace(R.id.fragment_teams, new OnboardingVerifyEmailFragment())
                        .commitNowAllowingStateLoss();
            }
        });

        selectedHostsCountText = rootView.findViewById(R.id.selectedTextView);

        toggleAllButton = rootView.findViewById(R.id.selectAllButton);

        updateUI(adapter.getKnownHosts());

        return rootView;
    }

    private void updateUI(List<Pair<KnownHost, AtomicBoolean>> hosts) {
        long selectedHosts = 0;
        for (Pair<KnownHost, AtomicBoolean> pair: hosts) {
            if (pair.second.get()) {
                selectedHosts++;
            }
        }
        if (selectedHostsCountText != null) {
            selectedHostsCountText.setText(String.valueOf(selectedHosts) + "/" + String.valueOf(hosts.size()) + " selected");
        }
        if (toggleAllButton != null) {
            if (selectedHosts == hosts.size()) {
                toggleAllButton.setText("Clear All");
                toggleAllButton.setTextColor(toggleAllButton.getResources().getColor(R.color.appBlack, null));
                toggleAllButton.setOnClickListener(v -> {
                    adapter.clearAll();
                });
            } else {
                toggleAllButton.setText("Select All");
                toggleAllButton.setTextColor(toggleAllButton.getResources().getColor(R.color.appGreen, null));
                toggleAllButton.setOnClickListener(v -> {
                    adapter.selectAll();
                });
            }
        }
    }

    @Override
    public void onDestroyView() {
        selectedHostsCountText = null;
        toggleAllButton = null;
        super.onDestroyView();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            List<KnownHost> knownHosts = Silo.shared(context).getKnownHosts();
            adapter.setKnownHosts(knownHosts);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void selectedHosts(List<Pair<KnownHost, AtomicBoolean>> hosts) {
        updateUI(hosts);
    }
}
