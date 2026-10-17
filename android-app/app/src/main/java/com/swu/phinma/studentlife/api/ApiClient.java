package com.swu.phinma.studentlife.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.json.JSONObject;

/**
 * Centralized PHP + PostgreSQL REST API Client for Android with Single-Flight Automatic Local LAN Discovery.
 *
 * Features:
 * - Single-flight discovery for concurrent API requests
 * - Server trust verification via /test_connection.php health JSON
 * - Non-blocking background thread pool execution
 * - Emulator & Physical Wi-Fi LAN support
 */
public class ApiClient {

    private static final String PREF_NAME = "swu_api_prefs";
    private static final String KEY_BASE_URL = "api_base_url";
    private static final String KEY_AUTH_TOKEN = "auth_token";

    public static final String EMULATOR_BASE_URL = "http://10.0.2.2:8080/android_api/";
    public static final String PHYSICAL_LAN_BASE_URL = "http://192.168.18.7:8080/android_api/";
    public static final String DEFAULT_BASE_URL = isEmulator() ? EMULATOR_BASE_URL : PHYSICAL_LAN_BASE_URL; // Legacy compatibility only; discovery never trusts this fallback.

    private static final ExecutorService executor = Executors.newFixedThreadPool(4);

    // Single-Flight Discovery Controls
    private static final Object DISCOVERY_LOCK = new Object();
    private static final AtomicReference<String> verifiedBaseUrl = new AtomicReference<>(null);

    public interface ApiCallback {
        void onSuccess(String jsonResponse);
        void onError(String errorMessage);
    }

    /**
     * Auto-detects whether the app is running in the Android Studio Emulator or on a Physical Device.
     */
    public static boolean isEmulator() {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT));
    }

    /**
     * Verifies server trust identity without sending authentication credentials.
     */
    public static boolean verifyServerTrust(String rawCandidateUrl) {
        if (rawCandidateUrl == null || rawCandidateUrl.isEmpty()) return false;
        HttpURLConnection conn = null;
        try {
            String candidateBaseUrl = rawCandidateUrl.endsWith("/") ? rawCandidateUrl : rawCandidateUrl + "/";
            URL url = new URL(candidateBaseUrl + "test_connection.php");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(800);
            conn.setReadTimeout(1000);

            int code = conn.getResponseCode();
            if (code == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line.trim());
                }
                JSONObject obj = new JSONObject(sb.toString());
                String status = obj.optString("status", "");
                String service = obj.optString("service", "");
                String database = obj.optString("database", "");

                return "ok".equalsIgnoreCase(status)
                        && "Student Life API".equalsIgnoreCase(service)
                        && "connected".equalsIgnoreCase(database);
            }
        } catch (Exception ignored) {
        } finally {
            if (conn != null) conn.disconnect();
        }
        return false;
    }

    private static final String KEY_LAST_NETWORK = "last_network_fingerprint";
    private static final int[] DISCOVERY_PORTS = {8080, 80};

    private static final class LanInfo {
        final String fingerprint;
        final String[] hosts;

        LanInfo(String fingerprint, String[] hosts) {
            this.fingerprint = fingerprint;
            this.hosts = hosts;
        }
    }

    /**
     * Uses the active Wi-Fi network only. Avoids accidentally scanning a VPN,
     * cellular interface, or an unrelated private network interface.
     * Caps discovery to 1024 candidate hosts to avoid scanning huge networks.
     */
    private static LanInfo getActiveLan(Context context) {
        if (context == null) return null;
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getApplicationContext()
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return null;
            Network network = cm.getActiveNetwork();
            if (network == null) return null;
            NetworkCapabilities caps = cm.getNetworkCapabilities(network);
            if (caps == null || !caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return null;
            LinkProperties props = cm.getLinkProperties(network);
            if (props == null) return null;
            for (LinkAddress link : props.getLinkAddresses()) {
                InetAddress address = link.getAddress();
                if (!(address instanceof java.net.Inet4Address)) continue;
                byte[] bytes = address.getAddress();
                int ip = ((bytes[0] & 255) << 24) | ((bytes[1] & 255) << 16)
                        | ((bytes[2] & 255) << 8) | (bytes[3] & 255);
                int prefix = link.getPrefixLength();
                if (prefix < 1 || prefix > 32) continue;
                int mask = prefix == 32 ? -1 : (int) (0xffffffffL << (32 - prefix));
                int networkAddress = ip & mask;
                long count = 1L << (32 - prefix);
                // Limit discovery to local, reasonably sized IPv4 networks.
                if (count > 1024 || count < 4) continue;
                ArrayList<String> hosts = new ArrayList<>();
                long first = (networkAddress & 0xffffffffL) + 1;
                long last = (networkAddress & 0xffffffffL) + count - 2;
                for (long candidate = first; candidate <= last; candidate++) {
                    if ((int) candidate == ip) continue;
                    int value = (int) candidate;
                    hosts.add(((value >>> 24) & 255) + "." + ((value >>> 16) & 255)
                            + "." + ((value >>> 8) & 255) + "." + (value & 255));
                }
                String fingerprint = network.getNetworkHandle() + ":" + (networkAddress & 0xffffffffL)
                        + "/" + prefix + ":" + address.getHostAddress();
                return new LanInfo(fingerprint, hosts.toArray(new String[0]));
            }
        } catch (Exception e) {
            android.util.Log.w("StudentLifeApi", "Unable to inspect active Wi-Fi network", e);
        }
        return null;
    }

    /** Serializes discovery and cache invalidation across concurrent API calls. */
    private static String resolveBaseUrlSingleFlight(Context context) {
        if (isEmulator()) return EMULATOR_BASE_URL;
        synchronized (DISCOVERY_LOCK) {
            LanInfo lan = getActiveLan(context);
            if (lan == null) {
                verifiedBaseUrl.set(null);
                return null;
            }
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String previousNetwork = prefs.getString(KEY_LAST_NETWORK, null);
            if (!lan.fingerprint.equals(previousNetwork)) {
                verifiedBaseUrl.set(null);
                prefs.edit().remove(KEY_BASE_URL)
                        .putString(KEY_LAST_NETWORK, lan.fingerprint).commit();
            }

            String cached = verifiedBaseUrl.get();
            if (cached != null && verifyServerTrust(cached)) return cached;
            verifiedBaseUrl.set(null);
            String saved = prefs.getString(KEY_BASE_URL, null);
            if (saved != null && verifyServerTrust(saved)) {
                verifiedBaseUrl.set(saved);
                return saved;
            }
            prefs.edit().remove(KEY_BASE_URL).apply();

            ExecutorService pool = Executors.newFixedThreadPool(32);
            ArrayList<Future<String>> tasks = new ArrayList<>();
            CompletionService<String> completion = new ExecutorCompletionService<>(pool);
            try {
                // Prefer 8080, but also support XAMPP's usual HTTP port 80.
                for (int port : DISCOVERY_PORTS) {
                    for (String host : lan.hosts) {
                        final String candidate = "http://" + host + ":" + port + "/android_api/";
                        tasks.add(completion.submit(() -> verifyServerTrust(candidate) ? candidate : null));
                    }
                }
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(18);
                for (int i = 0; i < tasks.size(); i++) {
                    long remaining = deadline - System.nanoTime();
                    if (remaining <= 0) break;
                    Future<String> finished = completion.poll(remaining, TimeUnit.NANOSECONDS);
                    if (finished == null) break;
                    String found = finished.get();
                    if (found != null) {
                        verifiedBaseUrl.set(found);
                        prefs.edit().putString(KEY_BASE_URL, found).apply();
                        android.util.Log.i("StudentLifeApi", "Discovered local API at " + found);
                        return found;
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                android.util.Log.w("StudentLifeApi", "API discovery failed", e);
            } finally {
                for (Future<String> task : tasks) task.cancel(true);
                pool.shutdownNow();
            }
            android.util.Log.w("StudentLifeApi", "No reachable Student Life API on current Wi-Fi");
            return null;
        }
    }

    public static String getBaseUrl(Context context) {
        return resolveBaseUrlSingleFlight(context);
    }

    /** Sets a candidate address; all requests still validate it before use. */
    public static void setBaseUrl(Context context, String newUrl) {
        if (context == null || newUrl == null) return;
        String formattedUrl = newUrl.endsWith("/") ? newUrl : newUrl + "/";
        synchronized (DISCOVERY_LOCK) {
            verifiedBaseUrl.set(null);
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                    .edit().putString(KEY_BASE_URL, formattedUrl).apply();
        }
    }

    public static String getAuthToken(Context context) {
        if (context == null) return null;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_AUTH_TOKEN, null);
    }

    public static void setAuthToken(Context context, String token) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_AUTH_TOKEN, token).apply();
    }

    private static final String KEY_STUDENT_NAME = "student_name";
    private static final String KEY_STUDENT_ID_NUM = "student_id_num";
    private static final String KEY_STUDENT_COURSE = "student_course";
    private static final String KEY_STUDENT_YEAR = "student_year";
    private static final String KEY_STUDENT_EMAIL = "student_email";
    private static final String KEY_STUDENT_STATUS = "student_status";
    private static final String KEY_STUDENT_PROGRESS = "student_progress";
    private static final String KEY_VERIFIED_DOCS = "verified_docs";
    private static final String KEY_TOTAL_DOCS = "total_docs";

    public static void saveStudentProfile(Context context, JSONObject studentObj) {
        if (context == null || studentObj == null) return;
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            String firstName = studentObj.optString("first_name", "");
            String lastName = studentObj.optString("last_name", "");
            String fullName = (firstName + " " + lastName).trim();
            if (fullName.isEmpty()) fullName = studentObj.optString("name", "");

            if (fullName.matches("^[0-9\\-]+$") || fullName.isEmpty()) {
                fullName = "Maria Santos";
            }

            editor.putString(KEY_STUDENT_NAME, fullName);
            editor.putString(KEY_STUDENT_ID_NUM, studentObj.optString("student_id", ""));
            editor.putString(KEY_STUDENT_COURSE, studentObj.optString("course", "BS Computer Science"));
            editor.putString(KEY_STUDENT_YEAR, studentObj.optString("year_level", "3rd Year"));
            editor.putString(KEY_STUDENT_EMAIL, studentObj.optString("email", ""));
            editor.putString(KEY_STUDENT_STATUS, studentObj.optString("academic_status", "Active"));
            editor.putInt(KEY_STUDENT_PROGRESS, studentObj.optInt("requirements_progress", 67));
            editor.putInt(KEY_VERIFIED_DOCS, studentObj.optInt("verified_docs_count", 4));
            editor.putInt(KEY_TOTAL_DOCS, studentObj.optInt("total_docs_count", 6));
            editor.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String[] getSavedStudentProfile(Context context) {
        if (context == null) return null;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String name = prefs.getString(KEY_STUDENT_NAME, null);
        if (name == null || name.isEmpty()) return null;

        String idNum = prefs.getString(KEY_STUDENT_ID_NUM, "");
        String course = prefs.getString(KEY_STUDENT_COURSE, "BS Computer Science");
        String year = prefs.getString(KEY_STUDENT_YEAR, "3rd Year");
        String email = prefs.getString(KEY_STUDENT_EMAIL, "");
        String status = prefs.getString(KEY_STUDENT_STATUS, "Active");
        String progress = String.valueOf(prefs.getInt(KEY_STUDENT_PROGRESS, 67));
        String verified = String.valueOf(prefs.getInt(KEY_VERIFIED_DOCS, 4));
        String total = String.valueOf(prefs.getInt(KEY_TOTAL_DOCS, 6));

        return new String[] { name, idNum, course, year, email, status, progress, verified, total };
    }

    public static void clearAuthToken(Context context) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .remove(KEY_AUTH_TOKEN)
                .remove(KEY_STUDENT_NAME)
                .remove(KEY_STUDENT_ID_NUM)
                .remove(KEY_STUDENT_COURSE)
                .remove(KEY_STUDENT_YEAR)
                .remove(KEY_STUDENT_EMAIL)
                .remove(KEY_STUDENT_STATUS)
                .remove(KEY_STUDENT_PROGRESS)
                .remove(KEY_VERIFIED_DOCS)
                .remove(KEY_TOTAL_DOCS)
                .apply();
    }

    /**
     * Executes an HTTP POST request with JSON body on background thread after resolving base URL.
     */
    public static void postJson(final Context context, final String endpoint, final String jsonBody, final ApiCallback callback) {
        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                String baseUrl = resolveBaseUrlSingleFlight(context);
                if (baseUrl == null) {
                    if (callback != null) {
                        callback.onError("Unable to locate Student Life API on your local network. Please ensure your device is connected to the same Wi-Fi network as the server.");
                    }
                    return;
                }
                String fullUrl = baseUrl + endpoint;
                URL url = new URL(fullUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Accept", "application/json");

                String token = getAuthToken(context);
                if (token != null && !token.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                }

                conn.setConnectTimeout(8000);
                conn.setReadTimeout(10000);
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int code = conn.getResponseCode();
                BufferedReader br = new BufferedReader(new InputStreamReader(
                        code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(),
                        StandardCharsets.UTF_8
                ));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line.trim());
                }

                if (code >= 200 && code < 300) {
                    if (callback != null) callback.onSuccess(response.toString());
                } else {
                    if (callback != null) callback.onError("Server returned status " + code + ": " + response);
                }
            } catch (Exception e) {
                if (callback != null) callback.onError("Network connection failure: " + e.getMessage());
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    /**
     * Executes an HTTP GET request on background thread after resolving base URL.
     */
    public static void get(final Context context, final String endpoint, final ApiCallback callback) {
        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                String baseUrl = resolveBaseUrlSingleFlight(context);
                if (baseUrl == null) {
                    if (callback != null) {
                        callback.onError("Unable to locate Student Life API on your local network. Please ensure your device is connected to the same Wi-Fi network as the server.");
                    }
                    return;
                }
                String fullUrl = baseUrl + endpoint;
                URL url = new URL(fullUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");

                String token = getAuthToken(context);
                if (token != null && !token.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                }

                conn.setConnectTimeout(6000);
                conn.setReadTimeout(8000);

                int code = conn.getResponseCode();
                BufferedReader br = new BufferedReader(new InputStreamReader(
                        code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(),
                        StandardCharsets.UTF_8
                ));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line.trim());
                }

                if (code >= 200 && code < 300) {
                    if (callback != null) callback.onSuccess(response.toString());
                } else {
                    if (callback != null) callback.onError("Server status " + code + ": " + response);
                }
            } catch (Exception e) {
                if (callback != null) callback.onError("Network failure: " + e.getMessage());
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    /**
     * Executes a multipart/form-data HTTP POST request for uploading files to PHP backend.
     */
    public static void postMultipart(final Context context, final String endpoint, final String documentType, final android.net.Uri fileUri, final ApiCallback callback) {
        postMultipart(context, endpoint, documentType, fileUri, null, null, null, callback);
    }

    public static void postMultipart(final Context context, final String endpoint, final String documentType, final android.net.Uri fileUri, final Integer requestId, final ApiCallback callback) {
        postMultipart(context, endpoint, documentType, fileUri, requestId, null, null, callback);
    }

    public static void postMultipart(final Context context, final String endpoint, final String documentType, final android.net.Uri fileUri, final Integer requestId, final Integer replaceDocId, final ApiCallback callback) {
        postMultipart(context, endpoint, documentType, fileUri, requestId, replaceDocId, null, callback);
    }

    public static void postMultipart(final Context context, final String endpoint, final String documentType, final android.net.Uri fileUri, final Integer requestId, final Integer replaceDocId, final Integer applicationId, final ApiCallback callback) {
        postMultipart(context, endpoint, documentType, fileUri, requestId, replaceDocId, applicationId, null, callback);
    }

    public static void postMultipart(final Context context, final String endpoint, final String documentType, final android.net.Uri fileUri, final Integer requestId, final Integer replaceDocId, final Integer applicationId, final Integer requirementId, final ApiCallback callback) {
        executor.execute(() -> {
            HttpURLConnection conn = null;
            String boundary = "===SWU_STUDENT_LIFE_" + System.currentTimeMillis() + "===";
            String lineEnd = "\r\n";
            String twoHyphens = "--";

            try {
                String baseUrl = resolveBaseUrlSingleFlight(context);
                if (baseUrl == null) {
                    if (callback != null) {
                        callback.onError("Unable to locate Student Life API on your local network. Please ensure your device is connected to the same Wi-Fi network as the server.");
                    }
                    return;
                }
                String fullUrl = baseUrl + endpoint;
                URL url = new URL(fullUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setUseCaches(false);
                conn.setConnectTimeout(12000);
                conn.setReadTimeout(20000);

                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                conn.setRequestProperty("Accept", "application/json");

                String token = getAuthToken(context);
                if (token != null && !token.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                }

                try (OutputStream os = conn.getOutputStream()) {
                    // 1. Text part: document_type
                    if (documentType != null && !documentType.isEmpty()) {
                        StringBuilder textPart = new StringBuilder();
                        textPart.append(twoHyphens).append(boundary).append(lineEnd);
                        textPart.append("Content-Disposition: form-data; name=\"document_type\"").append(lineEnd);
                        textPart.append("Content-Type: text/plain; charset=UTF-8").append(lineEnd);
                        textPart.append(lineEnd);
                        textPart.append(documentType).append(lineEnd);
                        os.write(textPart.toString().getBytes(StandardCharsets.UTF_8));
                    }

                    // 2. Text part: request_id
                    if (requestId != null && requestId > 0) {
                        StringBuilder reqIdPart = new StringBuilder();
                        reqIdPart.append(twoHyphens).append(boundary).append(lineEnd);
                        reqIdPart.append("Content-Disposition: form-data; name=\"request_id\"").append(lineEnd);
                        reqIdPart.append("Content-Type: text/plain; charset=UTF-8").append(lineEnd);
                        reqIdPart.append(lineEnd);
                        reqIdPart.append(requestId).append(lineEnd);
                        os.write(reqIdPart.toString().getBytes(StandardCharsets.UTF_8));
                    }

                    // 3. Text part: replace_document_id
                    if (replaceDocId != null && replaceDocId > 0) {
                        StringBuilder repPart = new StringBuilder();
                        repPart.append(twoHyphens).append(boundary).append(lineEnd);
                        repPart.append("Content-Disposition: form-data; name=\"replace_document_id\"").append(lineEnd);
                        repPart.append("Content-Type: text/plain; charset=UTF-8").append(lineEnd);
                        repPart.append(lineEnd);
                        repPart.append(replaceDocId).append(lineEnd);
                        os.write(repPart.toString().getBytes(StandardCharsets.UTF_8));
                    }

                    // 4. Text part: application_id
                    if (applicationId != null && applicationId > 0) {
                        StringBuilder appPart = new StringBuilder();
                        appPart.append(twoHyphens).append(boundary).append(lineEnd);
                        appPart.append("Content-Disposition: form-data; name=\"application_id\"").append(lineEnd);
                        appPart.append("Content-Type: text/plain; charset=UTF-8").append(lineEnd);
                        appPart.append(lineEnd);
                        appPart.append(applicationId).append(lineEnd);
                        os.write(appPart.toString().getBytes(StandardCharsets.UTF_8));
                    }

                    // 5. Text part: requirement_id
                    if (requirementId != null && requirementId > 0) {
                        StringBuilder reqPart = new StringBuilder();
                        reqPart.append(twoHyphens).append(boundary).append(lineEnd);
                        reqPart.append("Content-Disposition: form-data; name=\"requirement_id\"").append(lineEnd);
                        reqPart.append("Content-Type: text/plain; charset=UTF-8").append(lineEnd);
                        reqPart.append(lineEnd);
                        reqPart.append(requirementId).append(lineEnd);
                        os.write(reqPart.toString().getBytes(StandardCharsets.UTF_8));
                    }

                    // 6. File part: document
                    String fileName = "document.pdf";
                    if (fileUri != null && context != null) {
                        try (android.database.Cursor cursor = context.getContentResolver().query(fileUri, null, null, null, null)) {
                            if (cursor != null && cursor.moveToFirst()) {
                                int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                                if (nameIndex != -1) {
                                    fileName = cursor.getString(nameIndex);
                                }
                            }
                        } catch (Exception ignored) {}
                    }

                    StringBuilder fileHeader = new StringBuilder();
                    fileHeader.append(twoHyphens).append(boundary).append(lineEnd);
                    fileHeader.append("Content-Disposition: form-data; name=\"document\"; filename=\"").append(fileName).append("\"").append(lineEnd);
                    fileHeader.append("Content-Type: application/octet-stream").append(lineEnd);
                    fileHeader.append(lineEnd);
                    os.write(fileHeader.toString().getBytes(StandardCharsets.UTF_8));

                    // Stream file bytes from Uri
                    if (fileUri != null && context != null) {
                        try (java.io.InputStream inputStream = context.getContentResolver().openInputStream(fileUri)) {
                            byte[] buffer = new byte[8192];
                            int bytesRead;
                            while ((bytesRead = inputStream.read(buffer)) != -1) {
                                os.write(buffer, 0, bytesRead);
                            }
                        }
                    }
                    os.write(lineEnd.getBytes(StandardCharsets.UTF_8));

                    // Final boundary
                    String endBoundary = twoHyphens + boundary + twoHyphens + lineEnd;
                    os.write(endBoundary.getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }

                int code = conn.getResponseCode();
                BufferedReader br = new BufferedReader(new InputStreamReader(
                        code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(),
                        StandardCharsets.UTF_8
                ));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line.trim());
                }

                if (code >= 200 && code < 300) {
                    if (callback != null) callback.onSuccess(response.toString());
                } else {
                    if (callback != null) callback.onError("Server returned status " + code + ": " + response);
                }
            } catch (Exception e) {
                if (callback != null) callback.onError("Upload failed: " + e.getMessage());
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }
}
