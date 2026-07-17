package com.example.ethiospand;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.util.List;

public class DatabaseWorker extends Worker {
    private static final String TAG = "EthiospandWorker";

    public DatabaseWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String sender = getInputData().getString("sender");
        String messageBody = getInputData().getString("message_body");

        Log.d(TAG, "Worker processing SMS from: " + sender);

        if (!SmsParser.isBankSms(sender, messageBody)) {
            return Result.success();
        }

        try {
            Transaction t = SmsParser.parseSms(messageBody);
            TransactionDao dao = AppDatabase.getInstance(getApplicationContext()).transactionDao();

            if (isDuplicate(dao, t)) {
                Log.d(TAG, "Skipping duplicate SMS transaction: " + t.refNo);
                return Result.success();
            }

            TransactionEntity entity = new TransactionEntity(
                    t.type, t.amount, t.person, t.date, t.time, t.balance, t.refNo, "Bank SMS", ""
            );
            dao.insert(entity);

            String title = t.type.equalsIgnoreCase("Income") ? "💰 Money Received" : "💸 New Expense";
            String msg = "ETB " + t.amount + " - " + t.person;
            NotificationHelper.showBudgetNotification(getApplicationContext(), title, msg);
            checkBudgetAlerts(dao);

            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Failed to process SMS transaction", e);
            return Result.retry();
        }
    }

    private boolean isDuplicate(TransactionDao dao, Transaction transaction) {
        if (transaction.refNo != null
                && !transaction.refNo.trim().isEmpty()
                && !transaction.refNo.equalsIgnoreCase("N/A")
                && dao.countByReference(transaction.refNo) > 0) {
            return true;
        }

        return dao.countByTransactionFingerprint(
                transaction.type,
                transaction.amount,
                transaction.person,
                transaction.date,
                transaction.time,
                transaction.balance
        ) > 0;
    }

    private void checkBudgetAlerts(TransactionDao dao) {
        SharedPreferences prefs = getApplicationContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        int spendPercent = prefs.getInt("spendPercent", 70);
        
        List<TransactionEntity> transactions = dao.getAllTransactions();
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
