package com.example.ibnsina;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class DashboardActivity extends AppCompatActivity {

    private TextView tvUserName, tvUserDetails;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // ভিউ কানেক্ট করা
        tvUserName = findViewById(R.id.tvUserName);
        tvUserDetails = findViewById(R.id.tvUserDetails);

        // সেশন থেকে ডাটা নিয়ে আসা
        SharedPreferences prefs = getSharedPreferences("USER_SESSION", MODE_PRIVATE);
        String name = prefs.getString("userName", "User");
        String id = prefs.getString("userId", "N/A");
        String desig = prefs.getString("designation", "N/A");

        // ডাটা স্ক্রিনে দেখানো
        tvUserName.setText(name);
        tvUserDetails.setText("ID: " + id + " | " + desig);

        // কার্ডগুলো সেটআপ
        CardView cardPharma = findViewById(R.id.cardPharma);
        CardView cardSinaVision = findViewById(R.id.cardSinaVision);
        CardView cardINM = findViewById(R.id.cardINM);
        CardView cardSetting = findViewById(R.id.cardSetting);
        CardView cardAbout = findViewById(R.id.cardAbout);
        CardView cardLogout = findViewById(R.id.cardLogout);

        cardPharma.setOnClickListener(v -> startActivity(new Intent(this, PharmaMenuActivity.class)));
        cardSinaVision.setOnClickListener(v -> startActivity(new Intent(this, SinaVisionMenuActivity.class)));
        cardINM.setOnClickListener(v -> startActivity(new Intent(this, InmMenuActivity.class)));
        cardSetting.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));

        cardAbout.setOnClickListener(v ->
                Toast.makeText(this, "IBN SINA Inventory System v1.0", Toast.LENGTH_SHORT).show());

        cardLogout.setOnClickListener(v -> {
            SharedPreferences preferences = getSharedPreferences("USER_SESSION", MODE_PRIVATE);
            preferences.edit().clear().apply();

            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            Toast.makeText(this, "Logged Out Successfully", Toast.LENGTH_SHORT).show();
        });
    }
}