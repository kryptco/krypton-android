package co.krypt.kryptonite.devices;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

import co.krypt.kryptonite.R;
import co.krypt.kryptonite.pairing.Pairing;

public class DevicesRecyclerViewAdapter extends RecyclerView.Adapter<DevicesRecyclerViewAdapter.ViewHolder> {

    private final List<Pairing> mValues;
    private final OnDeviceListInteractionListener mListener;

    public DevicesRecyclerViewAdapter(List<Pairing> items, OnDeviceListInteractionListener listener) {
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
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        holder.mItem = mValues.get(position);
        holder.deviceName.setText(mValues.get(position).workstationName);

//        holder.unpairButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Silo.shared(v.getContext()).unpair(holder.mItem);
//                mValues.remove(position);
//                notifyItemRemoved(position);
//                notifyDataSetChanged();
//            }
//        });

       holder.moreButton.setOnClickListener(new View.OnClickListener() {
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
        public final TextView lastCommand;
        public final TextView lastCommandTime;
        public final Button unpairButton;
        public final Button moreButton;
        public Pairing mItem;

        public ViewHolder(final View view) {
            super(view);
            mView = view;
            deviceName = (TextView) view.findViewById(R.id.deviceName);
            lastCommand = (TextView) view.findViewById(R.id.lastCommandText);
            lastCommandTime = (TextView) view.findViewById(R.id.lastCommandTimeText);
            unpairButton = (Button) view.findViewById(R.id.unpairButton);
            moreButton = (Button) view.findViewById(R.id.moreButton);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + deviceName.getText() + "'";
        }
    }
}
