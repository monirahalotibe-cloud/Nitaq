package com.example.myapplication;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity {

    private TextView tvWelcomeUser, tvSelectedCompany, tvStatus, tvPinLabel, tvMapLabel;
    private TextView tvTime, tvPresentDays, tvAbsentDays, tvCheckInTimeRecord, tvWeeklyRecordTitle;
    private Button btnCheckIn, btnCheckOut, btnLangToggle, btnLeaveRequest, btnExportPdf, btnManagerDashboard;

    private int attendanceCount = 4;
    private int absenceCount = 1;
    private String lastRecordText = "";
    private boolean isEnglish = false;
    private String currentUsername = "الموظف";
    private String currentCompanyName = "شركة أرامكو السعودية";

    private final Handler handler = new Handler();
    private ActivityResultLauncher<Intent> filePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ربط جميع عناصر الواجهة
        tvWelcomeUser = findViewById(R.id.tvWelcomeUser);
        tvSelectedCompany = findViewById(R.id.tvSelectedCompany);
        tvStatus = findViewById(R.id.tvStatus);
        tvPinLabel = findViewById(R.id.tvPinLabel);

        btnCheckIn = findViewById(R.id.btnCheckIn);
        btnCheckOut = findViewById(R.id.btnCheckOut);
        btnLangToggle = findViewById(R.id.btnLangToggle);
        btnLeaveRequest = findViewById(R.id.btnLeaveRequest);
        btnExportPdf = findViewById(R.id.btnExportPdf);
        btnManagerDashboard = findViewById(R.id.btnManagerDashboard);

        tvTime = findViewById(R.id.tvTime);
        tvPresentDays = findViewById(R.id.tvPresentDays);
        tvAbsentDays = findViewById(R.id.tvAbsentDays);
        tvCheckInTimeRecord = findViewById(R.id.tvCheckInTimeRecord);

        // التقاط الملف المرفق للعذر
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            Toast.makeText(this, isEnglish ? "File attached successfully ✔️" : "تم إرفاق الملف بنجاح ✔️", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        startClockThread();

        if (getIntent().hasExtra("COMPANY_NAME")) {
            currentCompanyName = getIntent().getStringExtra("COMPANY_NAME");
        }
        if (getIntent().hasExtra("USERNAME")) {
            String uname = getIntent().getStringExtra("USERNAME");
            if (uname != null && !uname.isEmpty()) currentUsername = uname;
        }

        updateUiLanguage();

        // 1. تسجيل الحضور بالبصمة الحيوية
        btnCheckIn.setOnClickListener(v -> authenticateBiometric(true));

        // 2. تسجيل الانصراف بالبصمة الحيوية
        btnCheckOut.setOnClickListener(v -> authenticateBiometric(false));

        // 3. كروت الحضور والغياب وآخر تسجيل
        tvPresentDays.setOnClickListener(v -> showAttendanceDetailsDialog());
        tvAbsentDays.setOnClickListener(v -> showAbsenceExcuseDialog());
        tvCheckInTimeRecord.setOnClickListener(v -> showLastRecordDialog());

        // 4. تقديم طلب إجازة
        btnLeaveRequest.setOnClickListener(v -> showLeaveRequestDialog());

        // 5. تصدير تقرير PDF
        btnExportPdf.setOnClickListener(v -> {
            Toast.makeText(this, isEnglish ? "Attendance report exported (PDF) successfully 📄" : "تم تصدير تقرير الحضور بصيغة PDF بنجاح 📄", Toast.LENGTH_LONG).show();
        });

        // 6. تسجيل دخول المدير ولوحة التحكم
        btnManagerDashboard.setOnClickListener(v -> showManagerLoginDialog());

        // 7. زر تبديل اللغة الفعلي (عربي / إنجليزي)
        btnLangToggle.setOnClickListener(v -> {
            isEnglish = !isEnglish;
            updateUiLanguage();
        });
    }

    // تحديث كافة نصوص الواجهة بناءً على اللغة المختارة
    private void updateUiLanguage() {
        btnLangToggle.setText(isEnglish ? "AR" : "EN");

        if (isEnglish) {
            tvWelcomeUser.setText("Welcome, " + currentUsername);
            tvSelectedCompany.setText("Workplace: " + currentCompanyName);
            tvPinLabel.setText("HQ");
            tvStatus.setText("Press to confirm attendance via Biometrics inside geofence");
            btnCheckIn.setText("Check In");
            btnCheckOut.setText("Check Out");
            btnLeaveRequest.setText("🏖️ Request Leave / Permission");
            btnExportPdf.setText("📄 Export Attendance Report (PDF)");
            btnManagerDashboard.setText("👨‍💼 Manager Dashboard");
        } else {
            tvWelcomeUser.setText("مرحباً بك، " + currentUsername);
            tvSelectedCompany.setText("جهة العمل: " + currentCompanyName);
            tvPinLabel.setText(currentCompanyName);
            tvStatus.setText("اضغط لتأكيد الحضور بالبصمة الحيوية داخل النطاق");
            btnCheckIn.setText("تسجيل الحضور");
            btnCheckOut.setText("تسجيل الانصراف");
            btnLeaveRequest.setText("🏖️ تقديم طلب إجازة / استئذان");
            btnExportPdf.setText("📄 تصدير تقرير الحضور (PDF)");
            btnManagerDashboard.setText("👨‍💼 لوحة تحكم المدير");
        }
        updateCardsText();
    }

    private void updateCardsText() {
        String displayRecord;

        if (lastRecordText.isEmpty()) {
            displayRecord = isEnglish ? "Not recorded today" : "لم يسجل اليوم";
        } else {
            displayRecord = lastRecordText.contains("\n") ? lastRecordText.split("\n")[0] : lastRecordText;
        }

        if (isEnglish) {
            tvPresentDays.setText("Present Days\n" + attendanceCount + " Days");
            tvAbsentDays.setText("Absent Days\n" + absenceCount + " Day");
            tvCheckInTimeRecord.setText("Last Check-in\n" + displayRecord);
        } else {
            tvPresentDays.setText("أيام الحضور\n" + attendanceCount + " أيام");
            tvAbsentDays.setText("أيام الغياب\n" + absenceCount + " يوم");
            tvCheckInTimeRecord.setText("آخر تسجيل\n" + displayRecord);
        }
    }


    private void authenticateBiometric(boolean isCheckIn) {
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(MainActivity.this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                Locale locale = isEnglish ? Locale.ENGLISH : new Locale("ar");
                String currentDate = new SimpleDateFormat("yyyy/MM/dd", locale).format(new Date());
                String currentDay = new SimpleDateFormat("EEEE", locale).format(new Date());
                String currentTime = new SimpleDateFormat("hh:mm:ss a", locale).format(new Date());

                if (isCheckIn) {
                    tvStatus.setText(isEnglish ? "Check-in verified via Biometrics ✔️" : "تم تأكيد الحضور بالبصمة الحيوية ✔️");
                    tvStatus.setTextColor(Color.parseColor("#2A9D7C"));
                    attendanceCount++;
                    lastRecordText = (isEnglish ? "In: " : "حضور: ") + currentDay + " (" + currentDate + ")\n" + (isEnglish ? "Time: " : "الساعة: ") + currentTime;

                    btnCheckIn.setEnabled(false);
                    btnCheckIn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.GRAY));
                    btnCheckOut.setEnabled(true);
                    Toast.makeText(MainActivity.this, isEnglish ? "Checked in successfully!" : "تم تسجيل الحضور بنجاح!", Toast.LENGTH_SHORT).show();
                } else {
                    tvStatus.setText(isEnglish ? "Checked out successfully ✔️" : "تم تسجيل الانصراف بنجاح ✔️");
                    tvStatus.setTextColor(Color.parseColor("#D32F2F"));
                    lastRecordText += "\n" + (isEnglish ? "Out: Time " : "انصراف: الساعة ") + currentTime;

                    btnCheckOut.setEnabled(false);
                    btnCheckOut.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.GRAY));
                    Toast.makeText(MainActivity.this, isEnglish ? "Checked out successfully!" : "تم تسجيل الانصراف بنجاح!", Toast.LENGTH_SHORT).show();
                }
                updateCardsText();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(MainActivity.this, isEnglish ? "Biometric authentication failed" : "فشلت بصمة الإصبع/الوجه، حاول مجدداً", Toast.LENGTH_SHORT).show();
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(isCheckIn ? (isEnglish ? "Confirm Check-in" : "تأكيد بصمة الحضور") : (isEnglish ? "Confirm Check-out" : "تأكيد بصمة الانصراف"))
                .setSubtitle(isEnglish ? "Use fingerprint/Face ID to verify employee identity" : "استخدم البصمة الحيوية للتحقق من هوية الموظف")
                .setNegativeButtonText(isEnglish ? "Cancel" : "إلغاء")
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    // نافذة تسجيل دخول المدير أولاً
    private void showManagerLoginDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(isEnglish ? "Manager Login" : "تسجيل دخول المدير");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 10);

        final EditText etManagerPass = new EditText(this);
        etManagerPass.setHint(isEnglish ? "Enter Manager Password (1234)" : "أدخل كلمة مرور المدير (1234)");
        etManagerPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etManagerPass);

        builder.setView(layout);

        builder.setPositiveButton(isEnglish ? "Login" : "دخول", (dialog, which) -> {
            String pass = etManagerPass.getText().toString().trim();
            // كلمة المرور الافتراضية للمدير هي 1234
            if (pass.equals("1234")) {
                Toast.makeText(MainActivity.this, isEnglish ? "Login successful ✔️" : "تم تسجيل دخول المشرف بنجاح ✔️", Toast.LENGTH_SHORT).show();
                showManagerDashboardDialog();
            } else {
                Toast.makeText(MainActivity.this, isEnglish ? "Incorrect Password!" : "كلمة المرور غير صحيحة!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton(isEnglish ? "Cancel" : "إلغاء", null);
        builder.show();
    }

    // لوحة تحكم المدير بعد الدخول الناجح
    private void showManagerDashboardDialog() {
        String teamStatus = isEnglish ?
                "👨‍💼 Team Status (Manager View):\n\n" +
                "• Ahmed Ali: Present (07:50 AM)\n" +
                "• Sara Khaled: Present (08:02 AM)\n" +
                "• Mohammed Al-Omari: Absent (Medical Excuse Attached 📄)\n" +
                "• Fahad Al-Salem: Present (07:58 AM)" :
                "👨‍💼 لوحة متابعة الفريق (المدير):\n\n" +
                "• أحمد علي: حاضر (07:50 ص)\n" +
                "• سارة خالد: حاضرة (08:02 ص)\n" +
                "• محمد العمري: غائب (قدم عذر طبي 📄)\n" +
                "• فهد السالم: حاضر (07:58 ص)";

        new AlertDialog.Builder(this)
                .setTitle(isEnglish ? "Manager Dashboard" : "لوحة تحكم المشرف")
                .setMessage(teamStatus)
                .setPositiveButton(isEnglish ? "Approve Excuses" : "اعتماد الأعذار", (dialog, which) ->
                        Toast.makeText(this, isEnglish ? "Excuses approved ✔️" : "تم اعتماد الأعذار بنجاح ✔️", Toast.LENGTH_SHORT).show())
                .setNegativeButton(isEnglish ? "Close" : "إغلاق", null)
                .show();
    }

    private void showAttendanceDetailsDialog() {
        String details = isEnglish ?
                "📅 Weekly Attendance Details:\n\n" +
                "• Sun (2026/09/20):\n  In: 07:55 AM | Out: 04:02 PM\n\n" +
                "• Mon (2026/09/21):\n  In: 08:01 AM | Out: 04:10 PM\n\n" +
                "• Tue (2026/09/22):\n  In: 07:48 AM | Out: 04:00 PM\n\n" +
                "• Wed (2026/09/23):\n  In: 07:59 AM | Out: 04:05 PM" :
                "📅 سجل الحضور الأسبوعي التفصيلي:\n\n" +
                "• الأحد (2026/09/20):\n  حضور: 07:55 ص | انصراف: 04:02 م\n\n" +
                "• الإثنين (2026/09/21):\n  حضور: 08:01 ص | انصراف: 04:10 م\n\n" +
                "• الثلاثاء (2026/09/22):\n  حضور: 07:48 ص | انصراف: 04:00 م\n\n" +
                "• الأربعاء (2026/09/23):\n  حضور: 07:59 ص | انصراف: 04:05 م";

        new AlertDialog.Builder(this)
                .setTitle(isEnglish ? "Attendance Log" : "سجل أيام الحضور")
                .setMessage(details)
                .setPositiveButton(isEnglish ? "Close" : "إغلاق", null)
                .show();
    }

    private void showAbsenceExcuseDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(isEnglish ? "Submit Absence Excuse" : "تقديم عذر غياب");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 10);

        final EditText etExcuse = new EditText(this);
        etExcuse.setHint(isEnglish ? "Reason for excuse..." : "سبب العذر...");
        layout.addView(etExcuse);

        Button btnAttach = new Button(this);
        btnAttach.setText(isEnglish ? "📄 Attach Medical File" : "📄 إرفاق ملف / تقرير طبي");
        btnAttach.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            filePickerLauncher.launch(intent);
        });
        layout.addView(btnAttach);

        builder.setView(layout);
        builder.setPositiveButton(isEnglish ? "Submit" : "إرسال", (dialog, which) ->
                Toast.makeText(MainActivity.this, isEnglish ? "Excuse submitted for review ✔️" : "تم إرسال العذر للمراجعة ✔️", Toast.LENGTH_SHORT).show());
        builder.setNegativeButton(isEnglish ? "Cancel" : "إلغاء", null);
        builder.show();
    }

    private void showLeaveRequestDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(isEnglish ? "Leave Request" : "طلب إجازة / استئذان");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 10);

        final EditText etReason = new EditText(this);
        etReason.setHint(isEnglish ? "Leave type (Medical / Annual)..." : "نوع الإجازة (مرضية / اعتيادية / استئذان)...");
        layout.addView(etReason);

        builder.setView(layout);
        builder.setPositiveButton(isEnglish ? "Submit Request" : "تقديم الطلب", (dialog, which) ->
                Toast.makeText(MainActivity.this, isEnglish ? "Leave request submitted!" : "تم تقديم طلب الإجازة بنجاح!", Toast.LENGTH_SHORT).show());
        builder.setNegativeButton(isEnglish ? "Cancel" : "إلغاء", null);
        builder.show();
    }

    private void showLastRecordDialog() {
        new AlertDialog.Builder(this)
                .setTitle(isEnglish ? "Last Record Details" : "تفاصيل آخر بصمة")
                .setMessage((isEnglish ? "📍 Details of last check-in:\n\n" : "📍 تفاصيل آخر عملية:\n\n") + lastRecordText)
                .setPositiveButton(isEnglish ? "OK" : "حسناً", null)
                .show();
    }

    private void startClockThread() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Locale locale = isEnglish ? Locale.ENGLISH : new Locale("ar");
                String currentTime = new SimpleDateFormat("yyyy/MM/dd | hh:mm:ss a", locale).format(new Date());
                tvTime.setText((isEnglish ? "Current Time: " : "التاريخ والوقت: ") + currentTime);
                handler.postDelayed(this, 1000);
            }
        });
    }
}