package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private AutoCompleteTextView actvCompany;
    private Button btnLogin;

    // قائمة الشركات الـ 5
    private final String[] companies = new String[]{
            "شركة أرامكو السعودية",
            "شركة الاتصالات السعودية (STC)",
            "مصرف الراجحي",
            "شركة سابك (SABIC)",
            "شركة المراعي"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        actvCompany = findViewById(R.id.actvCompany);
        btnLogin = findViewById(R.id.btnLogin);

        // ربط قائمة الشركات مع حقل البحث والفلترة
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                companies
        );
        actvCompany.setAdapter(adapter);

        // عند الضغط على زر تسجيل الدخول
        btnLogin.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String company = actvCompany.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "الرجاء إدخال اسم المستخدم وكلمة المرور", Toast.LENGTH_SHORT).show();
                return;
            }

            if (company.isEmpty()) {
                Toast.makeText(LoginActivity.this, "الرجاء اختيار أو البحث عن الشركة", Toast.LENGTH_SHORT).show();
                return;
            }

            // الانتقال إلى شاشة الخريطة وتمرير البيانات
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.putExtra("USERNAME", username);
            intent.putExtra("COMPANY_NAME", company);
            startActivity(intent);
            finish();
        });
    }
}