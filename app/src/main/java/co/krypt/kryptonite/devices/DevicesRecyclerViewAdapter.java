package co.krypt.kryptonite.devices;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import co.krypt.kryptonite.R;

public class DevicesRecyclerViewAdapter extends RecyclerView.Adapter<DevicesRecyclerViewAdapter.ViewHolder> {

    private final List<Device> mValues;
    private final OnDeviceListInteractionListener mListener;

    public DevicesRecyclerViewAdapter(List<Device> items, OnDeviceListInteractionListener listener) {
        mValues = items;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.device, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        holder.deviceName.setText(mValues.get(position).workstationName);

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    mListener.onListFragmentInteraction(holder.mItem);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView deviceName;
        public Device mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            deviceName = (TextView) view.findViewById(R.id.deviceName);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + deviceName.getText() + "'";
        }
    }
}
