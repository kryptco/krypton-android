package co.krypt.krypton.team;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import co.krypt.krypton.R;
import co.krypt.krypton.team.TeamService.C;
import co.krypt.krypton.team.home.HomeFragment;
import co.krypt.krypton.uiutils.Transitions;

public class TeamFragment extends Fragment {
    private final String TAG = "TeamFragment";

    public TeamFragment() {
    }

    public static TeamFragment newInstance() {
        return new TeamFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_teams, container, false);

        EventBus.getDefault().register(this);
        EventBus.getDefault().post(new TeamService.GetTeamHomeData(C.background(getContext())));
        return rootView;
    }

    @Override
    public void onDestroyView() {
        EventBus.getDefault().unregister(this);
        super.onDestroyView();
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onTeamHomeData(TeamService.GetTeamHomeDataResult result) {
        Fragment f = getFragmentManager().findFragmentById(R.id.fragment_teams);
        if (result.r.success != null) {
            if (f != null && f instanceof HomeFragment) {
                return;
            }
            Transitions.beginFade(this)
                    .replace(R.id.fragment_teams, new HomeFragment()).commitAllowingStateLoss();
        } else {
            if (f != null && f instanceof IntroFragment) {
                return;
            }
            Transitions.beginFade(this)
                    .replace(R.id.fragment_teams, new IntroFragment()).commitAllowingStateLoss();
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser && getContext() != null) {
            EventBus.getDefault().post(new TeamService.GetTeamHomeData(C.background(getContext())));
        }
    }
}
