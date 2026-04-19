package bhupendra.ai.launcher.ai;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class WebFetcher {
    public static String fetch(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                in.close();
                
                // Basic HTML to Text (strip tags)
                String rawHtml = content.toString();
                String text = rawHtml.replaceAll("<script[\\s\\S]*?</script>", "")
                                    .replaceAll("<style[\\s\\S]*?</style>", "")
                                    .replaceAll("<[^>]*>", " ")
                                    .replaceAll("\\s+", " ")
                                    .trim();
                
                return text.length() > 2000 ? text.substring(0, 2000) + "..." : text;
            } else {
                return "[error: HTTP " + responseCode + "]";
            }
        } catch (Exception e) {
            return "[error: " + e.getMessage() + "]";
        }
    }
}
