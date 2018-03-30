package co.krypt.krypton.team.invite;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.AppCompatTextView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.greenrobot.eventbus.EventBus;

import co.krypt.krypton.R;
import co.krypt.krypton.team.Sigchain;
import co.krypt.krypton.team.TeamService;
import co.krypt.krypton.team.invite.inperson.admin.AdminQR;
import co.krypt.krypton.uiutils.Transitions;

public class CreateInviteDialogFragment extends Fragment {
    private final String TAG = "CreateInviteDialog";

    public CreateInviteDialogFragment() {
    }

    public static CreateInviteDialogFragment newInstance(String teamName, String myEmail) {
        CreateInviteDialogFragment fragment = new CreateInviteDialogFragment();

        Bundle args = new Bundle();
        args.putString("teamName", teamName);
        args.putString("myEmail", myEmail);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_teams_invite_modal, container, false);

        String myEmail = getArguments().getString("myEmail");

        //TODO: filter on common email endings and hide team link in that case
        String[] emailToks = myEmail.split("@");
        AppCompatImageButton teamLinkButton = rootView.findViewById(R.id.teamLinkButton);
        if (emailToks.length == 2) {
            String domain = emailToks[1];
            AppCompatTextView teamLinkDetail = rootView.findViewById(R.id.teamEmailLinkDetail);
            teamLinkDetail.setText("Anyone with an @" + domain + " email address");
            teamLinkButton.setOnClickListener(v -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("Create a team-only invite link?")
                        .setMessage("This will create a secret invite link that can only be used by people with an @" + domain + " email address.")
                        .setPositiveButton("Yes", (dialog, which) -> {

                            Sigchain.RequestableTeamOperation request = new Sigchain.RequestableTeamOperation(
                                    new Sigchain.IndirectInvitationRestriction(domain)
                            );

                            EventBus.getDefault().post(
                                    new TeamService.RequestTeamOperation(
                                            request,
                                            TeamService.C.withStatusCallback(
                                                    getActivity(),
                                                    this::onCreateInvite)
                                    ));

                            getFragmentManager().beginTransaction()
                                    .remove(this)
                                    .commitAllowingStateLoss();
                        })
                        .setNegativeButton("No", (dialog, which) -> {

                            getFragmentManager().beginTransaction()
                                    .remove(this)
                                    .commitAllowingStateLoss();
                        })
                        .show();
            });
        } else {
            // TODO: hide teamLinkButton if no domain on email or user has a common domain like gmail
        }

        AppCompatImageButton individualLinkButton = rootView.findViewById(R.id.individualsLinkButton);
        individualLinkButton.setOnClickListener(v -> {

            getFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right)
                    .replace(R.id.fragmentOverlay, SelectIndividualsFragment.newInstance())
                    .addToBackStack(null)
                    .commitAllowingStateLoss();
        });

        AppCompatImageButton inpersonButton = rootView.findViewById(R.id.inPersonButton);
        inpersonButton.setOnClickListener(v -> {
            Transitions.beginFade(this)
                    .replace(R.id.fragmentOverlay, new AdminQR())
                    .addToBackStack(null)
                    .commitAllowingStateLoss();
        });

        return rootView;
    }

    private void onCreateInvite(Sigchain.NativeResult<Sigchain.TeamOperationResponse> r) {
        if (getContext() == null) {
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
                    Format.shareInviteIntent(getContext(), invite, new String[] {}, teamHomeData)
            );
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}
