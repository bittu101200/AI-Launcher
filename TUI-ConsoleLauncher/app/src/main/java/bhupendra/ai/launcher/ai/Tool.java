package bhupendra.ai.launcher.ai;

import java.util.LinkedHashMap;
import java.util.Map;

public class Tool {
    public final String name;
    public final String description;
    public final Map<String, String> parameters;
    public final ToolRiskClass riskClass;

    public Tool(String name, String description,
                Map<String, String> parameters, ToolRiskClass riskClass) {
        this.name = name;
        this.description = description;
        this.parameters = parameters != null ? parameters : new LinkedHashMap<>();
        this.riskClass = riskClass;
    }
}