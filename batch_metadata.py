import os
import re

# Directory containing the command Java files
cmd_dir = "TUI-ConsoleLauncher/app/src/main/java/bhupendra/ai/launcher/commands/main/raw/"
# Path to strings.xml
strings_path = "TUI-ConsoleLauncher/app/src/main/res/values/strings.xml"

# Files already processed (CORE commands)
processed_files = {"apps.java", "config.java", "alias.java", "wifi.java", "bluetooth.java", "notes.java"}

# Load strings.xml into a dictionary
string_resources = {}
if os.path.exists(strings_path):
    with open(strings_path, 'r') as f:
        content = f.read()
        matches = re.findall(r'<string name="([^"]+)">([^<]+)</string>', content)
        for name, value in matches:
            string_resources[name] = value.strip()

def get_description(res_id):
    if res_id in string_resources:
        desc = string_resources[res_id]
        # Get first sentence or first line
        desc = desc.split('\\n')[0].split('.')[0].strip()
        return desc
    return "Command implementation."

for filename in os.listdir(cmd_dir):
    if filename.endswith(".java") and filename not in processed_files:
        path = os.path.join(cmd_dir, filename)
        with open(path, 'r') as f:
            content = f.read()
        
        # Find helpRes() resource ID
        res_match = re.search(r'public int helpRes\(\) \{\s+return R\.string\.(\w+);', content)
        if res_match:
            res_id = res_match.group(1)
            description = get_description(res_id)
            cmd_name = filename.replace(".java", "")
            
            # Prepare the getMetadata override
            metadata_code = f"""
    @Override
    public bhupendra.ai.launcher.commands.CommandMetadata getMetadata(android.content.Context context) {{
        return new bhupendra.ai.launcher.commands.CommandMetadata("{cmd_name}", "{description}", null, null, null);
    }}
"""
            
            # Check if getMetadata is already there (just in case)
            if "getMetadata" not in content:
                # Insert before the last closing brace
                last_brace_index = content.rfind('}')
                if last_brace_index != -1:
                    new_content = content[:last_brace_index] + metadata_code + content[last_brace_index:]
                    
                    # Add import if missing (using full paths in code to avoid import issues for now)
                    # but let's try to be cleaner if possible.
                    # Using full paths as I did above.
                    
                    with open(path, 'w') as f:
                        f.write(new_content)
                    print(f"Updated {filename} with description: {description}")
