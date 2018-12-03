package co.krypt.krypton.totp;

import android.animation.ObjectAnimator;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

import co.krypt.krypton.R;
import co.krypt.krypton.crypto.TOTP;
import co.krypt.krypton.exception.CryptoException;

public class TOTPAccountsFragment extends Fragment {
    private static final String TAG = "TOTPAccountsFragment";

    private RecyclerView totpAccounts;
    private TOTPRecyclerViewAdapter totpAccountsAdapter;

    public TOTPAccountsFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            totpAccountsAdapter = new TOTPRecyclerViewAdapter(TOTP.getAccounts(getContext()));
        }
        catch (CryptoException e) {
            e.printStackTrace();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_totp_accounts, container, false);
        totpAccounts = v.findViewById(R.id.totpAccounts);
        totpAccounts.setAdapter(totpAccountsAdapter);
        totpAccounts.setLayoutManager(new LinearLayoutManager(getContext()));
        return v;
    }

    private class TOTPRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final List<TOTPAccount> accounts;

        private TOTPRecyclerViewAdapter(List<TOTPAccount> accounts) {
            this.accounts = accounts;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.totp_item, parent, false);
            return new TOTPViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
            TOTPViewHolder totpHolder = (TOTPViewHolder) holder;
            totpHolder.account = accounts.get(position);
            totpHolder.updateView();
        }

        @Override
        public int getItemCount() {
            return accounts.size();
        }

        private class TOTPViewHolder extends RecyclerView.ViewHolder {
            private final View mView;
            private TOTPAccount account;

            private Handler updater;
            private Runnable updaterTask;

            public TextView issuer;
            public TextView username;
            public TextView otp;
            public ProgressBar progressBar;
            public TextView showCodeButton;

            private boolean codeShown = false;

            private final int appGreen = getResources().getColor(R.color.appGreen, getContext().getTheme());
            private final int appWarning = getResources().getColor(R.color.appWarning, getContext().getTheme());

            private TOTPViewHolder(View view) {
                super(view);
                mView = view;

                issuer = mView.findViewById(R.id.issuer);
                username = mView.findViewById(R.id.username);
                otp = mView.findViewById(R.id.otp);
                progressBar = mView.findViewById(R.id.timeRemaining);
                progressBar.setMax(TOTP.DEFAULT_PERIOD*100);
                showCodeButton = mView.findViewById(R.id.showCodeButton);

                updater = new Handler();
                updaterTask = new Runnable() {
                    @Override
                    public void run() {
                        int progress = account.getOtpAge();
                        progressBar.setProgress(progress*100);

                        if (account.period - progress <= 5) {
                            progressBar.getProgressDrawable().setColorFilter(appWarning, PorterDuff.Mode.MULTIPLY);
                        }
                        else {
                            progressBar.getProgressDrawable().setColorFilter(appGreen, PorterDuff.Mode.MULTIPLY);
                        }

                        ObjectAnimator animation = ObjectAnimator.ofInt(progressBar, "progress", (progress+1)*100);
                        animation.setDuration(1000);
                        animation.setInterpolator(new LinearInterpolator());
                        animation.start();

                        updateOtp();

                        updater.postDelayed(this, 1000);
                    }
                };

                view.setOnClickListener(this::toggleView);
            }

            private void toggleView(View v) {
                codeShown = !codeShown;
                if (codeShown) {
                    issuer.setVisibility(View.INVISIBLE);
                    username.setVisibility(View.INVISIBLE);
                    otp.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.VISIBLE);
                    showCodeButton.setText("Hide");
                    showCodeButton.getBackground().setTint(appWarning);
                    showCodeButton.setTextColor(appWarning);
                    updater.post(updaterTask);
                }
                else {
                    issuer.setVisibility(View.VISIBLE);
                    username.setVisibility(View.VISIBLE);
                    otp.setVisibility(View.INVISIBLE);
                    progressBar.setVisibility(View.INVISIBLE);
                    showCodeButton.setText("Show");
                    showCodeButton.getBackground().setTint(appGreen);
                    showCodeButton.setTextColor(appGreen);
                    updater.removeCallbacks(updaterTask);
                }
            }

            private void updateOtp() {
                try {
                    otp.setText(account.getOtp());
                }
                catch (CryptoException e) {
                    e.printStackTrace();
                }
            }

            private void updateView() {
                issuer.setText(account.issuer);
                username.setText(account.getUsername());
                updateOtp();
                progressBar.setMax(account.period*100);
            }
        }
    }
}
