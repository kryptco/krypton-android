package co.krypt.kryptonite.knownhosts;

import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.Color;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.sql.SQLException;
import java.util.List;

import co.krypt.kryptonite.R;
import co.krypt.kryptonite.silo.Silo;


public class KnownHostsRecyclerViewAdapter extends RecyclerView.Adapter<KnownHostsRecyclerViewAdapter.ViewHolder> {

    private final List<KnownHost> knownHosts;
    private final Activity activity;

    public KnownHostsRecyclerViewAdapter(Activity activity, List<KnownHost> items) {
        this.activity = activity;
        knownHosts = items;
    }

    public void setKnownHosts(List<KnownHost> knownHosts) {
        this.knownHosts.clear();
        for (KnownHost knownHost : KnownHost.sortByTimeDescending(knownHosts)) {
            this.knownHosts.add(knownHost);
        }
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.known_host, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        holder.knownHost = knownHosts.get(position);

        holder.hostName.setText(holder.knownHost.hostName);
        holder.hostKeyFingerprint.setText(holder.knownHost.fingerprint());
        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle("Unpin public key of " + holder.knownHost.hostName + "?");
                builder.setIcon(R.mipmap.ic_launcher);
                builder.setMessage("Only unpin this key if it has been purposefully changed on the server.");
                builder.setPositiveButton("Unpin",
                        new DialogInterface.OnClickListener()
                        {
                            public void onClick(DialogInterface dialog, int id) {
                                try {
                                    Silo.shared(activity.getApplicationContext()).deleteKnownHost(holder.knownHost.hostName);
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                builder.setNeutralButton("Cancel",
                        new DialogInterface.OnClickListener()
                        {
                            public void onClick(DialogInterface dialog, int id) {
                            }
                        });
                final AlertDialog dialog = builder.create();
                dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialogI) {
                        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(Color.RED);
                    }
                });
                dialog.show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return knownHosts.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView hostName;
        public final TextView hostKeyFingerprint;
        public final TextView addedTime;
        public KnownHost knownHost;

        public ViewHolder(final View view) {
            super(view);
            mView = view;
            hostName = (TextView) view.findViewById(R.id.hostNameText);
            hostKeyFingerprint = (TextView) view.findViewById(R.id.hostKeyFingerprintText);
            addedTime = (TextView) view.findViewById(R.id.addedTimeText);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + hostName.getText() + "'";
        }
    }
}
