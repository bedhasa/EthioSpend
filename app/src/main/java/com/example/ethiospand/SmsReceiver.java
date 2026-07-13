package com.example.ethiospand;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.SmsMessage;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class SmsReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // First check if SMS Reading is enabled in Settings
        SharedPreferences appPrefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        if (!appPrefs.getBoolean("sms_enabled", true)) {
            return;
        }

        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            Object[] pdus = (Object[]) bundle.get("pdus");
            if (pdus != null) {
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

                if (SmsParser.isBankSms(sender, messageBody)) {
                    // Hand off to WorkManager for reliable background processing
                    Data inputData = new Data.Builder()
                            .putString("sender", sender)
                            .putString("message_body", messageBody)
                            .build();

                    OneTimeWorkRequest parseWork = new OneTimeWorkRequest.Builder(DatabaseWorker.class)
                            .setInputData(inputData)
                            .build();

                    WorkManager.getInstance(context).enqueue(parseWork);
                }
            }
        }
    }
}
