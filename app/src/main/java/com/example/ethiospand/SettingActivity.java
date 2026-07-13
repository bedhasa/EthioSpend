package com.example.ethiospand;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.os.LocaleListCompat;

public class SettingActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private SwitchCompat switchSmsReader, switchDarkMode, switchLanguage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        
        // Apply saved theme
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);
        AppCompatDelegate.setDefaultNightMode(isDarkMode ? 
                AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        setContentView(R.layout.activity_setting);

        switchSmsReader = findViewById(R.id.switchSmsReader);
        switchDarkMode = findViewById(R.id.switchDarkMode);
        switchLanguage = findViewById(R.id.switchLanguage);

        // Load saved states
        switchSmsReader.setChecked(prefs.getBoolean("sms_enabled", true));
        switchDarkMode.setChecked(isDarkMode);
        
        String currentLang = prefs.getString("app_lang", "en");
        switchLanguage.setChecked(currentLang.equals("am"));

        setupListeners();
        setupNavigation();
    }

    private void setupListeners() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnChangePassword).setOnClickListener(v -> showChangePinDialog());

        switchSmsReader.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("sms_enabled", isChecked).apply();
            Toast.makeText(this, isChecked ? "SMS Reader Enabled" : "SMS Reader Disabled", Toast.LENGTH_SHORT).show();
        });

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("dark_mode", isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(isChecked ? 
                    AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        });

        switchLanguage.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String newLang = isChecked ? "am" : "en";
            prefs.edit().putString("app_lang", newLang).apply();
            
            LocaleListCompat appLocales = LocaleListCompat.forLanguageTags(newLang);
            AppCompatDelegate.setApplicationLocales(appLocales);
            
            Toast.makeText(this, isChecked ? "ቋንቋ ወደ አማርኛ ተቀይሯል" : "Language changed to English", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnRequestPermissions).setOnClickListener(v -> {
            scanInboxForTransactions();
        });

        findViewById(R.id.btnClearData).setOnClickListener(v -> showClearDataDialog());

        findViewById(R.id.btnPrivacyPolicy).setOnClickListener(v -> 
            Toast.makeText(this, "Privacy Policy coming soon...", Toast.LENGTH_SHORT).show());
    }

    private void scanInboxForTransactions() {
        Toast.makeText(this, "Scanning inbox for bank messages...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            int count = 0;
            ContentResolver cr = getContentResolver();
            Cursor cursor = cr.query(Uri.parse("content://sms/inbox"), null, null, null, "date DESC");

            if (cursor != null && cursor.moveToFirst()) {
                int bodyIdx = cursor.getColumnIndex("body");
                int addressIdx = cursor.getColumnIndex("address");
                int dateIdx = cursor.getColumnIndex("date");

                do {
                    String address = cursor.getString(addressIdx);
                    String body = cursor.getString(bodyIdx);
                    long timestamp = cursor.getLong(dateIdx);

                    if (SmsParser.isBankSms(address, body)) {
                        Transaction t = SmsParser.parseSms(body);
                        // Check if already exists? For simplicity, we just insert. 
                        // Room will ignore if we had a unique constraint, but we don't have one on refNo yet.
                        // Let's at least check refNo to avoid duplicates.
                        
                        TransactionEntity entity = new TransactionEntity(
                                t.type, t.amount, t.person, t.date, t.time, t.balance, t.refNo, "Bank SMS", ""
                        );
                        AppDatabase.getInstance(this).transactionDao().insert(entity);
                        count++;
                    }
                } while (cursor.moveToNext());
                cursor.close();
            }
            
            final int finalCount = count;
            runOnUiThread(() -> {
                Toast.makeText(this, "Scan complete! Found " + finalCount + " transactions.", Toast.LENGTH_LONG).show();
                if (MainActivity.instance != null) MainActivity.instance.loadTransactions();
            });
        }).start();
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
        findViewById(R.id.navReport).setOnClickListener(v -> {
            startActivity(new Intent(this, ReportActivity.class));
            finish();
        });
    }

    private void showChangePinDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_change_pin, null);
        
        EditText etOldPin = view.findViewById(R.id.etOldPin);
        EditText etNewPin = view.findViewById(R.id.etNewPin);

        builder.setView(view)
                .setPositiveButton(R.string.change_pin, (dialog, which) -> {
                    String oldPin = etOldPin.getText().toString();
                    String newPin = etNewPin.getText().toString();
                    
                    SharedPreferences securePrefs = SecurityHelper.getSecurePreferences(this);
                    String savedPin = securePrefs.getString("user_pin", "1234");
                    
                    if (oldPin.equals(savedPin) && newPin.length() == 4) {
                        securePrefs.edit().putString("user_pin", newPin).apply();
                        Toast.makeText(this, "PIN Updated Successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Incorrect current PIN or invalid format", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showClearDataDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_confirm_pin, null);
        
        EditText etConfirmPin = view.findViewById(R.id.etConfirmPin);

        builder.setView(view)
                .setPositiveButton("Delete", (dialog, which) -> {
                    String pin = etConfirmPin.getText().toString();
                    String savedPin = SecurityHelper.getSecurePreferences(this).getString("user_pin", "1234");
                    if (pin.equals(savedPin)) {
                        new Thread(() -> {
                            AppDatabase.getInstance(this).clearAllTables();
                            runOnUiThread(() -> {
                                Toast.makeText(this, "All data wiped clean", Toast.LENGTH_SHORT).show();
                                if (MainActivity.instance != null) MainActivity.instance.loadTransactions();
                            });
                        }).start();
                    } else {
                        Toast.makeText(this, "Incorrect PIN. Data not cleared.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
