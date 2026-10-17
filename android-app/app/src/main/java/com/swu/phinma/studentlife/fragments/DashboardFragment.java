package com.swu.phinma.studentlife.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.swu.phinma.studentlife.*;
import com.swu.phinma.studentlife.adapters.PendingActionsAdapter;
import com.swu.phinma.studentlife.adapters.RecentRequestsAdapter;
import com.swu.phinma.studentlife.api.ApiClient;
import com.swu.phinma.studentlife.models.PendingAction;
import com.swu.phinma.studentlife.models.RecentRequest;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Student Life Dashboard Fragment.
 * Features separated, live-calculated Scholarship Summary and Clearance Requests Summary cards.
 */
public class DashboardFragment extends Fragment {

    private TextView tvStudentName, tvStudentInfo, badgeActiveStatus;
    private TextView tvDashboardScholarshipTitle, tvDashboardScholarshipTerm, badgeDashboardScholarshipStatus;
    private TextView tvDashboardRequestTitle, tvDashboardRequestRef, badgeDashboardRequestStatus;
    private RecyclerView rvPendingActions, rvRecentRequests;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);
        bindViews(view);
        populateStudentProfileData();
        fetchLiveProfileFromDatabase();
        fetchLiveScholarshipSummaryFromApi();
        fetchLiveServiceRequestsSummaryFromApi();
        fetchLivePendingActionsFromApi();
        setupRecyclerViews();
        setupClickListeners(view);
        return view;
    }

    private void bindViews(View view) {
        tvStudentName = view.findViewById(R.id.tvStudentName);
        tvStudentInfo = view.findViewById(R.id.tvStudentInfo);
        badgeActiveStatus = view.findViewById(R.id.badgeActiveStatus);

        tvDashboardScholarshipTitle = view.findViewById(R.id.tvDashboardScholarshipTitle);
        tvDashboardScholarshipTerm = view.findViewById(R.id.tvDashboardScholarshipTerm);
        badgeDashboardScholarshipStatus = view.findViewById(R.id.badgeDashboardScholarshipStatus);

        tvDashboardRequestTitle = view.findViewById(R.id.tvDashboardRequestTitle);
        tvDashboardRequestRef = view.findViewById(R.id.tvDashboardRequestRef);
        badgeDashboardRequestStatus = view.findViewById(R.id.badgeDashboardRequestStatus);

        rvPendingActions = view.findViewById(R.id.rvPendingActions);
        rvRecentRequests = view.findViewById(R.id.rvRecentRequests);
    }

    private void populateStudentProfileData() {
        String[] savedProfile = ApiClient.getSavedStudentProfile(getContext());
        if (savedProfile != null && savedProfile.length >= 6) {
            String fullName = savedProfile[0];
            String studentNum = savedProfile[1];
            String course = savedProfile[2];
            String year = savedProfile[3];
            String status = savedProfile[5];

            if (tvStudentName != null) tvStudentName.setText(fullName);
            if (tvStudentInfo != null) tvStudentInfo.setText("ID: " + studentNum + " • " + course + " • " + year);
            if (badgeActiveStatus != null) badgeActiveStatus.setText("✔ " + status);
        } else {
            if (tvStudentName != null) tvStudentName.setText("SWU Student");
            if (tvStudentInfo != null) tvStudentInfo.setText("ID: Loading... • BS Computer Science • 3rd Year");
            if (badgeActiveStatus != null) badgeActiveStatus.setText("✔ Active");
        }
    }

    private void fetchLiveProfileFromDatabase() {
        if (getContext() == null) return;
        ApiClient.get(getContext(), "get_student.php", new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String jsonResponse) {
                try {
                    JSONObject obj = new JSONObject(jsonResponse);
                    if (obj.optBoolean("success", false) && obj.has("data")) {
                        JSONObject studentData = obj.getJSONObject("data");
                        ApiClient.saveStudentProfile(getContext(), studentData);
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> populateStudentProfileData());
                        }
                    }
                } catch (Exception ignored) {}
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

    private void fetchLiveScholarshipSummaryFromApi() {
        if (getContext() == null) return;

        ApiClient.get(getContext(), "scholarships.php", new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String jsonResponse) {
                try {
                    JSONObject obj = new JSONObject(jsonResponse);
                    if (obj.optBoolean("success", false) && obj.has("data")) {
                        JSONObject dataObj = obj.getJSONObject("data");
                        String programName = dataObj.optString("program_name", "No active scholarship application yet");
                        String acadYear = dataObj.optString("academic_year", "AY 2026–2027");
                        String grantStatus = dataObj.optString("grant_status", "Application Available");

                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                if (tvDashboardScholarshipTitle != null) tvDashboardScholarshipTitle.setText(programName);
                                if (tvDashboardScholarshipTerm != null) tvDashboardScholarshipTerm.setText(acadYear);
                                if (badgeDashboardScholarshipStatus != null) {
                                    badgeDashboardScholarshipStatus.setText(grantStatus);
                                    if ("Active Scholar".equalsIgnoreCase(grantStatus) || grantStatus.contains("Active")) {
                                        badgeDashboardScholarshipStatus.setBackgroundResource(R.drawable.bg_pill_active);
                                        badgeDashboardScholarshipStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_green_text));
                                    } else {
                                        badgeDashboardScholarshipStatus.setBackgroundResource(R.drawable.bg_pill_processing);
                                        badgeDashboardScholarshipStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_blue_text));
                                    }
                                }
                            });
                        }
                    }
                } catch (Exception ignored) {}
            }

            @Override
            public void onError(String errorMessage) {}
        });
    }

    private void fetchLiveServiceRequestsSummaryFromApi() {
        if (getContext() == null) return;

        ApiClient.get(getContext(), "requests.php", new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String jsonResponse) {
                try {
                    JSONObject obj = new JSONObject(jsonResponse);
                    if (obj.has("data")) {
                        JSONArray array = obj.getJSONArray("data");
                        List<RecentRequest> list = new ArrayList<>();

                        for (int i = 0; i < Math.min(array.length(), 5); i++) {
                            JSONObject item = array.getJSONObject(i);
                            String id = String.valueOf(item.opt("id"));
                            String title = item.optString("title", "Request");
                            String refNum = item.optString("reference_number", "SL-2026-REG");
                            String date = item.optString("submission_date", "Recent");
                            String status = item.optString("status", "Processing");
                            String statusMsg = item.optString("status_message", "In processing");

                            list.add(new RecentRequest(id, title, refNum, date, status, statusMsg));
                        }

                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                if (array.length() > 0) {
                                    JSONObject topReq = array.optJSONObject(0);
                                    if (topReq != null) {
                                        if (tvDashboardRequestTitle != null) tvDashboardRequestTitle.setText(topReq.optString("title", "Clearance Request"));
                                        if (tvDashboardRequestRef != null) tvDashboardRequestRef.setText("Ref: " + topReq.optString("reference_number", "SL-2026-REG"));
                                        if (badgeDashboardRequestStatus != null) {
                                            String st = topReq.optString("status", "Processing");
                                            badgeDashboardRequestStatus.setText("● " + st);
                                            if ("Completed".equalsIgnoreCase(st) || "Approved".equalsIgnoreCase(st)) {
                                                badgeDashboardRequestStatus.setBackgroundResource(R.drawable.bg_pill_active);
                                                badgeDashboardRequestStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_green_text));
                                            } else {
                                                badgeDashboardRequestStatus.setBackgroundResource(R.drawable.bg_pill_processing);
                                                badgeDashboardRequestStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_blue_text));
                                            }
                                        }
                                    }
                                } else {
                                    if (tvDashboardRequestTitle != null) tvDashboardRequestTitle.setText("No active clearance request yet");
                                    if (tvDashboardRequestRef != null) tvDashboardRequestRef.setText("Select a service to get started");
                                    if (badgeDashboardRequestStatus != null) {
                                        badgeDashboardRequestStatus.setText("○ Available");
                                        badgeDashboardRequestStatus.setBackgroundResource(R.drawable.bg_pill_processing);
                                        badgeDashboardRequestStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_blue_text));
                                    }
                                }

                                if (rvRecentRequests != null) {
                                    rvRecentRequests.setAdapter(new RecentRequestsAdapter(list, req -> {
                                        startActivity(new Intent(getActivity(), RequestHistoryActivity.class));
                                    }));
                                }
                            });
                        }
                    }
                } catch (Exception ignored) {}
            }

            @Override
            public void onError(String errorMessage) {}
        });
    }

    private void fetchLivePendingActionsFromApi() {
        if (getContext() == null) return;

        ApiClient.get(getContext(), "scholarships.php", new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String jsonResponse) {
                try {
                    JSONObject obj = new JSONObject(jsonResponse);
                    if (obj.optBoolean("success", false) && obj.has("data")) {
                        JSONObject dataObj = obj.getJSONObject("data");
                        String nextCutoff = dataObj.optString("next_critical_cutoff", "Upcoming");

                        List<PendingAction> actionList = new ArrayList<>();
                        if (dataObj.has("requirements")) {
                            JSONArray array = dataObj.getJSONArray("requirements");
                            for (int i = 0; i < array.length(); i++) {
                                JSONObject reqObj = array.getJSONObject(i);
                                String reqStatus = reqObj.optString("status", "UPLOAD");
                                if ("UPLOAD".equalsIgnoreCase(reqStatus) || "REJECTED".equalsIgnoreCase(reqStatus)) {
                                    String id = String.valueOf(reqObj.opt("id"));
                                    String title = reqObj.optString("title", "Requirement Upload");
                                    actionList.add(new PendingAction(id, "Upload " + title, "Due " + nextCutoff, "Scholarship Grants Office", "high"));
                                }
                            }
                        }

                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                if (rvPendingActions != null) {
                                    rvPendingActions.setAdapter(new PendingActionsAdapter(actionList, action -> {
                                        Toast.makeText(getContext(), "Opening " + action.getTitle(), Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(getActivity(), ScholarshipApplicationActivity.class));
                                    }));
                                }
                            });
                        }
                    }
                } catch (Exception ignored) {}
            }

            @Override
            public void onError(String errorMessage) {}
        });
    }

    private void setupRecyclerViews() {
        if (rvPendingActions != null) {
            rvPendingActions.setLayoutManager(new LinearLayoutManager(getContext()));
            rvPendingActions.setAdapter(new PendingActionsAdapter(new ArrayList<>(), action -> {
                Toast.makeText(getContext(), "Opening " + action.getTitle(), Toast.LENGTH_SHORT).show();
                startActivity(new Intent(getActivity(), ScholarshipApplicationActivity.class));
            }));
        }

        if (rvRecentRequests != null) {
            rvRecentRequests.setLayoutManager(new LinearLayoutManager(getContext()));
            rvRecentRequests.setAdapter(new RecentRequestsAdapter(new ArrayList<>(), req -> {
                startActivity(new Intent(getActivity(), RequestHistoryActivity.class));
            }));
        }
    }

    private void setupClickListeners(View view) {
        View btnNotifications = view.findViewById(R.id.btnNotifications);
        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v -> startActivity(new Intent(getActivity(), NotificationsActivity.class)));
        }

        View tvViewAllRecent = view.findViewById(R.id.tvViewAllRecent);
        if (tvViewAllRecent != null) {
            tvViewAllRecent.setOnClickListener(v -> startActivity(new Intent(getActivity(), RequestHistoryActivity.class)));
        }

        View cardScholarshipSummary = view.findViewById(R.id.cardScholarshipSummary);
        if (cardScholarshipSummary != null) {
            cardScholarshipSummary.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).selectTab(2);
                }
            });
        }

        View btnNavToScholarship = view.findViewById(R.id.btnNavToScholarship);
        if (btnNavToScholarship != null) {
            btnNavToScholarship.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).selectTab(2);
                }
            });
        }

        View cardRequestsSummary = view.findViewById(R.id.cardRequestsSummary);
        if (cardRequestsSummary != null) {
            cardRequestsSummary.setOnClickListener(v -> startActivity(new Intent(getActivity(), RequestHistoryActivity.class)));
        }

        View btnNavToRequests = view.findViewById(R.id.btnNavToRequests);
        if (btnNavToRequests != null) {
            btnNavToRequests.setOnClickListener(v -> startActivity(new Intent(getActivity(), RequestHistoryActivity.class)));
        }

        // Quick services
        View quickScholarship = view.findViewById(R.id.quickScholarship);
        if (quickScholarship != null) {
            quickScholarship.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).selectTab(2);
                }
            });
        }

        View quickDocuments = view.findViewById(R.id.quickDocuments);
        if (quickDocuments != null) {
            quickDocuments.setOnClickListener(v -> startActivity(new Intent(getActivity(), DocumentRequestsActivity.class)));
        }

        View quickConcerns = view.findViewById(R.id.quickConcerns);
        if (quickConcerns != null) {
            quickConcerns.setOnClickListener(v -> startActivity(new Intent(getActivity(), StudentConcernActivity.class)));
        }

        View quickLostId = view.findViewById(R.id.quickLostId);
        if (quickLostId != null) {
            quickLostId.setOnClickListener(v -> startActivity(new Intent(getActivity(), DocumentRequestsActivity.class)));
        }

        View quickHistory = view.findViewById(R.id.quickHistory);
        if (quickHistory != null) {
            quickHistory.setOnClickListener(v -> startActivity(new Intent(getActivity(), RequestHistoryActivity.class)));
        }

        View cardGuidance = view.findViewById(R.id.cardGuidance);
        if (cardGuidance != null) {
            cardGuidance.setOnClickListener(v -> startActivity(new Intent(getActivity(), StudentConcernActivity.class)));
        }

        // User Profile Avatar - Logout Action
        View btnProfile = view.findViewById(R.id.btnProfile);
        if (btnProfile != null) {
            btnProfile.setOnClickListener(v -> {
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Sign Out")
                    .setMessage("Are you sure you want to log out of Student Life?")
                    .setPositiveButton("Log Out", (dialog, which) -> {
                        if (getContext() == null || getActivity() == null) return;
                        
                        // Disable button to prevent repeated requests
                        btnProfile.setEnabled(false);

                        ApiClient.postJson(getContext(), "logout.php", "{}", new ApiClient.ApiCallback() {
                            @Override
                            public void onSuccess(String jsonResponse) {
                                executeLocalLogout("Logged out successfully");
                            }

                            @Override
                            public void onError(String errorMessage) {
                                // If 401, token is already dead; if network error, clear local to fail safe
                                executeLocalLogout("Local session cleared. Server disconnect unconfirmed.");
                            }
                        });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            });
        }
    }

    private void executeLocalLogout(String toastMessage) {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            if (getContext() != null) {
                ApiClient.clearAuthToken(getContext());
                Toast.makeText(getContext(), toastMessage, Toast.LENGTH_SHORT).show();
            }
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }
}
