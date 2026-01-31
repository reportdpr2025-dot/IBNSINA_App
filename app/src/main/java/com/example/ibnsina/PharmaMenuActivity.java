package com.example.ibnsina;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class PharmaMenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pharma_menu);

        // ১. ব্যাক বাটন
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // ২. Stock Report Update (MainActivity ওপেন করবে)
        View cardStockReport = findViewById(R.id.cardStockReport);
        if (cardStockReport != null) {
            cardStockReport.setOnClickListener(v -> {
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("SELECTED_CATEGORY", "PHARMA");
                startActivity(intent);
            });
        }

        // ৩. Stock Report Manual (১ম ওয়েব লিঙ্ক)
        View cardSalesUpdate = findViewById(R.id.cardSalesUpdate);
        if (cardSalesUpdate != null) {
            cardSalesUpdate.setOnClickListener(v -> {
                openWebLink("https://script.google.com/macros/s/AKfycbwIULJidE-kHC3Hzi-tczf3NS8hQC5ZX6ht0n43KybX_4zJfcBlsB3LIJdsu7m3GDGmvQ/exec");
            });
        }

        // ৪. Short Date Tracking (২য় ওয়েব লিঙ্ক)
        View cardShortItems = findViewById(R.id.cardShortItems);
        if (cardShortItems != null) {
            cardShortItems.setOnClickListener(v -> {
                openWebLink("https://script.google.com/macros/s/AKfycbz5rg_3VG0pF4u98p_najf_lubb5XHA85GzcNqrX-3aRfT-BySUlULu0l9zeEMXtdQQVA/exec");
            });
        }

        // ৫. Carton QTY Update
        View cardOrderList = findViewById(R.id.cardOrderList);
        if (cardOrderList != null) {
            cardOrderList.setOnClickListener(v -> {
                Toast.makeText(this, "Carton Update Link Coming Soon!", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void openWebLink(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Could not open link", Toast.LENGTH_SHORT).show();
        }
    }
}