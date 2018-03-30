package co.krypt.krypton.team.invite;

import android.content.Context;
import android.content.Intent;
import android.text.Html;

import co.krypt.krypton.R;
import co.krypt.krypton.team.Sigchain;

/**
 * Created by Kevin King on 2/6/18.
 * Copyright 2018. KryptCo, Inc.
 */

public class Format {

    public static Intent shareInviteIntent(Context context, String inviteLink, String[] to, Sigchain.TeamHomeData teamHomeData) {
        String template = context.getString(R.string.teams_invite_template);
        template = template.replaceAll("TEAM_NAME", teamHomeData.name)
                .replaceAll("APP_NAME", context.getString(R.string.app_name))
                .replaceAll("INVITE_LINK", inviteLink);

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(template));
        sendIntent.setType("text/plain");
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Join Team " + teamHomeData.name + " on Krypton");
        sendIntent.putExtra(Intent.EXTRA_BCC, to);
        sendIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {teamHomeData.email});

        return Intent.createChooser(sendIntent, "Share your secret team invite link");
    }
}
