package com.example.testapp;

import android.Manifest;
import android.app.Activity;
import android.app.role.RoleManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telecom.TelecomManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private TelecomManager telecom;
    private TextView statusView;
    private ListView contactsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        telecom = (TelecomManager) getSystemService(TELECOM_SERVICE);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 96, 48, 48);

        statusView = new TextView(this);
        statusView.setTextSize(16);

        EditText numberInput = new EditText(this);
        numberInput.setHint("Номер, например +79001234567");

        Button callButton = new Button(this);
        callButton.setText("Позвонить");
        callButton.setOnClickListener(v -> placeCall(numberInput.getText().toString()));

        Button defaultButton = new Button(this);
        defaultButton.setText("Сделать звонилкой по умолчанию");
        defaultButton.setOnClickListener(v -> requestDefaultDialer());

        TextView contactsTitle = new TextView(this);
        contactsTitle.setText("Контакты:");
        contactsTitle.setPadding(0, 24, 0, 8);

        contactsList = new ListView(this);

        layout.addView(statusView);
        layout.addView(numberInput);
        layout.addView(callButton);
        layout.addView(defaultButton);
        layout.addView(contactsTitle);
        layout.addView(contactsList);
        setContentView(layout);

        ensurePermissions();
        fillContacts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isDefaultDialer()) {
            statusView.setText("Статус: это приложение — звонилка по умолчанию");
        } else {
            statusView.setText("Статус: звонилка по умолчанию — системная. Нажми кнопку ниже.");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        recreate(); // после выдачи разрешений перезапускаем экран, чтобы подтянулись контакты
    }

    private void ensurePermissions() {
        List<String> needed = new ArrayList<>();
        if (checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.CALL_PHONE);
        }
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.READ_CONTACTS);
        }
        if (!needed.isEmpty()) {
            requestPermissions(needed.toArray(new String[0]), 1);
        }
    }

    private void fillContacts() {
        Cursor cursor = getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{
                        ContactsContract.CommonDataKinds.Phone._ID,
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                },
                null, null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC");

        if (cursor != null) {
            SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
                    android.R.layout.simple_list_item_2,
                    cursor,
                    new String[]{
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                            ContactsContract.CommonDataKinds.Phone.NUMBER
                    },
                    new int[]{android.R.id.text1, android.R.id.text2},
                    0);
            contactsList.setAdapter(adapter);
            contactsList.setOnItemClickListener((parent, view, position, id) -> {
                Cursor c = (Cursor) parent.getItemAtPosition(position);
                String num = c.getString(c.getColumnIndexOrThrow(
                        ContactsContract.CommonDataKinds.Phone.NUMBER));
                placeCall(num);
            });
        }
    }

    private boolean isDefaultDialer() {
        return getPackageName().equals(telecom.getDefaultDialerPackage());
    }

    private void requestDefaultDialer() {
        if (Build.VERSION.SDK_INT >= 29) {
            RoleManager roleManager = (RoleManager) getSystemService(ROLE_SERVICE);
            if (roleManager != null) {
                startActivityForResult(
                        roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER), 2);
                return;
            }
        }
        // Android 9 и ниже
        Intent intent = new Intent("android.telecom.action.CHANGE_DEFAULT_DIALER");
        intent.putExtra("android.telecom.extra.CHANGE_DEFAULT_DIALER_PACKAGE_NAME",
                getPackageName());
        startActivity(intent);
    }

    private void placeCall(String rawNumber) {
        String number = rawNumber == null ? "" : rawNumber.trim();
        if (number.isEmpty()) return;
        Uri uri = Uri.parse("tel:" + number);

        if (isDefaultDialer()) {
            // Мы звонилка по умолчанию — звоним через системный телеком
            try {
                telecom.placeCall(uri, null);
                return;
            } catch (Throwable t) {
                // на очень старых API placeCall может не быть — идём ниже
            }
            Intent intent = new Intent(Intent.ACTION_CALL, uri);
            intent.setPackage("com.android.server.telecom");
            try {
                startActivity(intent);
                return;
            } catch (Exception e) {
                startActivity(new Intent(Intent.ACTION_DIAL, uri));
            }
        } else {
            // Пока не мы — пусть звонит системная звонилка
            try {
                startActivity(new Intent(Intent.ACTION_CALL, uri));
            } catch (SecurityException e) {
                startActivity(new Intent(Intent.ACTION_DIAL, uri));
            }
        }
    }
}
