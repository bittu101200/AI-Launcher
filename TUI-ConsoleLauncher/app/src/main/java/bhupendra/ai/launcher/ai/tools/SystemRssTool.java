package bhupendra.ai.launcher.ai.tools;

import android.content.Context;
import org.json.JSONObject;
import java.util.LinkedHashMap;
import java.util.Map;

import bhupendra.ai.launcher.ai.ToolRiskClass;
import bhupendra.ai.launcher.managers.RssManager;

public class SystemRssTool extends BaseAITool {

    public SystemRssTool() {
        super(
                "system.rss",
                "Manage and read RSS feeds. Allows listing feeds, subscribing to new feeds, unsubscribing, pulling the latest XML feed content, and reading feed items.",
                createParams(),
                ToolRiskClass.STATE_CHANGING
        );
    }

    private static Map<String, String> createParams() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("action", "Required: one of 'list', 'add', 'remove', 'read', 'fetch_latest'.");
        params.put("url", "Required for 'add': The URL of the RSS feed.");
        params.put("id", "Required for 'remove', 'read', 'fetch_latest'. Optional for 'add': The numeric feed ID.");
        params.put("time_seconds", "Optional for 'add': Check interval in seconds (default is 1800).");
        return params;
    }

    @Override
    public String execute(Context context, JSONObject args) throws Exception {
        RssManager rssManager = getEntryPoint(context).rssManager();
        if (rssManager == null) {
            return "[error: rssManager not available]";
        }

        String action = args.optString("action", "").toLowerCase().trim();
        if (action.isEmpty()) {
            return "[error: action is required]";
        }

        switch (action) {
            case "list":
                return rssManager.list();

            case "add": {
                String url = args.optString("url", "").trim();
                if (url.isEmpty()) {
                    return "[error: url is required for add]";
                }
                int id = args.optInt("id", -1);
                if (id == -1) {
                    id = 1;
                    while (rssManager.findId(id) != null) {
                        id++;
                    }
                } else {
                    if (rssManager.findId(id) != null) {
                        return "[error: RSS feed with ID " + id + " already exists]";
                    }
                }
                long timeSeconds = args.optLong("time_seconds", 1800);
                String addResult = rssManager.add(id, timeSeconds, url);
                if (addResult == null) {
                    return "[Success: added RSS feed with ID " + id + " and url " + url + "]";
                } else {
                    return "[error: " + addResult + "]";
                }
            }

            case "remove": {
                if (!args.has("id")) {
                    return "[error: id is required for remove]";
                }
                int id = args.getInt("id");
                String removeResult = rssManager.rm(id);
                if (removeResult == null) {
                    return "[Success: removed RSS feed with ID " + id + "]";
                } else {
                    return "[error: " + removeResult + "]";
                }
            }

            case "read": {
                if (!args.has("id")) {
                    return "[error: id is required for read]";
                }
                int id = args.getInt("id");
                return rssManager.getFeedItemsAsString(id);
            }

            case "fetch_latest": {
                if (!args.has("id")) {
                    return "[error: id is required for fetch_latest]";
                }
                int id = args.getInt("id");
                return rssManager.fetchFeedSynchronously(id);
            }

            default:
                return "[error: unknown action " + action + "]";
        }
    }
}
