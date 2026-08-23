package com.example.testapp;

import android.content.Intent;
import android.net.Uri;
import android.telecom.Call;
import android.telecom.InCallService;

public class MyInCallService extends InCallService {

    public static Call activeCall;
    public static InCallActivity ui;

    private final Call.Callback callback = new Call.Callback() {
        @Override
        public void onStateChanged(Call call, int state) {
            if (ui != null) ui.runOnUiThread(ui::refresh);
        }
    };

    @Override
    public void onCallAdded(Call call) {
        activeCall = call;
        call.addCallback(callback);

        Intent intent = new Intent(this, InCallActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void onCallRemoved(Call call) {
        call.removeCallback(callback);
        if (activeCall == call) activeCall = null;
        if (ui != null) ui.runOnUiThread(ui::finishIfNoCall);
    }

    public static String numberOf(Call call) {
        Uri handle = call.getDetails().getHandle();
        return handle != null ? handle.getSchemeSpecificPart() : null;
    }
}
