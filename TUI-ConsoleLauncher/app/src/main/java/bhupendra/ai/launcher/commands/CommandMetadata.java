package bhupendra.ai.launcher.commands;

import java.util.List;
import java.util.Map;

/**
 * Data class representing command metadata for the Searchable Command Skill Registry.
 */
public class CommandMetadata {
    public final String name;
    public final String description;
    public final Map<String, String> flags;
    public final String arguments;
    public final List<String> examples;
    public final List<String> keywords;
    public final String subToolName;

    public CommandMetadata(String name, String description, Map<String, String> flags, String arguments, List<String> examples) {
        this(name, description, flags, arguments, examples, null, null);
    }

    public CommandMetadata(String name, String description, Map<String, String> flags, String arguments, List<String> examples, List<String> keywords, String subToolName) {
        this.name = name;
        this.description = description;
        this.flags = flags != null ? flags : new java.util.HashMap<>();
        this.arguments = arguments != null ? arguments : "";
        this.examples = examples != null ? examples : new java.util.ArrayList<>();
        this.keywords = keywords != null ? keywords : new java.util.ArrayList<>();
        this.subToolName = subToolName;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("COMMAND: ").append(name).append("\n");
        sb.append("DESCRIPTION: ").append(description).append("\n");
        if (!keywords.isEmpty()) {
            sb.append("KEYWORDS: ").append(String.join(", ", keywords)).append("\n");
        }
        if (!flags.isEmpty()) {
            sb.append("FLAGS:\n");
            for (Map.Entry<String, String> entry : flags.entrySet()) {
                sb.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        }
        if (!arguments.isEmpty()) {
            sb.append("ARGUMENTS: ").append(arguments).append("\n");
        }
        if (!examples.isEmpty()) {
            sb.append("EXAMPLES:\n");
            for (String example : examples) {
                sb.append("  - ").append(example).append("\n");
            }
        }
        if (subToolName != null && !subToolName.isEmpty()) {
            sb.append("NOTE: For dynamic details (e.g., current settings or values), consult tool: ").append(subToolName).append("\n");
        }
        return sb.toString();
    }
}
