package bhupendra.ai.launcher.ai;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.jsoup.select.Elements;

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
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine).append("\n");
                }
                in.close();
                
                String html = content.toString();
                Document doc = Jsoup.parse(html, urlString);
                
                // Noise Management: Remove non-content elements
                doc.select("script, style, nav, footer, header, aside, .sidebar, .menu, .ads").remove();
                
                // Focus on article/main if available
                Element mainContent = doc.select("article, main, .content, #content, .post, #post").first();
                if (mainContent == null) mainContent = doc.body();

                return htmlToMarkdown(mainContent);
            } else {
                return "[error: HTTP " + responseCode + "]";
            }
        } catch (Exception e) {
            return "[error: " + e.getMessage() + "]";
        }
    }

    private static String htmlToMarkdown(Element element) {
        StringBuilder md = new StringBuilder();
        
        for (Element child : element.children()) {
            String tag = child.tagName().toLowerCase();
            
            if (tag.matches("h[1-6]")) {
                int level = Integer.parseInt(tag.substring(1));
                for (int i = 0; i < level; i++) md.append("#");
                md.append(" ").append(child.text()).append("\n\n");
            } else if (tag.equals("p")) {
                md.append(child.text()).append("\n\n");
            } else if (tag.equals("li")) {
                md.append("- ").append(child.text()).append("\n");
            } else if (tag.equals("a")) {
                md.append("[").append(child.text()).append("](").append(child.attr("abs:href")).append(")");
            } else if (child.children().size() > 0) {
                md.append(htmlToMarkdown(child));
            } else {
                String text = child.text();
                if (!text.isEmpty()) md.append(text).append("\n\n");
            }
        }

        // Fallback if no children were processed or result is too small
        if (md.length() < 50) {
            String text = Jsoup.clean(element.html(), Safelist.none());
            return text.length() > 3000 ? text.substring(0, 3000) + "..." : text;
        }

        String result = md.toString().replaceAll("\n{3,}", "\n\n").trim();
        return result.length() > 3000 ? result.substring(0, 3000) + "..." : result;
    }
}
