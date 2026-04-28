package bhupendra.ai.launcher.managers;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import bhupendra.ai.launcher.ai.providers.SharedHttpClient;

public final class InternetSpeedTestManager {

    private static final String[] LATENCY_URLS = new String[] {
            "https://www.google.com/generate_204",
            "https://clients3.google.com/generate_204",
            "https://www.cloudflare.com/cdn-cgi/trace"
    };

    private static final String[] DOWNLOAD_URLS = new String[] {
            "https://speed.cloudflare.com/__down?bytes=2097152",
            "https://speed.hetzner.de/1MB.bin",
            "https://proof.ovh.net/files/1Mb.dat"
    };

    private static final String[] UPLOAD_URLS = new String[] {
            "https://speed.cloudflare.com/__up",
            "https://httpbin.org/post",
            "https://postman-echo.com/post"
    };

    private static final int UPLOAD_BYTES = 512 * 1024;
    private static final MediaType OCTET_STREAM = MediaType.parse("application/octet-stream");

    private InternetSpeedTestManager() {
    }

    public static Result run() {
        OkHttpClient client = SharedHttpClient.get();
        Probe latency = measureLatency(client);
        Probe download = measureDownload(client);
        Probe upload = measureUpload(client);
        return new Result(latency, download, upload);
    }

    public static String format(Result result) {
        if (result == null) {
            return "Speed test failed: no result";
        }

        StringBuilder builder = new StringBuilder();
        builder.append("Internet speed test").append("\n\n");
        builder.append("Latency: ").append(result.formatLatency()).append("\n");
        builder.append("Download: ").append(result.formatDownload()).append("\n");
        builder.append("Upload: ").append(result.formatUpload()).append("\n");
        builder.append("\n");
        builder.append("Endpoint: ").append(result.endpointSummary());
        return builder.toString();
    }

    private static Probe measureLatency(OkHttpClient client) {
        for (String url : LATENCY_URLS) {
            Probe probe = executeLatencyProbe(client, url);
            if (probe.success) {
                return probe;
            }
        }
        return Probe.failure("latency", "All latency endpoints failed");
    }

    private static Probe executeLatencyProbe(OkHttpClient client, String url) {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        long start = System.nanoTime();
        try (Response response = client.newCall(request).execute()) {
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            if (!response.isSuccessful() && response.code() != 204) {
                return Probe.failure("latency", url, "HTTP " + response.code());
            }
            return Probe.success("latency", url, elapsedMs, 0L);
        } catch (IOException e) {
            return Probe.failure("latency", url, e.getMessage());
        }
    }

    private static Probe measureDownload(OkHttpClient client) {
        for (String url : DOWNLOAD_URLS) {
            Probe probe = executeDownloadProbe(client, url);
            if (probe.success) {
                return probe;
            }
        }
        return Probe.failure("download", "All download endpoints failed");
    }

    private static Probe executeDownloadProbe(OkHttpClient client, String url) {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        long start = System.nanoTime();
        long bytesRead = 0L;
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return Probe.failure("download", url, "HTTP " + response.code());
            }
            ResponseBody body = response.body();
            if (body == null) {
                return Probe.failure("download", url, "empty response body");
            }

            byte[] buffer = new byte[16 * 1024];
            try (InputStream in = body.byteStream()) {
                int read;
                while ((read = in.read(buffer)) != -1) {
                    bytesRead += read;
                }
            }

            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            return Probe.success("download", url, elapsedMs, bytesRead);
        } catch (IOException e) {
            return Probe.failure("download", url, e.getMessage());
        }
    }

    private static Probe measureUpload(OkHttpClient client) {
        for (String url : UPLOAD_URLS) {
            Probe probe = executeUploadProbe(client, url);
            if (probe.success) {
                return probe;
            }
        }
        return Probe.failure("upload", "All upload endpoints failed");
    }

    private static Probe executeUploadProbe(OkHttpClient client, String url) {
        byte[] payload = new byte[UPLOAD_BYTES];
        Arrays.fill(payload, (byte) 0);

        RequestBody body = RequestBody.create(payload, OCTET_STREAM);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        long start = System.nanoTime();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return Probe.failure("upload", url, "HTTP " + response.code());
            }
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            return Probe.success("upload", url, elapsedMs, payload.length);
        } catch (IOException e) {
            return Probe.failure("upload", url, e.getMessage());
        }
    }

    public static final class Result {
        public final Probe latency;
        public final Probe download;
        public final Probe upload;

        Result(Probe latency, Probe download, Probe upload) {
            this.latency = latency;
            this.download = download;
            this.upload = upload;
        }

        public JSONObject toJson() throws Exception {
            JSONObject json = new JSONObject();
            json.put("latency", latency.toJson());
            json.put("download", download.toJson());
            json.put("upload", upload.toJson());
            return json;
        }

        public String formatLatency() {
            return latency.success ? latency.elapsedMs + " ms" : latency.error;
        }

        public String formatDownload() {
            if (!download.success) return download.error;
            return formatThroughput(download.bytes, download.elapsedMs);
        }

        public String formatUpload() {
            if (!upload.success) return upload.error;
            return formatThroughput(upload.bytes, upload.elapsedMs);
        }

        public String endpointSummary() {
            return "latency=" + latency.endpoint + ", download=" + download.endpoint + ", upload=" + upload.endpoint;
        }
    }

    public static final class Probe {
        public final String label;
        public final String endpoint;
        public final long elapsedMs;
        public final long bytes;
        public final boolean success;
        public final String error;

        private Probe(String label, String endpoint, long elapsedMs, long bytes, boolean success, String error) {
            this.label = label;
            this.endpoint = endpoint;
            this.elapsedMs = elapsedMs;
            this.bytes = bytes;
            this.success = success;
            this.error = error;
        }

        static Probe success(String label, String endpoint, long elapsedMs, long bytes) {
            return new Probe(label, endpoint, elapsedMs, bytes, true, null);
        }

        static Probe failure(String label, String endpoint, String error) {
            return new Probe(label, endpoint, 0L, 0L, false, error == null ? "unknown error" : error);
        }

        static Probe failure(String label, String error) {
            return failure(label, "n/a", error);
        }

        JSONObject toJson() throws Exception {
            JSONObject json = new JSONObject();
            json.put("label", label);
            json.put("endpoint", endpoint);
            json.put("elapsed_ms", elapsedMs);
            json.put("bytes", bytes);
            json.put("success", success);
            if (error != null) {
                json.put("error", error);
            }
            return json;
        }
    }

    public static String formatThroughput(long bytes, long elapsedMs) {
        if (elapsedMs <= 0) {
            return "0 Mbps";
        }
        double seconds = elapsedMs / 1000.0;
        double mbps = (bytes * 8.0) / seconds / 1_000_000.0;
        double mBps = bytes / seconds / 1_000_000.0;
        return String.format(Locale.US, "%.2f Mbps (%.2f MB/s)", mbps, mBps);
    }
}
