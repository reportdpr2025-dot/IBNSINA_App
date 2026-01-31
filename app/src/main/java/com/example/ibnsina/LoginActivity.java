package com.example.ibnsina;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etUser, etName, etDesignation, etPass;
    private MaterialButton btnLogin;
    private ProgressBar loginProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // সেশন চেক - আগে লগইন করা থাকলে ড্যাশবোর্ডে পাঠাবে
        SharedPreferences prefs = getSharedPreferences("USER_SESSION", MODE_PRIVATE);
        if (prefs.getBoolean("isLoggedIn", false)) {
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        etUser = findViewById(R.id.etUser);
        etName = findViewById(R.id.etName);
        etDesignation = findViewById(R.id.etDesignation);
        etPass = findViewById(R.id.etPass);
        btnLogin = findViewById(R.id.btnLogin);
        loginProgressBar = findViewById(R.id.loginProgressBar);

        etPass.setEnabled(true);
        btnLogin.setEnabled(true);

        etUser.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                String id = s.toString().trim().toUpperCase();
                if (id.length() >= 4) {
                    fetchAutoFillData(id, false);
                } else {
                    etName.setText("");
                    etDesignation.setText("");
                }
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        btnLogin.setOnClickListener(v -> {
            String userId = etUser.getText().toString().trim().toUpperCase();
            String password = etPass.getText().toString().trim();
            String currentName = etName.getText().toString().trim();

            if (userId.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "আইডি এবং পাসওয়ার্ড দিন", Toast.LENGTH_SHORT).show();
                return;
            }

            if (currentName.isEmpty()) {
                fetchAutoFillData(userId, true);
            } else {
                performLogin(userId, password);
            }
        });
    }

    private void fetchAutoFillData(String userId, boolean shouldLoginAfterFetch) {
        if (loginProgressBar != null) loginProgressBar.setVisibility(View.VISIBLE);
        
        String url = Config.SCRIPT_URL + "?action=getAutoFill&userId=" + userId;
        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    if (loginProgressBar != null) loginProgressBar.setVisibility(View.GONE);
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.getBoolean("success")) {
                            etName.setText(json.getString("name"));
                            etDesignation.setText(json.getString("designation"));

                            if (shouldLoginAfterFetch) {
                                performLogin(userId, etPass.getText().toString().trim());
                            }
                        } else {
                            if (shouldLoginAfterFetch) {
                                Toast.makeText(this, "সঠিক ইউজার আইডি পাওয়া যায়নি!", Toast.LENGTH_SHORT).show();
                            }
                            etName.setText("");
                            etDesignation.setText("");
                        }
                    } catch (JSONException e) { e.printStackTrace(); }
                }, error -> {
            if (loginProgressBar != null) loginProgressBar.setVisibility(View.GONE);
            if (shouldLoginAfterFetch) Toast.makeText(this, "সার্ভার এরর!", Toast.LENGTH_SHORT).show();
        });
        Volley.newRequestQueue(this).add(request);
    }

    private void performLogin(String userId, String password) {
        if (loginProgressBar != null) loginProgressBar.setVisibility(View.VISIBLE);
        btnLogin.setText("Checking...");
        btnLogin.setEnabled(false);

        String url = Config.SCRIPT_URL + "?action=login&userId=" + userId + "&password=" + password;

        StringRequest loginRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    if (loginProgressBar != null) loginProgressBar.setVisibility(View.GONE);
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Login");
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.getBoolean("success")) {
                            String userName = etName.getText().toString().trim();
                            String designation = etDesignation.getText().toString().trim();

                            SharedPreferences.Editor editor = getSharedPreferences("USER_SESSION", MODE_PRIVATE).edit();
                            editor.putBoolean("isLoggedIn", true);
                            editor.putString("userId", userId);
                            editor.putString("userName", userName);
                            editor.putString("designation", designation);
                            editor.apply();

                            startActivity(new Intent(this, DashboardActivity.class));
                            finish();
                        } else {
                            Toast.makeText(this, "পাসওয়ার্ড ভুল!", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) { e.printStackTrace(); }
                }, error -> {
            if (loginProgressBar != null) loginProgressBar.setVisibility(View.GONE);
            btnLogin.setEnabled(true);
            btnLogin.setText("Login");
            Toast.makeText(this, "সার্ভার কানেকশন এরর!", Toast.LENGTH_SHORT).show();
        });
        Volley.newRequestQueue(this).add(loginRequest);
    }
}