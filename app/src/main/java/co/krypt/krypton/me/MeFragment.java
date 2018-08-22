package co.krypt.krypton.me;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.ListViewCompat;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import co.krypt.krypton.R;
import co.krypt.krypton.crypto.U2F;
import co.krypt.krypton.exception.CryptoException;
import co.krypt.krypton.protocol.Profile;
import co.krypt.krypton.silo.IdentityService;
import co.krypt.krypton.silo.Silo;
import co.krypt.krypton.u2f.KnownAppIds;
import co.krypt.krypton.uiutils.Error;

public class MeFragment extends Fragment {
    private static final String TAG = "MeFragment";
    private EditText profileEmail;
    private ListViewCompat accounts;
    private ArrayAdapter<U2F.KeyManager.Account> accountsAdapter;

    private SharedPreferences prefs;
    private static final String HIDDEN_ACCOUNTS_KEY = "HIDDEN_ACCOUNTS";

    public MeFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getContext().getSharedPreferences("ME_FRAGMENT_PREFERENCES", Context.MODE_PRIVATE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_me, container, false);

        accountsAdapter = new AccountsAdapter(getContext());
        accounts = v.findViewById(R.id.accounts);
        accounts.setAdapter(accountsAdapter);

        accounts.addHeaderView(inflater.inflate(R.layout.fragment_me_header, container, false));

        profileEmail = v.findViewById(R.id.profileEmail);
        profileEmail.setText("loading...");
        profileEmail.setTextColor(getResources().getColor(R.color.appGray, null));
        profileEmail.setOnEditorActionListener((v12, keyCode, event) -> {
            v12.clearFocus();
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v12.getWindowToken(), 0);
            onEmailChanged(v12.getText().toString());
            return false;
        });
        profileEmail.setOnFocusChangeListener((v1, hasFocus) -> {
            if (!hasFocus) {
                EditText editText = (EditText) v1;
                onEmailChanged(editText.getText().toString());
            }
        });

        EventBus.getDefault().register(this);
        EventBus.getDefault().post(new IdentityService.GetProfile(getContext()));
        updateAccounts(new IdentityService.AccountsUpdated());
        return v;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void updateName(IdentityService.GetProfileResult r) {
        Profile me = r.profile;
        if (me != null) {
            profileEmail.setText(me.email);
        } else {
            profileEmail.setText(MeStorage.getDeviceName());
            Log.i(TAG, "no profile");
        }
        profileEmail.setTextColor(getResources().getColor(R.color.appBlack, null));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void updateAccounts(IdentityService.AccountsUpdated _) {
        try {
            List<U2F.KeyManager.Account> securedAccounts = U2F.getAccounts(getContext());
            List<U2F.KeyManager.Account> filteredAccounts = new ArrayList<>();

            List<KnownAppIds.KnownAppId> unsecuredAppIds = new ArrayList<>(KnownAppIds.COMMON_APP_IDS);

            Set<String> hiddenAccounts = prefs.getStringSet(HIDDEN_ACCOUNTS_KEY, new HashSet<>());

            for (U2F.KeyManager.Account account: securedAccounts) {
                ListIterator<KnownAppIds.KnownAppId> unsecuredIter = unsecuredAppIds.listIterator();
                while (unsecuredIter.hasNext()) {
                    if (unsecuredIter.next().site.equals(account.name)) {
                        unsecuredIter.remove();
                    }
                }

                if (!hiddenAccounts.contains(account.keyHandleHash)) {
                    filteredAccounts.add(account);
                }
            }

            List<U2F.KeyManager.Account> displayAccounts = new ArrayList<>();
            for (KnownAppIds.KnownAppId unsecuredAppId: unsecuredAppIds) {
                if (!hiddenAccounts.contains(unsecuredAppId.site)) {
                    displayAccounts.add(new U2F.KeyManager.Account(unsecuredAppId.site, unsecuredAppId.logoSrc,false, null, null, null, unsecuredAppId.shortName));
                }
            }
            displayAccounts.addAll(filteredAccounts);

            accountsAdapter.clear();
            accountsAdapter.addAll(displayAccounts);
        } catch (CryptoException e) {
            e.printStackTrace();
        }
    }

    private void onEmailChanged(String email) {
        Context context = getContext();
        if (context == null){
            return;
        }

        Profile me = Silo.shared(getContext()).meStorage().load();
        if (me == null) {
            me = new Profile(email, null, null, null, null);
        }
        me.email = email;
        Silo.shared(getContext()).meStorage().set(me);
    }

    @Override
    public void onDestroyView() {
        EventBus.getDefault().unregister(this);
        super.onDestroyView();
    }

    @Override
    public void onStop() {
        super.onStop();
        profileEmail.setOnEditorActionListener(null);
    }

    private class AccountsAdapter extends ArrayAdapter<U2F.KeyManager.Account> {

        public AccountsAdapter(@NonNull Context context) {
            super(context, R.layout.account_item, R.id.accountName);
        }

        @NonNull
        public View getView(int position, View convertView, @NonNull ViewGroup container) {
            View v = super.getView(position, convertView, container);

            U2F.KeyManager.Account account = getItem(position);

            if (account == null) {
                return v;
            }

            AppCompatTextView name = v.findViewById(R.id.accountName);
            name.setText(account.name);

            AppCompatImageView logo = v.findViewById(R.id.logo);
            logo.setImageResource(account.logo);

            AppCompatTextView addedOn = v.findViewById(R.id.dateAdded);
            if (account.lastUsed != null) {
                String timeAdded = DateUtils.getRelativeTimeSpanString(account.lastUsed.getTime(), System.currentTimeMillis(), 1000).toString();
                addedOn.setText("logged in " + timeAdded);
            } else if (account.added != null) {
                String timeAdded = DateUtils.getRelativeTimeSpanString(account.added.getTime(), System.currentTimeMillis(), 1000).toString();
                addedOn.setText("added " + timeAdded);
            } else {
                addedOn.setText("not set up");
            }

            AppCompatTextView fixText = v.findViewById(R.id.fixText);
            AppCompatImageView checkmark = v.findViewById(R.id.securedIcon);
            if (account.secured) {
                checkmark.setVisibility(View.VISIBLE);
                fixText.setVisibility(View.INVISIBLE);
            } else {
                checkmark.setVisibility(View.INVISIBLE);
                fixText.setVisibility(View.VISIBLE);
            }
            fixText.setEnabled(!account.secured);
            fixText.setOnClickListener(v_ -> Error.longToast(getContext(), "Go to https://krypt.co/start to secure this account with Krypton."));

            v.setOnLongClickListener(v_ -> {
                new AlertDialog.Builder(getContext())
                        .setMessage("Hide " + account.name + "?")
                        .setPositiveButton("Hide", (dialog, which) -> {
                            Set<String> hiddenAccounts = new HashSet<>(prefs.getStringSet(HIDDEN_ACCOUNTS_KEY, new HashSet<>()));

                            if (account.keyHandleHash != null) {
                                hiddenAccounts.add(account.keyHandleHash);
                            } else {
                                hiddenAccounts.add(account.name);
                            }

                            prefs.edit().putStringSet(HIDDEN_ACCOUNTS_KEY, hiddenAccounts).commit();
                            EventBus.getDefault().post(new IdentityService.AccountsUpdated());
                        })
                        .setNeutralButton("Cancel", (d, w) -> {})
                        .create().show();
                return true;
            });

            return v;
        }
    }
}
