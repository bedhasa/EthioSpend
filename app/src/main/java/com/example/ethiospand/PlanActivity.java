package com.example.ethiospand;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class PlanActivity extends AppCompatActivity {

    private EditText etStartDay, etSpendPercent;
    private TextView tvCycleInfo, tvTotalIncome, tvBudgetLimit, tvActualExpense, tvWarningMsg;
    private MaterialCardView cardWarning;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Use the unified SharedPreferences file
        prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        
        setContentView(R.layout.activity_plan);

        etStartDay = findViewById(R.id.etStartDay);
        etSpendPercent = findViewById(R.id.etSpendPercent);
        tvCycleInfo = findViewById(R.id.tvCycleInfo);
        tvTotalIncome = findViewById(R.id.tvTotalIncome);
        tvBudgetLimit = findViewById(R.id.tvBudgetLimit);
        tvActualExpense = findViewById(R.id.tvActualExpense);
        tvWarningMsg = findViewById(R.id.tvWarningMsg);
        cardWarning = findViewById(R.id.cardWarning);

        // Load saved settings
        etStartDay.setText(String.valueOf(prefs.getInt("startDay", 1)));
        etSpendPercent.setText(String.valueOf(prefs.getInt("spendPercent", 70)));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSavePlan).setOnClickListener(v -> saveAndCalculate());

        setupNavigation();
        calculateBudget();
    }

    private void setupNavigation() {
        findViewById(R.id.navDashboard).setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
        findViewById(R.id.navReport).setOnClickListener(v -> {
            startActivity(new Intent(this, ReportActivity.class));
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

    private void saveAndCalculate() {
        try {
            int day = Integer.parseInt(etStartDay.getText().toString());
            int percent = Integer.parseInt(etSpendPercent.getText().toString());

            if (day < 1 || day > 31) {
                Toast.makeText(this, "Day must be between 1-31", Toast.LENGTH_SHORT).show();
                return;
            }

            prefs.edit().putInt("startDay", day).putInt("spendPercent", percent).apply();
            calculateBudget();
            Toast.makeText(this, "Plan Updated & Saved", Toast.LENGTH_SHORT).show();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid input", Toast.LENGTH_SHORT).show();
        }
    }

    private void calculateBudget() {
        int startDay = prefs.getInt("startDay", 1);
        int spendPercent = prefs.getInt("spendPercent", 70);

        Calendar cal = Calendar.getInstance();
        int currentDay = cal.get(Calendar.DAY_OF_MONTH);
        
        Calendar startRange = (Calendar) cal.clone();
        if (currentDay < startDay) {
            startRange.add(Calendar.MONTH, -1);
        }
        startRange.set(Calendar.DAY_OF_MONTH, startDay);
        startRange.set(Calendar.HOUR_OF_DAY, 0);
        startRange.set(Calendar.MINUTE, 0);

        Calendar endRange = (Calendar) startRange.clone();
        endRange.add(Calendar.MONTH, 1);
        endRange.add(Calendar.DAY_OF_MONTH, -1);

        SimpleDateFormat df = new SimpleDateFormat("dd MMM", Locale.getDefault());
        tvCycleInfo.setText("Cycle: " + df.format(startRange.getTime()) + " - " + df.format(endRange.getTime()));

        new Thread(() -> {
            List<TransactionEntity> transactions = AppDatabase.getInstance(this).transactionDao().getAllTransactions();
            double incomeSum = 0;
            double expenseSum = 0;

            for (TransactionEntity t : transactions) {
                try {
                    double amt = Double.parseDouble(t.amount.replace(",", ""));
                    if (t.type.equalsIgnoreCase("Income")) incomeSum += amt;
                    else expenseSum += amt;
                } catch (Exception ignored) {}
            }

            double finalIncome = incomeSum;
            double finalExpense = expenseSum;
            double limit = (finalIncome * spendPercent) / 100.0;

            runOnUiThread(() -> {
                tvTotalIncome.setText(String.format("ETB %.2f", finalIncome));
                tvBudgetLimit.setText(String.format("ETB %.2f", limit));
                tvActualExpense.setText(String.format("ETB %.2f", finalExpense));

                if (finalExpense > limit && limit > 0) {
                    cardWarning.setVisibility(View.VISIBLE);
                    cardWarning.setCardBackgroundColor(Color.parseColor("#FFEBEE"));
                    tvWarningMsg.setText("Budget Exceeded!");
                    tvWarningMsg.setTextColor(Color.parseColor("#C62828"));
                } else if (finalExpense > limit * 0.8 && limit > 0) {
                    cardWarning.setVisibility(View.VISIBLE);
                    cardWarning.setCardBackgroundColor(Color.parseColor("#FFF3E0"));
                    tvWarningMsg.setText("Approaching limit (80% used)");
                    tvWarningMsg.setTextColor(Color.parseColor("#E65100"));
                } else {
                    cardWarning.setVisibility(View.GONE);
                }
            });
        }).start();
    }
}
