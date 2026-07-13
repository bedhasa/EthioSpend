package com.example.ethiospand;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.util.List;

public class DatabaseWorker extends Worker {

    public DatabaseWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String sender = getInputData().getString("sender");
        String messageBody = getInputData().getString("message_body");

        Log.d("Ethiospand", "Worker processing SMS from: " + sender);

        if (SmsParser.isBankSms(sender, messageBody)) {
            Transaction t = SmsParser.parseSms(messageBody);

            TransactionEntity entity = new TransactionEntity(
                    t.type, t.amount, t.person, t.date, t.time, t.balance, t.refNo, "Bank SMS", ""
            );
            
            AppDatabase.getInstance(getApplicationContext()).transactionDao().insert(entity);
            
            // 1. Notify user about the specific transaction
            String title = t.type.equalsIgnoreCase("Income") ? "💰 Money Received" : "💸 New Expense";
            String msg = "ETB " + t.amount + " - " + t.person;
            NotificationHelper.showBudgetNotification(getApplicationContext(), title, msg);

            // 2. Check budget limits and notify if exceeded
            checkBudgetAlerts();

            // 3. Refresh UI if app is currently open
            if (MainActivity.instance != null) {
                MainActivity.instance.runOnUiThread(() -> MainActivity.instance.loadTransactions());
            }
            
            return Result.success();
        }

        return Result.failure();
    }

    private void checkBudgetAlerts() {
        // Unified preferences file "AppPrefs"
        SharedPreferences prefs = getApplicationContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        int spendPercent = prefs.getInt("spendPercent", 70);
        
        List<TransactionEntity> transactions = AppDatabase.getInstance(getApplicationContext()).transactionDao().getAllTransactions();
        double incomeSum = 0;
        double expenseSum = 0;

        for (TransactionEntity t : transactions) {
            try {
                double amt = Double.parseDouble(t.amount.replace(",", ""));
                if (t.type.equalsIgnoreCase("Income")) incomeSum += amt;
                else expenseSum += amt;
            } catch (Exception ignored) {}
        }

        double limit = (incomeSum * spendPercent) / 100.0;
        if (limit > 0) {
            if (expenseSum > limit) {
                NotificationHelper.showBudgetNotification(getApplicationContext(), "🚨 Budget Alert!", "You have exceeded your " + spendPercent + "% spending limit!");
            } else if (expenseSum > limit * 0.8) {
                NotificationHelper.showBudgetNotification(getApplicationContext(), "⚠️ Budget Warning", "You've used 80% of your allowed budget.");
            }
        }
    }
}
