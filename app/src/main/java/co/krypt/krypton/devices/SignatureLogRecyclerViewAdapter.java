package co.krypt.krypton.devices;

import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import co.krypt.krypton.R;
import co.krypt.krypton.log.Log;

public class SignatureLogRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final List<Log> mValues;
    public AtomicReference<View> deviceCardView = new AtomicReference<>(null);
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private static final String TAG = "SignatureLogAdapter";

    public SignatureLogRecyclerViewAdapter(List<Log> items) {
        mValues = items;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            return new DeviceViewHolder(deviceCardView.get());
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.signature_log, parent, false);
            return new LogViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof DeviceViewHolder) {
            //  rendered by DeviceDetailFragment
        } else if (holder instanceof LogViewHolder) {
            LogViewHolder logHolder = (LogViewHolder) holder;
            logHolder.log = mValues.get(position - 1);

            logHolder.log.fillShortView((ConstraintLayout) logHolder.mView);
            logHolder.mView.requestLayout();

            logHolder._long = false;
        }
    }

    @Override
    public int getItemCount() {
        //  items + header
        return mValues.size() + (deviceCardView.get() != null ? 1 : 0);
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return TYPE_HEADER;
        }

        return TYPE_ITEM;
    }

    public synchronized void setLogs(List<Log> newLogs) {
        mValues.clear();
        for (Log log : newLogs) {
            mValues.add(log);
        }
        notifyDataSetChanged();
    }

    public static class LogViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public Log log;

        private boolean _long = false;

        public LogViewHolder(View view) {
            super(view);
            mView = view;
            view.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            _long = !_long;
                            if (_long) {
                                log.fillLongView((ConstraintLayout) v);
                            } else {
                                log.fillShortView((ConstraintLayout) v);
                            }
                        }
                    }
            );
        }
    }
    public static class DeviceViewHolder extends RecyclerView.ViewHolder {
        public final View mView;

        public DeviceViewHolder(View view) {
            super(view);
            mView = view;
        }
    }
}
