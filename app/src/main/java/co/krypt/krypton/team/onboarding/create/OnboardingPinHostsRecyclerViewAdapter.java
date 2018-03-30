package co.krypt.krypton.team.onboarding.create;

import android.content.res.Resources;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import co.krypt.krypton.R;
import co.krypt.krypton.knownhosts.KnownHost;


public class OnboardingPinHostsRecyclerViewAdapter extends RecyclerView.Adapter<OnboardingPinHostsRecyclerViewAdapter.ViewHolder> {

    interface OnSelectionChange {
        void selectedHosts(List<Pair<KnownHost, AtomicBoolean>> hosts);
    }

    //  (Host, Selected)
    private final List<Pair<KnownHost, AtomicBoolean>> knownHosts;

    private final OnSelectionChange callback;

    public OnboardingPinHostsRecyclerViewAdapter(OnSelectionChange callback) {
        knownHosts = new LinkedList<>();
        this.callback = callback;
    }

    public void setKnownHosts(List<KnownHost> knownHosts) {
        this.knownHosts.clear();
        for (KnownHost knownHost : KnownHost.sortByTimeDescending(knownHosts)) {
            //  default to selected
            this.knownHosts.add(new Pair<>(knownHost, new AtomicBoolean(true)));
        }
        onUpdate();
    }

    private void onUpdate() {
        List<KnownHost> allHosts = new LinkedList<>();
        List<KnownHost> selectedHosts = new LinkedList<>();
        List<KnownHost> unselectedHosts = new LinkedList<>();

        for (Pair<KnownHost, AtomicBoolean> pair: knownHosts) {
            if (pair.second.get()) {
                selectedHosts.add(pair.first);
            } else {
                unselectedHosts.add(pair.first);
            }
        }

        this.knownHosts.clear();
        for (KnownHost host: KnownHost.sortByHostAscending(selectedHosts)) {
            this.knownHosts.add(new Pair<>(host, new AtomicBoolean(true)));
        }
        for (KnownHost host: KnownHost.sortByHostAscending(unselectedHosts)) {
            this.knownHosts.add(new Pair<>(host, new AtomicBoolean(false)));
        }

        notifyDataSetChanged();

        callback.selectedHosts(this.knownHosts);
    }

    public List<Pair<KnownHost, AtomicBoolean>> getKnownHosts() {
        return knownHosts;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.teams_onboarding_pinnedhost_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        holder.knownHostSelected = knownHosts.get(position);
        AtomicBoolean selected = holder.knownHostSelected.second;

        Resources r = holder.mView.getResources();

        holder.hostName.setText(holder.knownHostSelected.first.hostName);
        holder.hostKeyFingerprint.setText(holder.knownHostSelected.first.fingerprint());
        holder.toggleButton.setChecked(selected.get());

        Runnable updateColors = () -> {
            if (selected.get()) {
                holder.line.setBackgroundColor(r.getColor(R.color.appGreen, null));
                holder.hostName.setTextColor(r.getColor(R.color.appBlack, null));
                holder.hostKeyFingerprint.setTextColor(r.getColor(R.color.appBlack, null));
            } else {
                holder.line.setBackgroundColor(r.getColor(R.color.appGray, null));
                holder.hostName.setTextColor(r.getColor(R.color.appGray, null));
                holder.hostKeyFingerprint.setTextColor(r.getColor(R.color.appGray, null));
            }
        };
        updateColors.run();

        holder.mView.setOnClickListener(v -> {
            holder.toggleButton.callOnClick();
        });
        holder.toggleButton.setOnClickListener(v -> {
            selected.set(!selected.get());
            holder.toggleButton.setChecked(selected.get());
            updateColors.run();

            onUpdate();
        });
    }

    public void selectAll() {
        for (Pair<KnownHost, AtomicBoolean> pair: knownHosts) {
            pair.second.set(true);
        }
        onUpdate();
    }

    public void clearAll() {
        for (Pair<KnownHost, AtomicBoolean> pair: knownHosts) {
            pair.second.set(false);
        }
        onUpdate();
    }

    @Override
    public int getItemCount() {
        return knownHosts.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final View mView;
        final TextView hostName;
        final TextView hostKeyFingerprint;
        final AppCompatCheckBox toggleButton;
        final View line;
        Pair<KnownHost, AtomicBoolean> knownHostSelected;

        public ViewHolder(final View view) {
            super(view);
            mView = view;
            hostName = (TextView) view.findViewById(R.id.hostNameText);
            hostKeyFingerprint = (TextView) view.findViewById(R.id.hostKeyFingerprintText);
            toggleButton = view.findViewById(R.id.toggleButton);
            line = view.findViewById(R.id.line);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + hostName.getText() + "'";
        }
    }
}
