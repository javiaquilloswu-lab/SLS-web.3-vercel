package com.swu.phinma.studentlife;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.swu.phinma.studentlife.api.ApiClient;

/**
 * Sign Up / Create Account screen for Student Life.
 * Matches student_life_create_account.png.
 */
public class SignUpActivity extends AppCompatActivity {

    private EditText etFirstName, etLastName, etSignUpStudentId, etEmail, etSignUpPassword;
    private CheckBox cbTerms;
    private ImageView ivSignUpTogglePassword;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etSignUpStudentId = findViewById(R.id.etSignUpStudentId);
        etEmail = findViewById(R.id.etEmail);
        etSignUpPassword = findViewById(R.id.etSignUpPassword);
        cbTerms = findViewById(R.id.cbTerms);
        ivSignUpTogglePassword = findViewById(R.id.ivSignUpTogglePassword);

        if (ivSignUpTogglePassword != null) {
            ivSignUpTogglePassword.setOnClickListener(v -> {
                isPasswordVisible = !isPasswordVisible;
                if (isPasswordVisible) {
                    etSignUpPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                } else {
                    etSignUpPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
                }
                etSignUpPassword.setSelection(etSignUpPassword.getText().length());
            });
        }

        findViewById(R.id.tvLinkSignIn).setOnClickListener(v -> {
            startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
            finish();
        });

        findViewById(R.id.btnDoSignUp).setOnClickListener(v -> attemptSignUp());
    }

    private void attemptSignUp() {
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String studentId = etSignUpStudentId.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etSignUpPassword.getText().toString().trim();

        if (TextUtils.isEmpty(firstName)) {
            etFirstName.setError("First name is required");
            return;
        }
        if (TextUtils.isEmpty(lastName)) {
            etLastName.setError("Last name is required");
            return;
        }
        if (TextUtils.isEmpty(studentId)) {
            etSignUpStudentId.setError("Student ID is required");
            return;
        }
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("University email is required");
            return;
        }
        String lowerEmail = email.toLowerCase();
        if (!lowerEmail.endsWith(".swu@phinmaed.com")) {
            etEmail.setError("Please use your SWU PHINMA student email (example: name.swu@phinmaed.com).");
            return;
        }
        if (password.length() < 8 || !password.matches("^(?=.*[A-Za-z])(?=.*\\d).+$")) {
            etSignUpPassword.setError("Minimum 8 characters with letters & numbers");
            return;
        }
        if (!cbTerms.isChecked()) {
            Toast.makeText(this, "Please agree to the Terms of Service", Toast.LENGTH_SHORT).show();
            return;
        }

        // Network call to PHP register.php with PostgreSQL
        org.json.JSONObject payload = new org.json.JSONObject();
        try {
            payload.put("first_name", firstName);
            payload.put("last_name", lastName);
            payload.put("student_id", studentId);
            payload.put("email", email);
            payload.put("password", password);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to format registration payload", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable button during network request to prevent double-submission
        final android.widget.Button btnSignUp = findViewById(R.id.btnDoSignUp);
        if (btnSignUp != null) {
            btnSignUp.setEnabled(false);
            btnSignUp.setText("Registering...");
        }

        ApiClient.postJson(this, "register.php", payload.toString(), new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String jsonResponse) {
                try {
                    org.json.JSONObject obj = new org.json.JSONObject(jsonResponse);
                    boolean success = obj.optBoolean("success", false);
                    String message = obj.optString("message", "Registration failed");

                    if (success) {
                        // Automatically authenticate using login.php
                        org.json.JSONObject loginPayload = new org.json.JSONObject();
                        try {
                            loginPayload.put("student_id", studentId);
                            loginPayload.put("password", password);
                        } catch (Exception ignored) {}

                        runOnUiThread(() -> {
                            if (btnSignUp != null) {
                                btnSignUp.setText("Logging in...");
                            }
                        });

                        ApiClient.postJson(SignUpActivity.this, "login.php", loginPayload.toString(), new ApiClient.ApiCallback() {
                            @Override
                            public void onSuccess(String loginResponse) {
                                try {
                                    org.json.JSONObject loginObj = new org.json.JSONObject(loginResponse);
                                    boolean loginSuccess = loginObj.optBoolean("success", false);
                                    if (loginSuccess && loginObj.has("data")) {
                                        org.json.JSONObject dataObj = loginObj.getJSONObject("data");
                                        String token = dataObj.optString("token", "");
                                        if (!TextUtils.isEmpty(token)) {
                                            ApiClient.setAuthToken(SignUpActivity.this, token);
                                            if (dataObj.has("student")) {
                                                ApiClient.saveStudentProfile(SignUpActivity.this, dataObj.getJSONObject("student"));
                                            }
                                            runOnUiThread(() -> {
                                                Toast.makeText(SignUpActivity.this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                                                Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
                                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                startActivity(intent);
                                                finish();
                                            });
                                            return;
                                        }
                                    }
                                    fallbackToLogin();
                                } catch (Exception e) {
                                    fallbackToLogin();
                                }
                            }

                            @Override
                            public void onError(String errorMessage) {
                                fallbackToLogin();
                            }

                            private void fallbackToLogin() {
                                runOnUiThread(() -> {
                                    Toast.makeText(SignUpActivity.this, "Account created successfully. Please sign in to continue.", Toast.LENGTH_LONG).show();
                                    Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                });
                            }
                        });
                        return;
                    }

                    // Strict Error Behavior: Remain on SignUpActivity
                    runOnUiThread(() -> {
                        if (btnSignUp != null) {
                            btnSignUp.setEnabled(true);
                            btnSignUp.setText("Sign Up");
                        }
                        Toast.makeText(SignUpActivity.this, message, Toast.LENGTH_LONG).show();
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        if (btnSignUp != null) {
                            btnSignUp.setEnabled(true);
                            btnSignUp.setText("Sign Up");
                        }
                        Toast.makeText(SignUpActivity.this, "Response parsing error.", Toast.LENGTH_LONG).show();
                    });
                }
            }

            @Override
            public void onError(String errorMessage) {
                // Strict Error Behavior: Remain on SignUpActivity
                runOnUiThread(() -> {
                    if (btnSignUp != null) {
                        btnSignUp.setEnabled(true);
                        btnSignUp.setText("Sign Up");
                    }
                    Toast.makeText(SignUpActivity.this, "Registration failed: " + errorMessage, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
}
