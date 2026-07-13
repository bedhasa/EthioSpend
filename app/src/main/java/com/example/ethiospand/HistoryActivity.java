package com.example.ethiospand;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity implements TransactionAdapter.OnTransactionClickListener {

    private TransactionAdapter adapter;
    private RecyclerView rvAllTransactions;
    private final String[] categories = {"Food", "Transport", "Rent", "Shopping", "Salary", "Gift", "Other", "Bank SMS"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        rvAllTransactions = findViewById(R.id.rvAllTransactions);
        rvAllTransactions.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TransactionAdapter();
        adapter.setOnTransactionClickListener(this);
        rvAllTransactions.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        setupNavigation();
        loadAllTransactions();
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
        findViewById(R.id.navReport).setOnClickListener(v -> {
            startActivity(new Intent(this, ReportActivity.class));
            finish();
        });
        findViewById(R.id.navSetting).setOnClickListener(v -> {
            startActivity(new Intent(this, SettingActivity.class));
            finish();
        });
    }

    private void loadAllTransactions() {
        new Thread(() -> {
            List<TransactionEntity> list = AppDatabase.getInstance(this).transactionDao().getAllTransactions();
            runOnUiThread(() -> adapter.setTransactions(list));
        }).start();
    }

    @Override
    public void onTransactionClick(TransactionEntity transaction) {
        showTransactionDialog(transaction);
    }

    private void showTransactionDialog(TransactionEntity transaction) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_manual_entry, null);
        
        EditText etAmount = view.findViewById(R.id.etAmount);
        EditText etPerson = view.findViewById(R.id.etPerson);
        EditText etDate = view.findViewById(R.id.etDate);
        EditText etRemarks = view.findViewById(R.id.etRemarks);
        Spinner spCategory = view.findViewById(R.id.spCategory);
        TextView tvTitle = view.findViewById(R.id.tvDialogTitle);

        tvTitle.setText("Transaction Details");
        etAmount.setText(transaction.amount);
        etPerson.setText(transaction.person);
        etDate.setText(transaction.date);
        etRemarks.setText(transaction.remarks != null ? transaction.remarks : "");

        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategory.setAdapter(catAdapter);

        if (transaction.category != null) {
            for (int i = 0; i < categories.length; i++) {
                if (categories[i].equalsIgnoreCase(transaction.category)) {
                    spCategory.setSelection(i);
                    break;
                }
            }
        }

        final Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        etDate.setOnClickListener(v -> {
            new DatePickerDialog(this, (view1, year, month, dayOfMonth) -> {
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                etDate.setText(dateFormat.format(calendar.getTime()));
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        builder.setView(view)
                .setPositiveButton("Update", (dialog, which) -> {
                    transaction.amount = etAmount.getText().toString();
                    transaction.person = etPerson.getText().toString();
                    transaction.date = etDate.getText().toString();
                    transaction.category = spCategory.getSelectedItem().toString();
                    transaction.remarks = etRemarks.getText().toString();
                    updateTransactionInDb(transaction);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateTransactionInDb(TransactionEntity transaction) {
        new Thread(() -> {
            AppDatabase.getInstance(this).transactionDao().update(transaction);
            loadAllTransactions();
        }).start();
    }
}
