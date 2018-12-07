package co.krypt.krypton.totp;

import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.sql.SQLException;
import java.util.List;

import co.krypt.krypton.R;
import co.krypt.krypton.crypto.TOTP;
import co.krypt.krypton.exception.CryptoException;
import co.krypt.krypton.silo.IdentityService;
import co.krypt.krypton.uiutils.Icons;
import co.krypt.kryptonite.MainActivity;

public class TOTPAccountsFragment extends Fragment {
    private static final String TAG = "TOTPAccountsFragment";

    private RecyclerView totpAccounts;
    private TOTPRecyclerViewAdapter totpAccountsAdapter;
    private View noCodesContainer;

    public TOTPAccountsFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            totpAccountsAdapter = new TOTPRecyclerViewAdapter(TOTP.getAccounts(getContext()));
        }
        catch (SQLException e) {
            Crashlytics.logException(e);
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
        noCodesContainer = v.findViewById(R.id.totpEmptyContainer);
        v.findViewById(R.id.scanTOTPButton).setOnClickListener((event) -> {
            MainActivity activity = (MainActivity) getActivity();
            activity.setActiveTab(MainActivity.SCAN_FRAGMENT_POSITION);
        });
        EventBus.getDefault().register(this);
        refreshAccounts(null);
        return v;
    }

    @Override
    public void onDestroyView() {
        EventBus.getDefault().unregister(this);
        super.onDestroyView();
    }

    @Subscribe
    public void refreshAccounts(IdentityService.TOTPAccountsUpdated _) {
        try {
            totpAccountsAdapter.accounts = TOTP.getAccounts(getContext());
            totpAccountsAdapter.notifyDataSetChanged();
            if (!totpAccountsAdapter.accounts.isEmpty()) {
                noCodesContainer.setVisibility(View.GONE);
            } else {
                noCodesContainer.setVisibility(View.VISIBLE);
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private class TOTPRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        public List<TOTPAccount> accounts;

        //The position of the TOTP account which is displaying an OTP
        public int openCode = -1;

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
            if (openCode == position) {
                totpHolder.showCode();
            }
            else {
                totpHolder.hideCode();
            }
            totpHolder.mView.setOnClickListener((View v) -> {
                if (openCode == position) {
                    openCode = -1;
                }
                else {
                    openCode = position;
                }
                notifyDataSetChanged();
            });
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

            public ImageView icon;
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

                icon = mView.findViewById(R.id.icon);
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
                view.setOnLongClickListener(this::showPopupMenu);
            }

            private void copyOTPToClipboard() {
                try {
                    ClipboardManager clipboardManager = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    clipboardManager.setPrimaryClip(ClipData.newPlainText("TOTP Code", account.getOtp()));
                    Toast.makeText(getContext(), "Code copied to clipboard", Toast.LENGTH_SHORT).show();
                    //TODO: clear code on expiry (requires API 28)
                }
                catch (CryptoException e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), "Failed to copy code to clipboard", Toast.LENGTH_SHORT).show();
                }
            }

            public void hideCode() {
                codeShown = false;
                issuer.setVisibility(View.VISIBLE);
                username.setVisibility(View.VISIBLE);
                otp.setVisibility(View.INVISIBLE);
                progressBar.setVisibility(View.INVISIBLE);
                showCodeButton.setText("Show");
                showCodeButton.getBackground().setTint(appGreen);
                showCodeButton.setTextColor(appGreen);
                updater.removeCallbacks(updaterTask);
            }

            private void openDeleteDialog() {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setMessage("Are you sure you want to delete this Backup Code?");
                builder.setNegativeButton("Cancel", (dialog, id) -> {
                    return;
                });
                builder.setPositiveButton("Delete", (dialog, id) -> {
                    try {
                        TOTP.deleteTOTPAccount(getContext(), account);
                    }
                    catch (SQLException e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "An error occurred", Toast.LENGTH_SHORT).show();
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
                Button delete = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                delete.setTextColor(getContext().getColor(R.color.appReject));
            }

            private boolean showPopupMenu(View v) {
                PopupMenu popupMenu = new PopupMenu(getContext(), showCodeButton);
                popupMenu.inflate(R.menu.totp_item_menu);
                popupMenu.setOnMenuItemClickListener((MenuItem item) -> {
                    int id = item.getItemId();
                    switch (id) {
                        case R.id.copyOTPToClipboard:
                            copyOTPToClipboard();
                            return true;
                        case R.id.removeAccount:
                            openDeleteDialog();
                            return true;
                    }
                    return true;
                });
                popupMenu.show();
                return true;
            }

            public void showCode() {
                codeShown = true;
                issuer.setVisibility(View.INVISIBLE);
                username.setVisibility(View.INVISIBLE);
                otp.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.VISIBLE);
                showCodeButton.setText("Hide");
                showCodeButton.getBackground().setTint(appWarning);
                showCodeButton.setTextColor(appWarning);
                updater.post(updaterTask);
            }

            private void toggleView(View v) {
                codeShown = !codeShown;
                if (codeShown) {
                    showCode();
                }
                else {
                    hideCode();
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
                icon.setImageResource(Icons.getIconFromTOTPIssuer(account.issuer));
                issuer.setText(account.issuer);
                username.setText(account.getUsername());
                updateOtp();
                progressBar.setMax(account.period*100);
            }
        }
    }
}
