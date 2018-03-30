package co.krypt.krypton.team.invite;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.ListViewCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

import co.krypt.krypton.R;
import co.krypt.krypton.team.Sigchain;
import co.krypt.krypton.team.TeamService;
import co.krypt.krypton.team.TeamService.C;
import co.krypt.krypton.uiutils.Email;

public class SelectIndividualsFragment extends Fragment {
    private final String TAG = "CreateInviteDialog";

    public SelectIndividualsFragment() {
    }

    public static SelectIndividualsFragment newInstance() {
        return new SelectIndividualsFragment();
    }

    AppCompatButton createButton;
    AppCompatButton cancelButton;

    Sigchain.IndirectInvitationRestriction inviteRestriction = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_teams_individual_emails_invite, container, false);

        ArrayAdapter emailsAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, new ArrayList<>());
        ListViewCompat emailsList = rootView.findViewById(R.id.emailResultsList);
        AppCompatEditText emailText = rootView.findViewById(R.id.emailAddInput);
        AppCompatImageButton addButton = rootView.findViewById(R.id.addEmailButton);
        createButton = rootView.findViewById(R.id.createIndividualLink);
        cancelButton = rootView.findViewById(R.id.cancelIndividualLinkButton);

        emailsList.setAdapter(emailsAdapter);

        addButton.setEnabled(false);
        addButton.setOnClickListener(v -> {
            emailsAdapter.add(emailText.getText().toString());
            emailText.setText("");

            createButton.setEnabled(emailsAdapter.getCount() > 0);
        });

        Email.colorValidEmail(emailText);
        emailText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                addButton.setEnabled(Email.verifyEmailPattern.matcher(s.toString()).matches());
            }
        });

        createButton.setEnabled(false);
        createButton.setOnClickListener(v -> {

            List<String> emails = new ArrayList<String>();
            for (int i = 0; i < emailsAdapter.getCount(); i++) {
                emails.add((String) emailsAdapter.getItem(i));
            }

            inviteRestriction = new Sigchain.IndirectInvitationRestriction(emails.toArray(new String[0]));
            EventBus.getDefault().post(new TeamService.RequestTeamOperation(
                    new Sigchain.RequestableTeamOperation(inviteRestriction),
                    C.withConfirmStatusCallback(
                            getActivity(),
                            this::onCreateInvite
                    )));
        });

        cancelButton.setOnClickListener(v -> {
            getFragmentManager().popBackStack();
        });

        return rootView;
    }

    @Override
    public void onDestroyView() {
        createButton = null;
        cancelButton = null;
        super.onDestroyView();
    }

    private void onCreateInvite(Sigchain.NativeResult<Sigchain.TeamOperationResponse> r) {
        if (createButton == null) {
            return;
        }
        TeamService.UpdateTeamHomeDataResult teamHomeDataResult = EventBus.getDefault().getStickyEvent(TeamService.UpdateTeamHomeDataResult.class);
        if (teamHomeDataResult.r.success == null) {
            return;
        }
        Sigchain.TeamHomeData teamHomeData = teamHomeDataResult.r.success;
        if (r.success != null && r.success.data != null && r.success.data.inviteLink != null) {
            String invite = r.success.data.inviteLink;

            getContext().startActivity(
                    Format.shareInviteIntent(getContext(), invite, inviteRestriction.emails, teamHomeData)
            );
            cancelButton.setText("Done");
            cancelButton.setOnClickListener(v -> {
                getFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
                        .remove(this)
                        .commitAllowingStateLoss();
                getFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            });
            createButton.setText("Resend");
        }
    }


}
