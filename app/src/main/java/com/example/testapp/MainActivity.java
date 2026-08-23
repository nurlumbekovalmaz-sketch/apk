package com.example.testapp;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView textView = new TextView(this);
        textView.setText("Привет! Облачная сборка APK работает.");
        textView.setPadding(48, 96, 48, 48);

        setContentView(textView);
    }
}
