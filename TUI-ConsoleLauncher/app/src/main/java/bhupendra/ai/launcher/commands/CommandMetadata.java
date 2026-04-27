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

    public CommandMetadata(String name, String description, Map<String, String> flags, String arguments, List<String> examples) {
        this.name = name;
        this.description = description;
        this.flags = flags;
        this.arguments = arguments;
        this.examples = examples;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Command: ").append(name).append("\n");
        sb.append("Description: ").append(description).append("\n");
        
        if (arguments != null && !arguments.isEmpty()) {
            sb.append("Arguments: ").append(arguments).append("\n");
        }
        
        if (flags != null && !flags.isEmpty()) {
            sb.append("Flags:\n");
            for (Map.Entry<String, String> entry : flags.entrySet()) {
                sb.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        }
        
        if (examples != null && !examples.isEmpty()) {
            sb.append("Examples:\n");
            for (String example : examples) {
                sb.append("  - ").append(example).append("\n");
            }
        }
        
        return sb.toString();
    }
}
