package com.example.ethiospand;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReportActivity extends AppCompatActivity {

    private TextView tvCycleDates, tvReportTotalSpent, tvReportTotalIncome;
    private LinearLayout layoutTopCategories, layoutTopDestinations, layoutTopSources;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        // Standardized to AppPrefs
        prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);

        tvCycleDates = findViewById(R.id.tvCycleDates);
        tvReportTotalSpent = findViewById(R.id.tvReportTotalSpent);
        tvReportTotalIncome = findViewById(R.id.tvReportTotalIncome);
        layoutTopCategories = findViewById(R.id.layoutTopCategories);
        layoutTopDestinations = findViewById(R.id.layoutTopDestinations);
        layoutTopSources = findViewById(R.id.layoutTopSources);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        setupNavigation();
        loadReportData();
    }

    private void setupNavigation() {
        findViewById(R.id.navDashboard).setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
        findViewById(R.id.navPlan).setOnClickListener(v -> {
            startActivity(new Intent(this, PlanActivity.class));
            finish();
        });
        findViewById(R.id.navHistory).setOnClickListener(v -> {
            startActivity(new Intent(this, HistoryActivity.class));
            finish();
        });
        findViewById(R.id.navSetting).setOnClickListener(v -> {
            startActivity(new Intent(this, SettingActivity.class));
            finish();
        });
    }

    private void loadReportData() {
        int startDay = prefs.getInt("startDay", 1);
        Calendar cal = Calendar.getInstance();
        int currentDay = cal.get(Calendar.DAY_OF_MONTH);
        
        Calendar startRange = (Calendar) cal.clone();
        if (currentDay < startDay) startRange.add(Calendar.MONTH, -1);
        startRange.set(Calendar.DAY_OF_MONTH, startDay);
        startRange.set(Calendar.HOUR_OF_DAY, 0);

        Calendar endRange = (Calendar) startRange.clone();
        endRange.add(Calendar.MONTH, 1);
        endRange.add(Calendar.DAY_OF_MONTH, -1);

        SimpleDateFormat df = new SimpleDateFormat("dd MMM", Locale.getDefault());
        tvCycleDates.setText(df.format(startRange.getTime()) + " - " + df.format(endRange.getTime()));

        new Thread(() -> {
            List<TransactionEntity> transactions = AppDatabase.getInstance(this).transactionDao().getAllTransactions();
            
            double totalIncome = 0;
            double totalExpense = 0;
            Map<String, Double> categoriesMap = new HashMap<>();
            Map<String, Double> destinationsMap = new HashMap<>();
            Map<String, Double> sourcesMap = new HashMap<>();

            for (TransactionEntity t : transactions) {
                try {
                    double amt = Double.parseDouble(t.amount.replace(",", ""));
                    if (t.type.equalsIgnoreCase("Income")) {
                        totalIncome += amt;
                        sourcesMap.put(t.person, sourcesMap.getOrDefault(t.person, 0.0) + amt);
                    } else {
                        totalExpense += amt;
                        String cat = (t.category != null && !t.category.isEmpty()) ? t.category : "Uncategorized";
                        categoriesMap.put(cat, categoriesMap.getOrDefault(cat, 0.0) + amt);
                        destinationsMap.put(t.person, destinationsMap.getOrDefault(t.person, 0.0) + amt);
                    }
                } catch (Exception ignored) {}
            }

            final double fIncome = totalIncome;
            final double fExpense = totalExpense;

            runOnUiThread(() -> {
                tvReportTotalIncome.setText(String.format("ETB %.2f", fIncome));
                tvReportTotalSpent.setText(String.format("ETB %.2f", fExpense));
                
                displayTopItems(layoutTopCategories, categoriesMap, "#FF4B4B");
                displayTopItems(layoutTopDestinations, destinationsMap, "#101828");
                displayTopItems(layoutTopSources, sourcesMap, "#00C566");
            });
        }).start();
    }

    private void displayTopItems(LinearLayout layout, Map<String, Double> dataMap, String colorHex) {
        layout.removeAllViews();
        if (dataMap.isEmpty()) return;

        List<Map.Entry<String, Double>> list = new ArrayList<>(dataMap.entrySet());
        Collections.sort(list, (o1, o2) -> o2.getValue().compareTo(o1.getValue()));

        for (int i = 0; i < Math.min(list.size(), 5); i++) {
            Map.Entry<String, Double> entry = list.get(i);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 12, 0, 12);
            
            TextView name = new TextView(this);
            name.setText(entry.getKey());
            name.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            name.setTextColor(Color.parseColor("#344054"));
            
            TextView amount = new TextView(this);
            amount.setText(String.format("ETB %.2f", entry.getValue()));
            amount.setTextColor(Color.parseColor(colorHex));
            amount.setTypeface(null, Typeface.BOLD);
            
            row.addView(name);
            row.addView(amount);
            layout.addView(row);
        }
    }
}
