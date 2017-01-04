package co.krypt.kryptonite.onboarding;


import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatImageView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.VideoView;

import co.krypt.kryptonite.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class GeneratingFragment extends Fragment {
    private static final String TAG = "GeneratingFragment";


    public GeneratingFragment() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_generating, container, false);
        final View animationContainer = root.findViewById(R.id.animationContainer);
        final VideoView anim = (VideoView) root.findViewById(R.id.generateAnimation);
        anim.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                anim.post(new Runnable() {
                    @Override
                    public void run() {
                        anim.start();
                    }
                });
            }
        });
        anim.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer mp, int what, int extra) {
                if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                    // video started
                    animationContainer.setVisibility(View.VISIBLE);
                    return true;
                }
                return false;
            }
        });
        anim.setVideoURI(Uri.parse("android.resource://"+getActivity().getPackageName()+"/"+R.raw.generate_animation));
        return root;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        AppCompatImageView image = (AppCompatImageView) view.findViewById(R.id.generatingImage);
        final AnimatorSet tickAnimator = (AnimatorSet) AnimatorInflater.loadAnimator(getContext(), R.animator.rotate_tick);
        tickAnimator.setTarget(image);
        tickAnimator.addListener(new AnimatorListenerAdapter() {
            //  repeat
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                tickAnimator.start();
            }
        });
        tickAnimator.setInterpolator(new OvershootInterpolator());
        image.setRotation(0);
        tickAnimator.start();
    }
}
