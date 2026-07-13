package com.example.ethiospand;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class LoginActivity extends AppCompatActivity {

    private StringBuilder pinBuilder = new StringBuilder();
    private View[] dots = new View[4];
    private SharedPreferences securePrefs;
    private boolean isSetupMode = false;
    private String firstPin = "";
    private TextView tvTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        securePrefs = SecurityHelper.getSecurePreferences(this);
        tvTitle = findViewById(R.id.tvTitle);

        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // Check if user is new
        if (!securePrefs.contains("user_pin")) {
            isSetupMode = true;
            tvTitle.setText("Set your 4-digit PIN");
        }

        dots[0] = findViewById(R.id.dot1);
        dots[1] = findViewById(R.id.dot2);
        dots[2] = findViewById(R.id.dot3);
        dots[3] = findViewById(R.id.dot4);

        int[] buttonIds = {
                R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
                R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9
        };

        View.OnClickListener numListener = v -> {
            if (pinBuilder.length() < 4) {
                Button b = (Button) v;
                pinBuilder.append(b.getText().toString());
                updateDots();
                if (pinBuilder.length() == 4) {
                    handlePinComplete();
                }
            }
        };

        for (int id : buttonIds) {
            findViewById(id).setOnClickListener(numListener);
        }

        findViewById(R.id.btnDelete).setOnClickListener(v -> {
            if (pinBuilder.length() > 0) {
                pinBuilder.deleteCharAt(pinBuilder.length() - 1);
                updateDots();
            }
        });
    }

    private void updateDots() {
        for (int i = 0; i < dots.length; i++) {
            dots[i].setBackgroundResource(i < pinBuilder.length() ? R.drawable.dot_filled : R.drawable.dot_empty);
        }
    }

    private void handlePinComplete() {
        String enteredPin = pinBuilder.toString();
        if (isSetupMode) {
            if (firstPin.isEmpty()) {
                firstPin = enteredPin;
                pinBuilder.setLength(0);
                updateDots();
                tvTitle.setText("Confirm your PIN");
            } else {
                if (enteredPin.equals(firstPin)) {
                    securePrefs.edit().putString("user_pin", enteredPin).apply();
                    Toast.makeText(this, "PIN Setup Successful", Toast.LENGTH_SHORT).show();
                    startMainActivity();
                } else {
                    Toast.makeText(this, "PINs do not match. Start over.", Toast.LENGTH_SHORT).show();
                    firstPin = "";
                    pinBuilder.setLength(0);
                    updateDots();
                    tvTitle.setText("Set your 4-digit PIN");
                }
            }
        } else {
            String savedPin = securePrefs.getString("user_pin", null);
            if (enteredPin.equals(savedPin)) {
                startMainActivity();
            } else {
                Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show();
                pinBuilder.setLength(0);
                updateDots();
            }
        }
    }

    private void startMainActivity() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
