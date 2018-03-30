package co.krypt.krypton.uiutils;

import android.content.Context;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.TextWatcher;

import java.util.regex.Pattern;

import co.krypt.krypton.R;

/**
 * Created by Kevin King on 1/30/18.
 * Copyright 2018. KryptCo, Inc.
 */

public class Email {

    public static final Pattern verifyEmailPattern = Pattern.compile("^.+@.+\\..+$");

    public static final void colorValidEmail(AppCompatEditText editText) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                Context context = editText.getContext();
                if (Email.verifyEmailPattern.matcher(editText.getText()).matches()) {
                    editText.setTextColor(context.getColor(R.color.appGreen));
                } else {
                    editText.setTextColor(context.getColor(R.color.appGray));
                }
            }
        });
    }
}
