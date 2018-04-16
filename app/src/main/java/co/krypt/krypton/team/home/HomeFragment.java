package co.krypt.krypton.team.home;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import co.krypt.krypton.R;
import co.krypt.krypton.policy.Approval;
import co.krypt.krypton.policy.Policy;
import co.krypt.krypton.team.IntroFragment;
import co.krypt.krypton.team.Sigchain;
import co.krypt.krypton.team.TeamService;
import co.krypt.krypton.team.TeamService.C;
import co.krypt.krypton.team.billing.Billing;
import co.krypt.krypton.team.invite.CreateInviteDialogFragment;
import co.krypt.krypton.transport.SNSTransport;
import co.krypt.krypton.uiutils.TimeUtils;
import co.krypt.krypton.uiutils.Transitions;
import mobi.upod.timedurationpicker.TimeDurationPickerDialog;

public class HomeFragment extends Fragment {
    private static final String TAG = "TeamHomeFragment";

    private AppCompatEditText teamName;

    private AppCompatTextView email;
    private AppCompatTextView adminText;
    private AppCompatTextView membersText;
    private AppCompatTextView pinnedHostsText;
    private AppCompatTextView activityText;

    private AppCompatTextView tempApprovalText;
    private AppCompatImageButton tempApprovalButton;

    private SwitchCompat auditLoggingSwitch;
    private AppCompatTextView auditLoggingText;

    private AppCompatButton leaveTeamButton;

    private AppCompatButton inviteButton;
    private AppCompatButton cancelInvitesButton;

    private SwipeRefreshLayout swipeRefreshLayout;

    private AppCompatTextView billingPlan;
    private AppCompatTextView memberUsageCurrent;
    private AppCompatTextView memberUsageLimit;
    private AppCompatTextView hostsUsageCurrent;
    private AppCompatTextView hostsUsageLimit;
    private AppCompatTextView logsUsageCurrent;
    private AppCompatTextView logsUsageLimit;
    private AppCompatTextView manageBillingButton;
    private ConstraintLayout billingContainer;

    public HomeFragment() {
    }

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_teams_home, container, false);

        new Thread(() -> SNSTransport.getInstance(getContext()).registerWithTeamServer()).start();

        ConstraintLayout membersCell = rootView.findViewById(R.id.membersContainer);
        membersCell.setOnClickListener(v -> {
            Transitions.beginSlide(this)
                    .replace(R.id.fragmentOverlay, new MembersFragment())
                    .addToBackStack(null)
                    .commitAllowingStateLoss();
        });

        ConstraintLayout pinnedHostsCell = rootView.findViewById(R.id.pinnedHostsCell);
        pinnedHostsCell.setOnClickListener(v -> {
            Transitions.beginSlide(this)
                    .replace(R.id.fragmentOverlay, new PinHostsFragment())
                    .addToBackStack(null)
                    .commitAllowingStateLoss();
        });

        ConstraintLayout activityCell = rootView.findViewById(R.id.activityCell);
        activityCell.setOnClickListener(v -> {
            Transitions.beginSlide(this)
                    .replace(R.id.fragmentOverlay, new BlocksFragment())
                    .addToBackStack(null)
                    .commitAllowingStateLoss();
        });

        teamName = rootView.findViewById(R.id.teamName);
        email = rootView.findViewById(R.id.email);
        adminText = rootView.findViewById(R.id.adminLabel);
        membersText = rootView.findViewById(R.id.membersContainerText);
        pinnedHostsText = rootView.findViewById(R.id.pinnedHostsText);
        activityText = rootView.findViewById(R.id.activityText);

        tempApprovalText = rootView.findViewById(R.id.tempApproveLabel);
        tempApprovalButton = rootView.findViewById(R.id.tempApprovalButton);
        auditLoggingSwitch = rootView.findViewById(R.id.auditLogSwitch);
        auditLoggingText = rootView.findViewById(R.id.auditLogLabel);
        inviteButton = rootView.findViewById(R.id.inviteButton);

        cancelInvitesButton = rootView.findViewById(R.id.cancelInvitesButton);
        cancelInvitesButton.setOnClickListener(b -> {
            EventBus.getDefault().post(new TeamService.RequestTeamOperation(
                    Sigchain.RequestableTeamOperation.cancelInvite(),
                    C.withConfirmStatus(getActivity())
                    )
            );
        });

        leaveTeamButton = rootView.findViewById(R.id.moreSettingsButton);
        leaveTeamButton.setOnClickListener(v -> {
            EventBus.getDefault().post(new TeamService.RequestTeamOperation(
                            Sigchain.RequestableTeamOperation.leave(),
                            C.withConfirmStatusCallback(getActivity(), r -> {
                                EventBus.getDefault().removeAllStickyEvents();
                                getFragmentManager().beginTransaction().replace(R.id.fragment_teams, new IntroFragment()).commitAllowingStateLoss();
                            })
                    )
            );
        });

        swipeRefreshLayout = rootView.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            swipeRefreshLayout.setRefreshing(true);
            scheduleUpdate(getContext());
        });

        billingPlan = rootView.findViewById(R.id.billingPlan);
        memberUsageCurrent = rootView.findViewById(R.id.membersUsageCurrent);
        memberUsageLimit = rootView.findViewById(R.id.membersUsageLimit);
        hostsUsageCurrent = rootView.findViewById(R.id.hostsUsageCurrent);
        hostsUsageLimit = rootView.findViewById(R.id.hostsUsageLimit);
        logsUsageCurrent = rootView.findViewById(R.id.logsUsageCurrent);
        logsUsageLimit = rootView.findViewById(R.id.logsUsageLimit);
        manageBillingButton = rootView.findViewById(R.id.manageBillingButton);
        billingContainer = rootView.findViewById(R.id.billingContainer);
        hideBilling();

        EventBus.getDefault().register(this);
        EventBus.getDefault().post(new TeamService.UpdateTeamHomeDataIfOutdated(getContext()));
        return rootView;
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void updateUI(TeamService.UpdateTeamHomeDataResult result) {
        if (result.r.success == null) {
            swipeRefreshLayout.setRefreshing(false);
            return;
        }
        Sigchain.TeamHomeData teamHomeData = result.r.success;
        updateUI(teamHomeData);
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void updateUI(TeamService.GetTeamHomeDataResult result) {
        if (result.r.success == null) {
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        Sigchain.TeamHomeData teamHomeData = result.r.success;
        // Update BillingInfo first time only
        if (teamHomeData.is_admin) {
            if (EventBus.getDefault().getStickyEvent(TeamService.BillingInfoResult.class) == null) {
                EventBus.getDefault().post(new TeamService.RequestBillingInfo(getContext()));
            }
        }

        updateUI(teamHomeData);
    }

    private void updateUI(Sigchain.TeamHomeData teamHomeData) {
        swipeRefreshLayout.setRefreshing(false);

        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

        teamName.setText(teamHomeData.name);
        email.setText(teamHomeData.email);

        if (teamHomeData.is_admin) {
            adminText.setAlpha(1);
            tempApprovalButton.setVisibility(View.VISIBLE);
            inviteButton.setVisibility(View.VISIBLE);
        } else {
            adminText.setAlpha(0);
            tempApprovalButton.setVisibility(View.INVISIBLE);
            inviteButton.setVisibility(View.GONE);
        }
        teamName.setEnabled(teamHomeData.is_admin);
        tempApprovalButton.setEnabled(teamHomeData.is_admin);
        auditLoggingSwitch.setEnabled(teamHomeData.is_admin);
        inviteButton.setEnabled(teamHomeData.is_admin);

        if (teamHomeData.nOpenInvites > 0 && teamHomeData.is_admin) {
            cancelInvitesButton.setVisibility(View.VISIBLE);
            cancelInvitesButton.setEnabled(true);
            cancelInvitesButton.setText("Close " + teamHomeData.nOpenInvites + " Active Invitations");
        } else {
            cancelInvitesButton.setVisibility(View.GONE);
            cancelInvitesButton.setEnabled(false);
        }

        int activeMemberCount = 0;
        for (Sigchain.Member member: teamHomeData.members) {
            if (!member.is_removed) {
                activeMemberCount++;
            }
        }
        membersText.setText(activeMemberCount + " members");
        pinnedHostsText.setText(teamHomeData.pinnedHosts.length + " pinned hosts");
        activityText.setText(teamHomeData.nBlocks + " events");

        teamName.setSingleLine();
        teamName.setOnEditorActionListener((v, actionId, event) -> {
            teamName.clearFocus();
            if (imm != null) {
                imm.hideSoftInputFromWindow(teamName.getWindowToken(), 0);
            }
            if (v.getText().toString().equals(teamHomeData.name)) {
                return true;
            }
            Sigchain.RequestableTeamOperation request = Sigchain.RequestableTeamOperation.setTeamInfo(
                    new Sigchain.TeamInfo(v.getText().toString())
            );

            EventBus.getDefault().post(
                    new TeamService.RequestTeamOperation(
                            request,
                            C.withConfirmStatusCallback(
                                    getActivity(),
                                    r -> EventBus.getDefault().post(new TeamService.UpdateTeamHomeData(getContext()))
                            )
                    ));
            return true;
        });

        if (teamHomeData.temporaryApprovalSeconds != null) {
            if (teamHomeData.temporaryApprovalSeconds == 0) {
                tempApprovalText.setText("Disabled");
            } else {
                tempApprovalText.setText(TimeUtils.formatDurationSeconds(teamHomeData.temporaryApprovalSeconds));
            }
        } else {
            tempApprovalText.setText("Unrestricted");
        }
        Runnable editTempApproval = () -> {
            new TimeDurationPickerDialog(
                    getContext(),
                    (v_, d) -> {
                        new Thread(() -> {
                            EventBus.getDefault().post(
                                    new TeamService.RequestTeamOperation(
                                            new Sigchain.RequestableTeamOperation(new Sigchain.Policy(d/1000)),
                                            C.withConfirmStatus(getActivity())
                                    ));

                            scheduleUpdate(getContext());
                        }).start();
                    },
                    (Policy.temporaryApprovalSeconds(getContext(), Approval.ApprovalType.SSH_USER_HOST)) * 1000
            ).show();
        };
        if (teamHomeData.is_admin) {
            tempApprovalButton.setOnClickListener(v -> editTempApproval.run());
            tempApprovalText.setOnClickListener(v -> editTempApproval.run());
        } else {
            tempApprovalButton.setOnClickListener(null);
            tempApprovalText.setOnClickListener(null);
        }

        auditLoggingSwitch.setChecked(teamHomeData.auditLogsEnabled);
        if (teamHomeData.auditLogsEnabled) {
            auditLoggingText.setText("Enabled");
        } else {
            auditLoggingText.setText("Disabled");
        }

        inviteButton.setOnClickListener(v -> {
            getFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
                    .replace(R.id.fragmentOverlay, CreateInviteDialogFragment.newInstance(teamHomeData.name, teamHomeData.email))
                    .addToBackStack(null)
                    .commitAllowingStateLoss();
        });
    }

    private void hideBilling() {
        billingContainer.getLayoutParams().height = 0;
        billingContainer.setAlpha(0f);
    }

    private void showBilling() {
        billingContainer.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
        billingContainer.setAlpha(1f);
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void updateBillingUI(TeamService.BillingInfoResult r) {
        TeamService.GetTeamHomeDataResult homeData = EventBus.getDefault().getStickyEvent(TeamService.GetTeamHomeDataResult.class);
        if (homeData == null || homeData.r.success == null) {
            Log.e(TAG, "no existing team home data");
            return;
        }
        Sigchain.TeamHomeData teamHomeData = homeData.r.success;
        if (!teamHomeData.is_admin) {
            hideBilling();
            return;
        }
        if (r.r.success == null) {
            Log.e(TAG, "error retrieving Billing: " + r.r.error);
            return;
        }
        Billing billing = r.r.success;

        memberUsageCurrent.setText(String.valueOf(billing.usage.members));
        hostsUsageCurrent.setText(String.valueOf(billing.usage.hosts));
        logsUsageCurrent.setText(String.valueOf(billing.usage.logsLast30Days));

        if (billing.currentTier.limit != null) {
            Billing.Limit limit = billing.currentTier.limit;
            if (limit.members == null) {
                memberUsageLimit.setText("∞");
            } else {
                memberUsageLimit.setText(String.valueOf(limit.members));
            }
            if (limit.hosts == null) {
                hostsUsageLimit.setText("∞");
            } else {
                hostsUsageLimit.setText(String.valueOf(limit.hosts));
            }
            if (limit.logsLast30Days == null) {
                logsUsageLimit.setText("∞");
            } else {
                logsUsageLimit.setText(String.valueOf(limit.logsLast30Days));
            }
        } else {
            memberUsageLimit.setText("∞");
            hostsUsageLimit.setText("∞");
            logsUsageLimit.setText("∞");
        }

        if (teamHomeData.billingUrl != null) {
            manageBillingButton.setOnClickListener(v -> {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(teamHomeData.billingUrl));
                startActivity(browserIntent);
            });
        }

        billingPlan.setText(billing.currentTier.name);
        if (billing.currentTier.price > 0) {
            ViewCompat.setBackgroundTintList(billingPlan, ContextCompat.getColorStateList(getContext(), R.color.appGreen));
            billingPlan.setTextColor(getContext().getResources().getColor(R.color.appGreen, null));
            manageBillingButton.setText("Manage Billing");
        } else {
            ViewCompat.setBackgroundTintList(billingPlan, ContextCompat.getColorStateList(getContext(), R.color.appWarning));
            billingPlan.setTextColor(getContext().getResources().getColor(R.color.appWarning, null));
            manageBillingButton.setText("Upgrade");
        }

        showBilling();
    }

    @Override
    public void onDestroyView() {
        EventBus.getDefault().unregister(this);
        super.onDestroyView();
    }

    private void scheduleUpdate(Context context) {
        EventBus.getDefault().post(new TeamService.UpdateTeamHomeData(context));
        EventBus.getDefault().post(new TeamService.RequestBillingInfo(context));
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
