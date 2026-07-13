package com.example.ethiospand;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements TransactionAdapter.OnTransactionClickListener {

    public static MainActivity instance;
    private TransactionAdapter adapter;
    private RecyclerView rvTransactions;
    private TextView tvBalanceAmount;
    private LineChart lineChart;
    private PieChart pieChart;
    
    private boolean isFabOpen = false;
    private LinearLayout layoutAddIncome, layoutAddExpense;
    private View fabAdd;

    private final String[] categories = {"Food", "Transport", "Rent", "Shopping", "Salary", "Gift", "Other", "Bank SMS"};
    private static final int PERMISSION_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        tvBalanceAmount = findViewById(R.id.tvBalanceAmount);
        rvTransactions = findViewById(R.id.rvTransactions);
        lineChart = findViewById(R.id.lineChart);
        pieChart = findViewById(R.id.pieChart);
        
        layoutAddIncome = findViewById(R.id.layoutAddIncome);
        layoutAddExpense = findViewById(R.id.layoutAddExpense);
        fabAdd = findViewById(R.id.fabAdd);

        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TransactionAdapter();
        adapter.setOnTransactionClickListener(this);
        rvTransactions.setAdapter(adapter);

        setupFabMenu();
        setupCharts();
        setupNavigation();

        checkPermissions();
        requestBatteryExemption();
        loadTransactions();
    }

    private void setupNavigation() {
        findViewById(R.id.navPlan).setOnClickListener(v -> startActivity(new Intent(this, PlanActivity.class)));
        findViewById(R.id.navHistory).setOnClickListener(v -> startActivity(new Intent(this, HistoryActivity.class)));
        findViewById(R.id.navSetting).setOnClickListener(v -> startActivity(new Intent(this, SettingActivity.class)));
        findViewById(R.id.navReport).setOnClickListener(v -> startActivity(new Intent(this, ReportActivity.class)));
        findViewById(R.id.navDashboard).setOnClickListener(v -> loadTransactions());

        View seeAll = findViewById(R.id.tvSeeAll);
        if (seeAll != null) {
            seeAll.setOnClickListener(v -> startActivity(new Intent(this, HistoryActivity.class)));
        }
    }

    private void checkPermissions() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.RECEIVE_SMS);
        permissions.add(Manifest.permission.READ_SMS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p : permissions) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }

        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[0]), PERMISSION_CODE);
        }
    }

    private void requestBatteryExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                new AlertDialog.Builder(this)
                        .setTitle("Background Tracking")
                        .setMessage("Allow Ethiospand to run in the background to track SMS even when the app is closed.")
                        .setPositiveButton("Settings", (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                            intent.setData(Uri.parse("package:" + getPackageName()));
                            startActivity(intent);
                        })
                        .setNegativeButton("Later", null)
                        .show();
            }
        }
    }

    private void setupFabMenu() {
        fabAdd.setOnClickListener(v -> toggleFabMenu());
        findViewById(R.id.fabIncome).setOnClickListener(v -> {
            showTransactionDialog(null, "Income");
            toggleFabMenu();
        });
        findViewById(R.id.fabExpense).setOnClickListener(v -> {
            showTransactionDialog(null, "Expense");
            toggleFabMenu();
        });
    }

    private void toggleFabMenu() {
        isFabOpen = !isFabOpen;
        layoutAddIncome.setVisibility(isFabOpen ? View.VISIBLE : View.GONE);
        layoutAddExpense.setVisibility(isFabOpen ? View.VISIBLE : View.GONE);
    }

    private void showTransactionDialog(TransactionEntity existingTransaction, String typeIfNew) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_manual_entry, null);
        
        EditText etAmount = view.findViewById(R.id.etAmount);
        EditText etPerson = view.findViewById(R.id.etPerson);
        EditText etDate = view.findViewById(R.id.etDate);
        EditText etRemarks = view.findViewById(R.id.etRemarks);
        Spinner spCategory = view.findViewById(R.id.spCategory);
        TextView tvTitle = view.findViewById(R.id.tvDialogTitle);

        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategory.setAdapter(catAdapter);

        final Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        if (existingTransaction != null) {
            tvTitle.setText("Edit Transaction");
            etAmount.setText(existingTransaction.amount);
            etPerson.setText(existingTransaction.person);
            etDate.setText(existingTransaction.date);
            etRemarks.setText(existingTransaction.remarks);
            for (int i = 0; i < categories.length; i++) {
                if (categories[i].equalsIgnoreCase(existingTransaction.category)) {
                    spCategory.setSelection(i);
                    break;
                }
            }
        } else {
            tvTitle.setText("Add " + typeIfNew);
            etDate.setText(dateFormat.format(calendar.getTime()));
        }

        etDate.setOnClickListener(v -> {
            new DatePickerDialog(this, (view1, year, month, dayOfMonth) -> {
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                etDate.setText(dateFormat.format(calendar.getTime()));
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        builder.setView(view)
                .setPositiveButton("Save", (dialog, which) -> {
                    String amount = etAmount.getText().toString();
                    String person = etPerson.getText().toString();
                    String date = etDate.getText().toString();
                    String category = spCategory.getSelectedItem().toString();
                    String remarks = etRemarks.getText().toString();

                    if (!amount.isEmpty() && !person.isEmpty()) {
                        if (existingTransaction == null) {
                            saveNewTransaction(typeIfNew, amount, person, date, category, remarks);
                        } else {
                            existingTransaction.amount = amount;
                            existingTransaction.person = person;
                            existingTransaction.date = date;
                            existingTransaction.category = category;
                            existingTransaction.remarks = remarks;
                            updateTransactionInDb(existingTransaction);
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveNewTransaction(String type, String amount, String person, String date, String category, String remarks) {
        new Thread(() -> {
            String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
            String lastBalanceStr = AppDatabase.getInstance(this).transactionDao().getLatestBalance();
            double balance = 0;
            try {
                if (lastBalanceStr != null) balance = Double.parseDouble(lastBalanceStr.replace(",", ""));
            } catch (Exception ignored) {}
            
            double amt = Double.parseDouble(amount);
            double newBalance = type.equals("Income") ? balance + amt : balance - amt;
            
            TransactionEntity entity = new TransactionEntity(type, amount, person, date, time, 
                    String.format("%.2f", newBalance), "Manual", category, remarks);
            AppDatabase.getInstance(this).transactionDao().insert(entity);
            runOnUiThread(this::loadTransactions);
        }).start();
    }

    private void updateTransactionInDb(TransactionEntity transaction) {
        new Thread(() -> {
            AppDatabase.getInstance(this).transactionDao().update(transaction);
            runOnUiThread(this::loadTransactions);
        }).start();
    }

    private void setupCharts() {
        lineChart.getDescription().setEnabled(false);
        lineChart.setDrawGridBackground(false);
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f); 
        xAxis.setLabelRotationAngle(-45);
        lineChart.getAxisRight().setEnabled(false);
        Legend l = lineChart.getLegend();
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);

        pieChart.getDescription().setEnabled(false);
        pieChart.setHoleRadius(40f);
        pieChart.setTransparentCircleRadius(45f);
        pieChart.setDrawEntryLabels(true);
        pieChart.setEntryLabelColor(Color.BLACK);
    }

    public void loadTransactions() {
        new Thread(() -> {
            List<TransactionEntity> list = AppDatabase.getInstance(this).transactionDao().getAllTransactions();
            String latestBalance = AppDatabase.getInstance(this).transactionDao().getLatestBalance();
            runOnUiThread(() -> {
                adapter.setTransactions(list);
                if (latestBalance != null && !latestBalance.isEmpty()) {
                    tvBalanceAmount.setText("ETB " + latestBalance);
                }
                updateCharts(list);
            });
        }).start();
    }

    private void updateCharts(List<TransactionEntity> transactions) {
        Map<String, Float> dailyIncome = new HashMap<>();
        Map<String, Float> dailyExpense = new HashMap<>();
        Map<String, Float> categoryMap = new HashMap<>();

        for (TransactionEntity t : transactions) {
            try {
                float val = Float.parseFloat(t.amount.replace(",", ""));
                String dateKey = t.date; 
                if (t.type.equalsIgnoreCase("Income")) {
                    dailyIncome.put(dateKey, dailyIncome.getOrDefault(dateKey, 0f) + val);
                } else {
                    dailyExpense.put(dateKey, dailyExpense.getOrDefault(dateKey, 0f) + val);
                    String cat = (t.category != null && !t.category.isEmpty()) ? t.category : "Uncategorized";
                    categoryMap.put(cat, categoryMap.getOrDefault(cat, 0f) + val);
                }
            } catch (Exception ignored) {}
        }

        List<String> allDates = new ArrayList<>();
        for (String d : dailyIncome.keySet()) if (!allDates.contains(d)) allDates.add(d);
        for (String d : dailyExpense.keySet()) if (!allDates.contains(d)) allDates.add(d);

        Collections.sort(allDates, (d1, d2) -> {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                return sdf.parse(d1).compareTo(sdf.parse(d2));
            } catch (Exception e) { return d1.compareTo(d2); }
        });

        List<String> lastDates = allDates.subList(Math.max(0, allDates.size() - 7), allDates.size());
        List<Entry> incomeEntries = new ArrayList<>();
        List<Entry> expenseEntries = new ArrayList<>();
        final List<String> xLabels = new ArrayList<>();

        for (int i = 0; i < lastDates.size(); i++) {
            String date = lastDates.get(i);
            incomeEntries.add(new Entry(i, dailyIncome.getOrDefault(date, 0f)));
            expenseEntries.add(new Entry(i, dailyExpense.getOrDefault(date, 0f)));
            xLabels.add(date.length() > 5 ? date.substring(0, 5) : date);
        }

        lineChart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                int index = (int) value;
                return (index >= 0 && index < xLabels.size()) ? xLabels.get(index) : "";
            }
        });

        LineDataSet setIn = new LineDataSet(incomeEntries, "Income");
        setIn.setColor(Color.parseColor("#4285F4"));
        setIn.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        setIn.setDrawFilled(true);
        setIn.setFillAlpha(30);

        LineDataSet setEx = new LineDataSet(expenseEntries, "Expenses");
        setEx.setColor(Color.parseColor("#FBBC05"));
        setEx.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        setEx.setDrawFilled(true);
        setEx.setFillAlpha(30);

        lineChart.setData(new LineData(setIn, setEx));
        lineChart.invalidate();

        List<PieEntry> pieEntries = new ArrayList<>();
        for (Map.Entry<String, Float> entry : categoryMap.entrySet()) {
            pieEntries.add(new PieEntry(entry.getValue(), entry.getKey()));
        }
        PieDataSet pieDataSet = new PieDataSet(pieEntries, "");
        pieDataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        pieChart.setData(new PieData(pieDataSet));
        pieChart.invalidate();
    }

    @Override
    public void onTransactionClick(TransactionEntity transaction) {
        showTransactionDialog(transaction, null);
    }

    public static void updateUI(Transaction transaction, String sender) {
        if (instance != null) instance.runOnUiThread(() -> instance.loadTransactions());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTransactions();
    }
}
