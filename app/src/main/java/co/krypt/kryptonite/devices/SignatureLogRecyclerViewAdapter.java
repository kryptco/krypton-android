package co.krypt.kryptonite.devices;

import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import co.krypt.kryptonite.R;
import co.krypt.kryptonite.log.Log;

public class SignatureLogRecyclerViewAdapter extends RecyclerView.Adapter<SignatureLogRecyclerViewAdapter.ViewHolder> {

    private final List<Log> mValues;

    public SignatureLogRecyclerViewAdapter(List<Log> items) {
        mValues = items;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.signature_log, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.log = mValues.get(position);
//        holder.commandText.setText(holder.log.shortDisplay());
//        holder.commandTime.setText(
//                DateUtils.getRelativeTimeSpanString(holder.log.unixSeconds() * 1000, System.currentTimeMillis(), 1000));

        holder.log.fillShortView((ConstraintLayout) holder.mView);
        holder.mView.requestLayout();

        holder._long = false;
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public synchronized void setLogs(List<Log> newLogs) {
        mValues.clear();
        for (Log log : newLogs) {
            mValues.add(log);
        }
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public Log log;

        private boolean _long = false;

        public ViewHolder(View view) {
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
}
