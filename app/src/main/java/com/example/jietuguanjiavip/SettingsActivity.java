package com.example.jietuguanjiavip;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class SettingsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        EditText etUsername = findViewById(R.id.et_username);
        EditText etExpireTime = findViewById(R.id.et_expire_time);
        Button btnSave = findViewById(R.id.btn_save);

        SharedPreferences pref = getSharedPreferences("settings", Context.MODE_PRIVATE);
        etUsername.setText(pref.getString("username", "谁明浪子心"));
        etExpireTime.setText(pref.getString("expire_time", "2099-12-31 23:59:59"));

        btnSave.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String expireTime = etExpireTime.getText().toString().trim();

            if (username.isEmpty()) username = "谁明浪子心";
            if (expireTime.isEmpty()) expireTime = "2099-12-31 23:59:59";

            pref.edit()
                    .putString("username", username)
                    .putString("expire_time", expireTime)
                    .apply();

            Toast.makeText(this, "保存成功！重启截图管家后生效", Toast.LENGTH_SHORT).show();
        });
    }
}
