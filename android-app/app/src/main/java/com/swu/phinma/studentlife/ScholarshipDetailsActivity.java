package com.swu.phinma.studentlife;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.swu.phinma.studentlife.models.ScholarshipProgram;

public class ScholarshipDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scholarship_details);

        ScholarshipProgram program = (ScholarshipProgram) getIntent().getSerializableExtra("program");
        if (program == null) {
            finish();
            return;
        }

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        TextView tvDetailCode = findViewById(R.id.tvDetailCode);
        TextView tvDetailTitle = findViewById(R.id.tvDetailTitle);
        TextView tvDetailAcadYear = findViewById(R.id.tvDetailAcadYear);
        TextView badgeDetailStatus = findViewById(R.id.badgeDetailStatus);
        TextView tvDetailDeadline = findViewById(R.id.tvDetailDeadline);
        TextView tvDetailDescription = findViewById(R.id.tvDetailDescription);
        TextView tvDetailStipend = findViewById(R.id.tvDetailStipend);

        tvDetailCode.setText(program.getCode());
        tvDetailTitle.setText(program.getTitle());
        tvDetailAcadYear.setText(program.getAcademicYear());
        tvDetailDeadline.setText(program.getDeadline());
        tvDetailDescription.setText(program.getDescription());
        tvDetailStipend.setText(program.getStipendAmount());

        if (program.isActive() && !program.isExpired()) {
            badgeDetailStatus.setText("Available");
            badgeDetailStatus.setBackgroundResource(R.drawable.bg_pill_active);
            badgeDetailStatus.setTextColor(ContextCompat.getColor(this, R.color.status_green_text));
        } else {
            badgeDetailStatus.setText(program.isExpired() ? "Deadline Passed" : "Inactive");
            badgeDetailStatus.setBackgroundResource(R.drawable.bg_pill_rejected);
            badgeDetailStatus.setTextColor(ContextCompat.getColor(this, R.color.secondary_ruby));
        }

        com.google.android.material.button.MaterialButton btnApplyNow = findViewById(R.id.btnApplyNow);
        if (program.isActive() && !program.isExpired()) {
            btnApplyNow.setEnabled(true);
            btnApplyNow.setText("Apply for Scholarship");
            btnApplyNow.setOnClickListener(v -> {
                btnApplyNow.setEnabled(false);
                btnApplyNow.setText("Submitting Application...");

                org.json.JSONObject payload = new org.json.JSONObject();
                try {
                    payload.put("scholarship_id", Integer.parseInt(program.getId()));
                    payload.put("application_type", "New Application");
                } catch (Exception e) {
                    btnApplyNow.setEnabled(true);
                    btnApplyNow.setText("Apply for Scholarship");
                    android.widget.Toast.makeText(this, "Invalid scholarship reference", android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }

                com.swu.phinma.studentlife.api.ApiClient.postJson(this, "scholarships.php", payload.toString(), new com.swu.phinma.studentlife.api.ApiClient.ApiCallback() {
                    @Override
                    public void onSuccess(String jsonResponse) {
                        runOnUiThread(() -> {
                            android.widget.Toast.makeText(ScholarshipDetailsActivity.this, "Scholarship application submitted successfully!", android.widget.Toast.LENGTH_LONG).show();
                            finish();
                        });
                    }

                    @Override
                    public void onError(String errorMessage) {
                        runOnUiThread(() -> {
                            btnApplyNow.setEnabled(true);
                            btnApplyNow.setText("Apply for Scholarship");
                            String userMsg = errorMessage;
                            try {
                                int jsonStart = errorMessage.indexOf('{');
                                if (jsonStart >= 0) {
                                    org.json.JSONObject errJson = new org.json.JSONObject(errorMessage.substring(jsonStart));
                                    if (errJson.has("message")) {
                                        userMsg = errJson.getString("message");
                                    }
                                }
                            } catch (Exception ignored) {}
                            android.widget.Toast.makeText(ScholarshipDetailsActivity.this, userMsg, android.widget.Toast.LENGTH_LONG).show();
                        });
                    }
                });
            });
        } else {
            btnApplyNow.setEnabled(false);
            btnApplyNow.setText(program.isExpired() ? "Application Deadline Passed" : "Program Closed");
        }
    }
}
