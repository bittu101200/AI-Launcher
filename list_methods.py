import re

with open('TUI-ConsoleLauncher/app/src/main/java/bhupendra/ai/launcher/tuils/Tuils.java', 'r') as f:
    content = f.read()

# Find all public static methods
# A simple regex for `public static <Return Type> <methodName>(...`
methods = re.findall(r'public static (?:<[^>]+>\s+)?([\w\<\>\[\]]+)\s+(\w+)\s*\(', content)
for m in methods:
    print(m[1])
