package co.krypt.kryptonite.devices;

import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import co.krypt.kryptonite.R;
import co.krypt.kryptonite.log.SignatureLog;
import co.krypt.kryptonite.pairing.Pairing;
import co.krypt.kryptonite.pairing.Session;

public class DevicesRecyclerViewAdapter extends RecyclerView.Adapter<DevicesRecyclerViewAdapter.ViewHolder> {

    private final List<Session> sessions;
    private final OnDeviceListInteractionListener mListener;

    public DevicesRecyclerViewAdapter(List<Session> items, OnDeviceListInteractionListener listener) {
        sessions = items;
        mListener = listener;
    }

    public void setPairings(List<Session> sessions) {
        this.sessions.clear();
        for (Session session : sessions) {
            this.sessions.add(session);
        }
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.device, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        holder.device = sessions.get(position).pairing;
        holder.lastLog = sessions.get(position).lastCommand;

        holder.deviceName.setText(sessions.get(position).pairing.workstationName);
        if (holder.lastLog != null) {
            holder.lastCommand.setText(holder.lastLog.userHostText());
            holder.lastCommandTime.setText(DateUtils.getRelativeTimeSpanString(holder.lastLog.unixSeconds * 1000, System.currentTimeMillis(), 1000));
        }

       holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    mListener.onListFragmentInteraction(holder.device);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return sessions.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView deviceName;
        public final TextView lastCommand;
        public final TextView lastCommandTime;
        public Pairing device;
        public SignatureLog lastLog;

        public ViewHolder(final View view) {
            super(view);
            mView = view;
            deviceName = (TextView) view.findViewById(R.id.deviceName);
            lastCommand = (TextView) view.findViewById(R.id.lastCommandText);
            lastCommandTime = (TextView) view.findViewById(R.id.lastCommandTimeText);
            lastCommandTime.setText("");
        }

        @Override
        public String toString() {
            return super.toString() + " '" + deviceName.getText() + "'";
        }
    }
}
