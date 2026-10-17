package com.swu.phinma.studentlife.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.swu.phinma.studentlife.*;
import com.swu.phinma.studentlife.adapters.ScholarshipProgramAdapter;
import com.swu.phinma.studentlife.adapters.ScholarshipRequirementsAdapter;
import com.swu.phinma.studentlife.api.ApiClient;
import com.swu.phinma.studentlife.models.ScholarshipProgram;
import com.swu.phinma.studentlife.models.ScholarshipRequirement;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public class ScholarshipFragment extends Fragment {

    private ViewFlipper flipperScholarshipStates;
    
    // State 0: Browse
    private RecyclerView rvAvailablePrograms;

    // State 1: Pending Application
    private TextView tvPendingScholarshipName, tvPendingScholarshipAcadYear, tvPendingGrantStatus;
    private RecyclerView rvPendingRequirements;
    private com.google.android.material.button.MaterialButton btnWithdrawApplication;

    // State 2: Active Scholar
    private TextView tvScholarshipName, tvScholarshipAcadYear, tvScholarshipGrantStatus;
    private TextView tvScholarshipDocCount, tvScholarshipProgressPercent, tvScholarshipCutoff;
    private TextView tvApprovedCount, tvInReviewCount, tvRequiredCount;
    private ProgressBar progressBarCompliance;
    private View layoutScholarshipTabs, tabContentOverview, tabContentRequirements, tabContentDisbursements;
    private TextView tabOverview, tabRequirements, tabDisbursements;
    private RecyclerView rvActiveRequirements;
    private com.google.android.material.button.MaterialButton btnWithdrawActiveScholarship;
    private TextView tvWithdrawalPendingNotice;

    private Integer currentApplicationId = null;
    private String selectedRequirementTitle = "General Document";
    private Integer selectedRequirementId = null;

    private final ActivityResultLauncher<String> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            (Uri uri) -> {
                if (uri != null && getContext() != null) {
                    uploadRequirementDocument(selectedRequirementTitle, selectedRequirementId, uri);
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_scholarship, container, false);
        bindViews(view);
        setupTabs();
        setupClickListeners(view);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchScholarshipCatalogFromApi();
    }

    private void bindViews(View view) {
        flipperScholarshipStates = view.findViewById(R.id.flipperScholarshipStates);

        // State 0
        rvAvailablePrograms = view.findViewById(R.id.rvAvailablePrograms);
        rvAvailablePrograms.setLayoutManager(new LinearLayoutManager(getContext()));

        // State 1
        tvPendingScholarshipName = view.findViewById(R.id.tvPendingScholarshipName);
        tvPendingScholarshipAcadYear = view.findViewById(R.id.tvPendingScholarshipAcadYear);
        tvPendingGrantStatus = view.findViewById(R.id.tvPendingGrantStatus);
        rvPendingRequirements = view.findViewById(R.id.rvPendingRequirements);
        rvPendingRequirements.setLayoutManager(new LinearLayoutManager(getContext()));
        btnWithdrawApplication = view.findViewById(R.id.btnWithdrawApplication);

        // State 2
        tvScholarshipName = view.findViewById(R.id.tvScholarshipName);
        tvScholarshipAcadYear = view.findViewById(R.id.tvScholarshipAcadYear);
        tvScholarshipGrantStatus = view.findViewById(R.id.tvScholarshipGrantStatus);
        tvScholarshipDocCount = view.findViewById(R.id.tvScholarshipDocCount);
        tvScholarshipProgressPercent = view.findViewById(R.id.tvScholarshipProgressPercent);
        tvScholarshipCutoff = view.findViewById(R.id.tvScholarshipCutoff);
        tvApprovedCount = view.findViewById(R.id.tvApprovedCount);
        tvInReviewCount = view.findViewById(R.id.tvInReviewCount);
        tvRequiredCount = view.findViewById(R.id.tvRequiredCount);
        progressBarCompliance = view.findViewById(R.id.progressBarCompliance);

        layoutScholarshipTabs = view.findViewById(R.id.layoutScholarshipTabs);
        tabOverview = view.findViewById(R.id.tabOverview);
        tabRequirements = view.findViewById(R.id.tabRequirements);
        tabDisbursements = view.findViewById(R.id.tabDisbursements);

        tabContentOverview = view.findViewById(R.id.tabContentOverview);
        tabContentRequirements = view.findViewById(R.id.tabContentRequirements);
        tabContentDisbursements = view.findViewById(R.id.tabContentDisbursements);
        
        rvActiveRequirements = view.findViewById(R.id.rvActiveRequirements);
        rvActiveRequirements.setLayoutManager(new LinearLayoutManager(getContext()));
        btnWithdrawActiveScholarship = view.findViewById(R.id.btnWithdrawActiveScholarship);
        tvWithdrawalPendingNotice = view.findViewById(R.id.tvWithdrawalPendingNotice);
    }

    private void setupTabs() {
        View.OnClickListener tabListener = v -> {
            // Reset all tabs
            tabOverview.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.transparent));
            tabOverview.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            tabRequirements.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.transparent));
            tabRequirements.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            tabDisbursements.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.transparent));
            tabDisbursements.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));

            tabContentOverview.setVisibility(View.GONE);
            tabContentRequirements.setVisibility(View.GONE);
            tabContentDisbursements.setVisibility(View.GONE);

            // Activate selected tab
            TextView selected = (TextView) v;
            selected.setBackgroundResource(R.drawable.bg_card_maroon_banner);
            selected.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));

            if (v == tabOverview) {
                tabContentOverview.setVisibility(View.VISIBLE);
            } else if (v == tabRequirements) {
                tabContentRequirements.setVisibility(View.VISIBLE);
            } else if (v == tabDisbursements) {
                tabContentDisbursements.setVisibility(View.VISIBLE);
            }
        };

        tabOverview.setOnClickListener(tabListener);
        tabRequirements.setOnClickListener(tabListener);
        tabDisbursements.setOnClickListener(tabListener);
    }

    private void uploadRequirementDocument(String docType, Integer requirementId, Uri uri) {
        if (getContext() == null) return;
        Toast.makeText(getContext(), "Uploading " + docType + "...", Toast.LENGTH_SHORT).show();

        ApiClient.postMultipart(getContext(), "upload_document.php", docType, uri, null, null, currentApplicationId, requirementId, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String jsonResponse) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Document uploaded successfully! Status: Reviewing", Toast.LENGTH_LONG).show();
                        fetchScholarshipCatalogFromApi();
                    });
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Upload failed: " + errorMessage, Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }

    private void fetchScholarshipCatalogFromApi() {
        if (getContext() == null) return;

        ApiClient.get(getContext(), "scholarships.php", new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String jsonResponse) {
                try {
                    JSONObject obj = new JSONObject(jsonResponse);
                    boolean success = obj.optBoolean("success", false);
                    if (success && obj.has("data")) {
                        JSONObject dataObj = obj.getJSONObject("data");
                        
                        String programName = dataObj.optString("program_name", "No Active Scholarship Assigned");
                        String grantStatus = dataObj.optString("grant_status", "Application Available");
                        
                        // Parse requirements
                        List<ScholarshipRequirement> reqList = new ArrayList<>();
                        if (dataObj.has("requirements")) {
                            JSONArray array = dataObj.getJSONArray("requirements");
                            for (int i = 0; i < array.length(); i++) {
                                JSONObject reqObj = array.getJSONObject(i);
                                reqList.add(new ScholarshipRequirement(
                                    String.valueOf(reqObj.opt("id")),
                                    reqObj.optString("title", "Requirement"),
                                    reqObj.optString("description", ""),
                                    reqObj.optString("status", "UPLOAD")
                                ));
                            }
                        }

                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                if (!dataObj.has("has_active_scholarship") || !dataObj.has("has_pending_application")) {
                                    // Missing critical state fields, fallback to Error State (3)
                                    flipperScholarshipStates.setDisplayedChild(3);
                                    View btnRetry = getView() != null ? getView().findViewById(R.id.btnRetryScholarship) : null;
                                    if (btnRetry != null) {
                                        btnRetry.setOnClickListener(v -> fetchScholarshipCatalogFromApi());
                                    }
                                } else {
                                    try {
                                        boolean hasActive = dataObj.getBoolean("has_active_scholarship");
                                        boolean hasPending = dataObj.getBoolean("has_pending_application");
                                        
                                        if (dataObj.has("application_id") && !dataObj.isNull("application_id")) {
                                            currentApplicationId = dataObj.optInt("application_id");
                                        } else {
                                            currentApplicationId = null;
                                        }

                                        if (hasActive) {
                                            // State 2: Active Scholarship
                                            flipperScholarshipStates.setDisplayedChild(2);
                                            
                                            tvScholarshipName.setText(programName);
                                            tvScholarshipAcadYear.setText(dataObj.optString("academic_year", "AY 2026-2027"));
                                            tvScholarshipGrantStatus.setText("✔ " + grantStatus);
                                            tvScholarshipCutoff.setText("🕒 Next critical cutoff: " + dataObj.optString("next_critical_cutoff", "TBD"));
                                            
                                            int progress = dataObj.optInt("progress_percent", 0);
                                            int verifiedCount = dataObj.optInt("verified_count", 0);
                                            int reviewingCount = dataObj.optInt("reviewing_count", 0);
                                            int requiredCount = dataObj.optInt("required_count", 0);
                                            int totalCount = dataObj.optInt("total_count", 0);
                                            
                                            tvScholarshipProgressPercent.setText(progress + "%");
                                            progressBarCompliance.setProgress(progress);
                                            tvScholarshipDocCount.setText(verifiedCount + " of " + totalCount + " Documents");
                                            tvApprovedCount.setText(String.valueOf(verifiedCount));
                                            tvInReviewCount.setText(String.valueOf(reviewingCount));
                                            tvRequiredCount.setText(String.valueOf(requiredCount));
                                            
                                            if (currentApplicationId != null) {
                                                rvActiveRequirements.setAdapter(new ScholarshipRequirementsAdapter(reqList, req -> {
                                                    selectedRequirementTitle = req.getTitle();
                                                    try {
                                                        selectedRequirementId = Integer.parseInt(req.getId());
                                                    } catch (Exception ignored) {
                                                        selectedRequirementId = null;
                                                    }
                                                    filePickerLauncher.launch("*/*");
                                                }));
                                                rvActiveRequirements.setVisibility(View.VISIBLE);
                                            } else {
                                                rvActiveRequirements.setVisibility(View.GONE);
                                            }

                                            boolean withdrawalPending = dataObj.optBoolean("withdrawal_request_pending", false);
                                            if (btnWithdrawActiveScholarship != null && tvWithdrawalPendingNotice != null) {
                                                if (withdrawalPending) {
                                                    btnWithdrawActiveScholarship.setVisibility(View.GONE);
                                                    tvWithdrawalPendingNotice.setVisibility(View.VISIBLE);
                                                } else {
                                                    btnWithdrawActiveScholarship.setVisibility(View.VISIBLE);
                                                    tvWithdrawalPendingNotice.setVisibility(View.GONE);
                                                    btnWithdrawActiveScholarship.setEnabled(true);
                                                    btnWithdrawActiveScholarship.setText("Request Scholarship Withdrawal");
                                                    btnWithdrawActiveScholarship.setOnClickListener(v -> {
                                                        android.widget.EditText input = new android.widget.EditText(requireContext());
                                                        input.setHint("Enter reason for withdrawal...");
                                                        input.setPadding(40, 20, 40, 20);

                                                        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                                                            .setTitle("Request Scholarship Withdrawal")
                                                            .setMessage("Submitting this request does not immediately cancel your scholarship. Staff will review your reason.\n\nPlease provide a reason for withdrawal:")
                                                            .setView(input)
                                                            .setPositiveButton("Submit Request", (dialog, which) -> {
                                                                String reason = input.getText().toString().trim();
                                                                if (TextUtils.isEmpty(reason)) {
                                                                    Toast.makeText(getContext(), "Withdrawal reason is required.", Toast.LENGTH_SHORT).show();
                                                                    return;
                                                                }

                                                                btnWithdrawActiveScholarship.setEnabled(false);
                                                                btnWithdrawActiveScholarship.setText("Submitting Request...");

                                                                org.json.JSONObject payload = new org.json.JSONObject();
                                                                try {
                                                                    payload.put("action", "request_withdrawal");
                                                                    payload.put("application_id", currentApplicationId);
                                                                    payload.put("reason", reason);
                                                                } catch (Exception ignored) {}

                                                                ApiClient.postJson(getContext(), "scholarships.php", payload.toString(), new ApiClient.ApiCallback() {
                                                                    @Override
                                                                    public void onSuccess(String jsonResponse) {
                                                                        if (getActivity() != null) {
                                                                            getActivity().runOnUiThread(() -> {
                                                                                Toast.makeText(getContext(), "Withdrawal request submitted successfully.", Toast.LENGTH_LONG).show();
                                                                                fetchScholarshipCatalogFromApi();
                                                                            });
                                                                        }
                                                                    }

                                                                    @Override
                                                                    public void onError(String errorMessage) {
                                                                        if (getActivity() != null) {
                                                                            getActivity().runOnUiThread(() -> {
                                                                                btnWithdrawActiveScholarship.setEnabled(true);
                                                                                btnWithdrawActiveScholarship.setText("Request Scholarship Withdrawal");
                                                                                Toast.makeText(getContext(), "Submission failed: " + errorMessage, Toast.LENGTH_LONG).show();
                                                                            });
                                                                        }
                                                                    }
                                                                });
                                                            })
                                                            .setNegativeButton("Cancel", null)
                                                            .show();
                                                    });
                                                }
                                            }
                                            
                                        } else if (hasPending) {
                                            // State 1: Pending Application
                                            flipperScholarshipStates.setDisplayedChild(1);
                                            
                                            tvPendingScholarshipName.setText(programName);
                                            tvPendingScholarshipAcadYear.setText(dataObj.optString("academic_year", "AY 2026-2027"));
                                            tvPendingGrantStatus.setText("● " + dataObj.optString("grant_status", "Under Review"));
                                            
                                            if (currentApplicationId != null) {
                                                rvPendingRequirements.setAdapter(new ScholarshipRequirementsAdapter(reqList, req -> {
                                                    selectedRequirementTitle = req.getTitle();
                                                    try {
                                                        selectedRequirementId = Integer.parseInt(req.getId());
                                                    } catch (Exception ignored) {
                                                        selectedRequirementId = null;
                                                    }
                                                    filePickerLauncher.launch("*/*");
                                                }));
                                                rvPendingRequirements.setVisibility(View.VISIBLE);
                                            } else {
                                                rvPendingRequirements.setVisibility(View.GONE);
                                            }

                                            if (btnWithdrawApplication != null && currentApplicationId != null) {
                                                btnWithdrawApplication.setEnabled(true);
                                                btnWithdrawApplication.setText("Withdraw Application");
                                                btnWithdrawApplication.setOnClickListener(v -> {
                                                    new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                                                        .setTitle("Withdraw Application")
                                                        .setMessage("Are you sure you want to withdraw your application for " + programName + "? This will end your pending application and allow you to reapply while the scholarship remains open.")
                                                        .setPositiveButton("Withdraw", (dialog, which) -> {
                                                            btnWithdrawApplication.setEnabled(false);
                                                            btnWithdrawApplication.setText("Withdrawing...");

                                                            org.json.JSONObject payload = new org.json.JSONObject();
                                                            try {
                                                                payload.put("action", "withdraw");
                                                                payload.put("application_id", currentApplicationId);
                                                            } catch (Exception ignored) {}

                                                            ApiClient.postJson(getContext(), "scholarships.php", payload.toString(), new ApiClient.ApiCallback() {
                                                                @Override
                                                                public void onSuccess(String jsonResponse) {
                                                                    if (getActivity() != null) {
                                                                        getActivity().runOnUiThread(() -> {
                                                                            Toast.makeText(getContext(), "Application withdrawn successfully.", Toast.LENGTH_SHORT).show();
                                                                            fetchScholarshipCatalogFromApi();
                                                                        });
                                                                    }
                                                                }

                                                                @Override
                                                                public void onError(String errorMessage) {
                                                                    if (getActivity() != null) {
                                                                        getActivity().runOnUiThread(() -> {
                                                                            btnWithdrawApplication.setEnabled(true);
                                                                            btnWithdrawApplication.setText("Withdraw Application");
                                                                            Toast.makeText(getContext(), "Withdrawal failed: " + errorMessage, Toast.LENGTH_LONG).show();
                                                                        });
                                                                    }
                                                                }
                                                            });
                                                        })
                                                        .setNegativeButton("Cancel", null)
                                                        .show();
                                                });
                                            }
                                            
                                        } else {
                                            // State 0: Browse Catalog
                                            flipperScholarshipStates.setDisplayedChild(0);
                                            
                                            List<ScholarshipProgram> availList = new ArrayList<>();
                                            if (dataObj.has("available_programs")) {
                                                JSONArray arr = dataObj.optJSONArray("available_programs");
                                                if (arr != null) {
                                                    for (int i = 0; i < arr.length(); i++) {
                                                        JSONObject p = arr.optJSONObject(i);
                                                        if (p != null) {
                                                            availList.add(new ScholarshipProgram(
                                                                String.valueOf(p.opt("id")),
                                                                p.optString("code", ""),
                                                                p.optString("title", ""),
                                                                p.optString("description", ""),
                                                                p.optString("academic_year", ""),
                                                                p.optString("deadline", ""),
                                                                p.optString("stipend_amount", ""),
                                                                p.optBoolean("is_active", true),
                                                                p.optBoolean("is_expired", false)
                                                            ));
                                                        }
                                                    }
                                                }
                                            }

                                            rvAvailablePrograms.setAdapter(new ScholarshipProgramAdapter(availList, program -> {
                                                Intent intent = new Intent(getActivity(), ScholarshipDetailsActivity.class);
                                                intent.putExtra("program", program);
                                                startActivity(intent);
                                            }));
                                        }
                                    } catch (org.json.JSONException e) {
                                        flipperScholarshipStates.setDisplayedChild(3);
                                    }
                                }
                            });
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (errorMessage != null && (errorMessage.contains("401") || errorMessage.contains("403"))) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            ApiClient.clearAuthToken(requireContext());
                            Toast.makeText(requireContext(), "Session expired. Please sign in again.", Toast.LENGTH_LONG).show();
                            Intent intent = new Intent(getActivity(), LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        });
                    }
                }
            }
        });
    }

    private void setupClickListeners(View view) {
        view.findViewById(R.id.btnScholarshipNotifications).setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), NotificationsActivity.class));
        });
    }
}
