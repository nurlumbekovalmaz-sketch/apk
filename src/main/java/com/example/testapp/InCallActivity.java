package com.example.testapp;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telecom.Call;
import android.telecom.VideoProfile;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class InCallActivity extends Activity {

    private TextView nameView;
    private TextView stateView;
    private Button answerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 120, 48, 48);

        nameView = new TextView(this);
        nameView.setTextSize(24);

        stateView = new TextView(this);
        stateView.setTextSize(16);
        stateView.setPadding(0, 16, 0, 48);

        answerButton = new Button(this);
        answerButton.setText("Ответить");
        answerButton.setOnClickListener(v -> {
            Call call = MyInCallService.activeCall;
            if (call != null) call.answer(VideoProfile.STATE_AUDIO_ONLY);
        });

        Button hangupButton = new Button(this);
        hangupButton.setText("Отбой");
        hangupButton.setOnClickListener(v -> {
            Call call = MyInCallService.activeCall;
            if (call != null) call.disconnect();
            finish();
        });

        layout.addView(nameView);
        layout.addView(stateView);
        layout.addView(answerButton);
        layout.addView(hangupButton);
        setContentView(layout);
    }

    @Override
    protected void onResume() {
        super.onResume();
        MyInCallService.ui = this;
        refresh();
    }

    @Override
    protected void onPause() {
        super.onPause();
        MyInCallService.ui = null;
    }

    public void refresh() {
        Call call = MyInCallService.activeCall;
        if (call == null) {
            finish();
            return;
        }
        String number = MyInCallService.numberOf(call);
        String name = lookupContactName(number);
        nameView.setText(name != null ? name : (number != null ? number : "Неизвестный номер"));
        stateView.setText(stateText(call.getState()));
        answerButton.setVisibility(
                call.getState() == Call.STATE_RINGING ? View.VISIBLE : View.GONE);
    }

    public void finishIfNoCall() {
        if (MyInCallService.activeCall == null) finish();
    }

    private String stateText(int state) {
        switch (state) {
            case Call.STATE_RINGING: return "Входящий звонок…";
            case Call.STATE_DIALING: return "Звоним…";
            case Call.STATE_ACTIVE: return "Разговор";
            case Call.STATE_DISCONNECTED: return "Звонок завершён";
            default: return "Звонок";
        }
    }

    private String lookupContactName(String number) {
        if (number == null) return null;
        Uri uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(number));
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(uri,
                    new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME},
                    null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(0);
            }
        } catch (SecurityException ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }
        return null;
    }
}
