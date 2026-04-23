import re
import os

source_file = "TUI-ConsoleLauncher/app/src/main/java/bhupendra/ai/launcher/tuils/Tuils.java"
dest_dir = "TUI-ConsoleLauncher/app/src/main/java/bhupendra/ai/launcher/managers/"

with open(source_file, "r") as f:
    content = f.read()

def extract_method(method_name, is_overloaded=False):
    # This is a naive bracket counter for Java method extraction.
    # We find "public static .* methodName("
    pattern = r'(public\s+static\s+(?:<[^>]+>\s+)?[\w\<\>\[\]]+\s+' + method_name + r'\s*\()'
    matches = list(re.finditer(pattern, content))
    if not matches:
        return ""
    
    extracted = ""
    for match in matches:
        start_idx = match.start()
        
        # Also grab any leading comments/annotations just before the method.
        # Naively backtrack to find `@` or `/*` or `//` or just the previous newline.
        # Let's just grab from start_idx. Wait, we want to include annotations if present.
        prefix_start = start_idx
        while prefix_start > 0 and content[prefix_start-1] in [' ', '\t', '\n', '@'] or (prefix_start > 10 and content[prefix_start-10:prefix_start].strip().startswith('@')):
            prefix_start -= 1
            if content[prefix_start] == '\n':
                # Check line above
                prev_newline = content.rfind('\n', 0, prefix_start)
                line = content[prev_newline+1:prefix_start].strip()
                if not line.startswith('@') and not line.startswith('//'):
                    prefix_start += 1
                    break
        
        brace_count = 0
        in_method = False
        in_string = False
        in_char = False
        in_comment = False
        end_idx = start_idx
        
        for i in range(start_idx, len(content)):
            char = content[i]
            if char == '"' and not in_char and not in_comment:
                if i > 0 and content[i-1] != '\\':
                    in_string = not in_string
            elif char == "'" and not in_string and not in_comment:
                if i > 0 and content[i-1] != '\\':
                    in_char = not in_char
            elif char == '/' and i < len(content)-1 and content[i+1] == '*' and not in_string and not in_char:
                in_comment = True
            elif char == '*' and i < len(content)-1 and content[i+1] == '/' and in_comment:
                in_comment = False
            elif char == '{' and not in_string and not in_char and not in_comment:
                in_method = True
                brace_count += 1
            elif char == '}' and not in_string and not in_char and not in_comment:
                brace_count -= 1
                if in_method and brace_count == 0:
                    end_idx = i + 1
                    break
                    
        extracted += content[prefix_start:end_idx] + "\n\n"
    
    return extracted

# Let's see if this works for getFolder
print(extract_method("getFolder"))
