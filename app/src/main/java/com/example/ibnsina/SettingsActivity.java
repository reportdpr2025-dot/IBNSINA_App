package com.example.ibnsina;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

public class SettingsActivity extends AppCompatActivity {

    // ফিল্ডগুলো ডিক্লেয়ার করা
    EditText etOldPass, etNewPass, etConfirmPass;
    Button btnUpdatePass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // ভিউগুলো কানেক্ট করা
        etOldPass = findViewById(R.id.etOldPass);
        etNewPass = findViewById(R.id.etNewPass);
        etConfirmPass = findViewById(R.id.etConfirmPass);
        btnUpdatePass = findViewById(R.id.btnUpdatePass);

        btnUpdatePass.setOnClickListener(v -> {
            String oldPass = etOldPass.getText().toString().trim();
            String newPass = etNewPass.getText().toString().trim();
            String confirmPass = etConfirmPass.getText().toString().trim();

            // ইউজার সেশন থেকে আইডি নেওয়া
            String userId = getSharedPreferences("USER_SESSION", MODE_PRIVATE).getString("userId", "");

            // ১. খালি ঘর চেক করা
            if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(this, "সবগুলো ঘর পূরণ করুন", Toast.LENGTH_SHORT).show();
                return;
            }

            // ২. নতুন পাসওয়ার্ড ও কনফার্ম পাসওয়ার্ড চেক করা
            if (!newPass.equals(confirmPass)) {
                etConfirmPass.setError("পাসওয়ার্ড মিলছে না!");
                return;
            }

            // ৩. সার্ভার URL তৈরি (এখানে oldPass-ও পাঠানো হচ্ছে নিরাপত্তার জন্য)
            String url = Config.SCRIPT_URL + "?action=changePassword" +
                    "&userId=" + userId +
                    "&oldPass=" + oldPass +
                    "&newPass=" + newPass;

            // ৪. ভলি (Volley) রিকোয়েস্ট পাঠানো
            StringRequest request = new StringRequest(Request.Method.GET, url,
                    response -> {
                        if (response.contains("Success")) {
                            Toast.makeText(SettingsActivity.this, "পাসওয়ার্ড সফলভাবে আপডেট হয়েছে!", Toast.LENGTH_SHORT).show();
                            // আপডেট হয়ে গেলে ঘরগুলো খালি করে দেওয়া
                            etOldPass.setText("");
                            etNewPass.setText("");
                            etConfirmPass.setText("");
                        } else {
                            Toast.makeText(SettingsActivity.this, "পুরাতন পাসওয়ার্ড ভুল!", Toast.LENGTH_SHORT).show();
                        }
                    },
                    error -> Toast.makeText(SettingsActivity.this, "সার্ভার এরর!", Toast.LENGTH_SHORT).show());

            Volley.newRequestQueue(this).add(request);
        });
    }
}