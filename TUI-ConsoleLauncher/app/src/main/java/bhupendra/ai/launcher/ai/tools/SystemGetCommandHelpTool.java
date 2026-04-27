package bhupendra.ai.launcher.ai.tools;

import android.content.Context;
import org.json.JSONObject;
import java.util.List;
import java.util.Map;

import bhupendra.ai.launcher.commands.CommandHelpRegistry;
import bhupendra.ai.launcher.commands.CommandMetadata;
import bhupendra.ai.launcher.ai.ToolRiskClass;

public class SystemGetCommandHelpTool extends BaseAITool {

    public SystemGetCommandHelpTool(String name, String description, Map<String, String> parameters, ToolRiskClass riskClass) {
        super(name, description, parameters, riskClass);
    }

    @Override
    public String execute(Context context, JSONObject args) throws Exception {
        String commandName = args.optString("command_name", null);
        String searchQuery = args.optString("search_query", null);

        CommandHelpRegistry registry = CommandHelpRegistry.getInstance(context);

        if (commandName != null && !commandName.isEmpty()) {
            CommandMetadata meta = registry.getCommandHelp(commandName);
            if (meta != null) {
                return meta.toString();
            } else {
                return "[error: command '" + commandName + "' not found in registry]";
            }
        }

        if (searchQuery != null && !searchQuery.isEmpty()) {
            List<CommandMetadata> results = registry.searchCommands(searchQuery);
            if (results.isEmpty()) {
                return "[no matches found for '" + searchQuery + "']";
            }
            StringBuilder sb = new StringBuilder("Search results for '").append(searchQuery).append("':\n");
            for (CommandMetadata meta : results) {
                sb.append("- ").append(meta.name).append(": ").append(meta.description).append("\n");
            }
            sb.append("\nUse 'command_name' with one of these to get detailed syntax.");
            return sb.toString();
        }

        return "[error: either 'command_name' or 'search_query' must be provided]";
    }
}
