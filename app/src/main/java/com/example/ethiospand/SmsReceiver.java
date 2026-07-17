package com.example.ethiospand;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.WorkManager;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SmsReceiver extends BroadcastReceiver {
    private static final String TAG = "EthiospandSmsReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        PendingResult pendingResult = goAsync();
        try {
            SharedPreferences appPrefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
            if (!appPrefs.getBoolean("sms_enabled", true)) {
                return;
            }

            Bundle bundle = intent.getExtras();
            if (bundle == null) {
                return;
            }

            Object[] pdus = (Object[]) bundle.get("pdus");
            if (pdus == null || pdus.length == 0) {
                return;
            }

            String format = bundle.getString("format");
            StringBuilder fullMessage = new StringBuilder();
            String sender = null;

            for (Object pdu : pdus) {
                SmsMessage sms = SmsMessage.createFromPdu((byte[]) pdu, format);
                if (sender == null) {
                    sender = sms.getOriginatingAddress();
                }
                fullMessage.append(sms.getMessageBody());
            }

            String messageBody = fullMessage.toString();
            if (!SmsParser.isBankSms(sender, messageBody)) {
                return;
            }

            Data inputData = new Data.Builder()
                    .putString("sender", sender)
                    .putString("message_body", messageBody)
                    .build();

            String uniqueWorkName = "sms_transaction_" + sha256((sender == null ? "" : sender) + "|" + messageBody);
            OneTimeWorkRequest parseWork = new OneTimeWorkRequest.Builder(DatabaseWorker.class)
                    .setInputData(inputData)
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .addTag("sms_transaction_processing")
                    .build();

            WorkManager.getInstance(context.getApplicationContext())
                    .enqueueUniqueWork(uniqueWorkName, ExistingWorkPolicy.KEEP, parseWork);
        } catch (Exception e) {
            Log.e(TAG, "Failed to enqueue SMS transaction work", e);
        } finally {
            pendingResult.finish();
        }
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(value.hashCode());
        }
    }
}
