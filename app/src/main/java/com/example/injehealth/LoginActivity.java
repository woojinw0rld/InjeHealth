package com.example.injehealth;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.example.injehealth.db.AppDatabase;
import com.example.injehealth.db.entity.User;

import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private EditText etWeight, etHeight, etAge;
    private Button btnMale, btnFemale, btnStart;
    private String selectedGender = "male";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etWeight = findViewById(R.id.et_weight);
        etHeight = findViewById(R.id.et_height);
        etAge    = findViewById(R.id.et_age);
        btnMale  = findViewById(R.id.btn_male);
        btnFemale = findViewById(R.id.btn_female);
        btnStart = findViewById(R.id.btn_start);

        setupGenderToggle();
        setupInputWatcher();

        btnStart.setOnClickListener(v -> saveAndProceed());
    }

    private void setupGenderToggle() {
        btnMale.setOnClickListener(v -> {
            selectedGender = "male";
            btnMale.setBackgroundResource(R.drawable.bg_gender_selected);
            btnMale.setTextColor(getColor(R.color.white));
            btnFemale.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            btnFemale.setTextColor(getColor(R.color.text_secondary));
        });

        btnFemale.setOnClickListener(v -> {
            selectedGender = "female";
            btnFemale.setBackgroundResource(R.drawable.bg_gender_selected);
            btnFemale.setTextColor(getColor(R.color.white));
            btnMale.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            btnMale.setTextColor(getColor(R.color.text_secondary));
        });
    }

    private void setupInputWatcher() {
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                boolean filled = !etWeight.getText().toString().trim().isEmpty()
                        && !etHeight.getText().toString().trim().isEmpty()
                        && !etAge.getText().toString().trim().isEmpty();
                btnStart.setEnabled(filled);
            }
        };
        etWeight.addTextChangedListener(watcher);
        etHeight.addTextChangedListener(watcher);
        etAge.addTextChangedListener(watcher);
    }

    private void saveAndProceed() {
        double weight = Double.parseDouble(etWeight.getText().toString().trim());
        double height = Double.parseDouble(etHeight.getText().toString().trim());
        int age       = Integer.parseInt(etAge.getText().toString().trim());

        User user = new User();
        user.weight = weight;
        user.height = height;
        user.age    = age;
        user.gender = selectedGender;

        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase.getInstance(this).userDao().insert(user);
            runOnUiThread(() -> {
                //startActivity(new Intent(this, HomeActivity.class));
                //finish();
                return;
            });
        });
    }
}
