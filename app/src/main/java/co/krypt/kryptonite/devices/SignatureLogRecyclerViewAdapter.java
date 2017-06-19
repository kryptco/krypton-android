package co.krypt.kryptonite.devices;

import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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
        holder.commandText.setText(holder.log.shortDisplay());
        holder.commandTime.setText(
                DateUtils.getRelativeTimeSpanString(holder.log.unixSeconds() * 1000, System.currentTimeMillis(), 1000));
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
        public final TextView commandText;
        public final TextView commandTime;
        public Log log;

        private boolean longText = false;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            commandText = (TextView) view.findViewById(R.id.commandText);
            view.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (longText) {
                                commandText.setText(log.shortDisplay());
                                commandText.setMaxLines(3);
                            } else {
                                commandText.setText(log.longDisplay());
                                commandText.setMaxLines(30);
                            }
                            longText = !longText;
                        }
                    }
            );
            commandTime = (TextView) view.findViewById(R.id.commandTime);

        }

        @Override
        public String toString() {
            return super.toString() + " '" + commandText.getText() + "'";
        }
    }
}
