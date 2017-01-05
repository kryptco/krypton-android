package co.krypt.kryptonite.onboarding;


import android.animation.LayoutTransition;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.transition.Fade;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.VideoView;

import co.krypt.kryptonite.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class GeneratingFragment extends Fragment {
    private static final String TAG = "GeneratingFragment";

    private VideoView anim;


    public GeneratingFragment() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_generating, container, false);
        final View animationContainer = root.findViewById(R.id.animationContainer);
        this.anim = (VideoView) root.findViewById(R.id.generateAnimation);
        final VideoView anim = this.anim;
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
        anim.seekTo(0);

        return root;
    }

    public void onGenerate() {
        anim.post(new Runnable() {
            @Override
            public void run() {
                final int duration = 500;
                ColorDrawable[] color = {new ColorDrawable(Color.TRANSPARENT), new ColorDrawable(Color.WHITE)};
                TransitionDrawable transition = new TransitionDrawable(color);
                anim.setBackground(transition);
                transition.startTransition(duration);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onStop() {
        anim.setVisibility(View.GONE);
//        anim.setBackgroundColor(Color.WHITE);
        super.onStop();
    }
}
