package co.krypt.krypton.devices;

import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import co.krypt.krypton.R;
import co.krypt.krypton.log.Log;
import co.krypt.krypton.pairing.DeviceType;
import co.krypt.krypton.pairing.Pairing;
import co.krypt.krypton.pairing.Session;

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
        View view;
        if (viewType == 1) {
            //Extension device
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.ext_device, parent, false);
        }
        else {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.cli_device, parent, false);
        }
        return new ViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        holder.device = sessions.get(position).pairing;

        holder.deviceName.setText(sessions.get(position).pairing.getDisplayName());
        if(holder.getItemViewType() == 1) {
            holder.deviceIcon.setImageResource(DeviceType.getDeviceIcon(holder.device.deviceType));
        }

        holder.updateLastLog(sessions.get(position).lastApproval);

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

    @Override
    public int getItemViewType(int position) {
        Pairing pairing = sessions.get(position).pairing;
        return pairing.isBrowser() ? 1 : 0;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView deviceName;
        public final AppCompatImageView deviceIcon;
        public final TextView lastCommand;
        public final TextView lastCommandTime;
        public final int viewType;
        public Pairing device;
        private Log lastLog;

        public ViewHolder(final View view, int viewType) {
            super(view);
            mView = view;
            deviceName = (TextView) view.findViewById(R.id.deviceName);
            deviceIcon = (AppCompatImageView) view.findViewById(R.id.deviceIcon);
            lastCommand = (TextView) view.findViewById(R.id.lastCommandText);
            lastCommand.setText("");
            lastCommandTime = (TextView) view.findViewById(R.id.lastCommandTimeText);
            lastCommandTime.setText("");
            this.viewType = viewType;
        }

        public void updateLastLog(Log log) {
            lastLog = log;
            switch (viewType) {
                case 1: //Extension device
                    if (lastLog != null) {
                        lastCommand.setText(lastLog.shortDisplay());
                        lastCommandTime.setText(DateUtils.getRelativeTimeSpanString(lastLog.unixSeconds() * 1000, System.currentTimeMillis(), 1000));
                    } else {
                        lastCommand.setText("no activity");
                        lastCommandTime.setText("");
                    }
                    break;
                default:
                    if (lastLog != null) {
                        lastCommand.setText(lastLog.shortDisplay());
                        lastCommandTime.setText(DateUtils.getRelativeTimeSpanString(lastLog.unixSeconds() * 1000, System.currentTimeMillis(), 1000));
                    } else {
                        lastCommand.setText("no activity");
                        lastCommandTime.setText("");
                    }
                    break;
            }
        }

        @Override
        public String toString() {
            return super.toString() + " '" + deviceName.getText() + "'";
        }
    }
}
