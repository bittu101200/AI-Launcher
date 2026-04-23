fs_path = "TUI-ConsoleLauncher/app/src/main/java/bhupendra/ai/launcher/managers/FileSystemManager.java"
xml_prefs_path = "TUI-ConsoleLauncher/app/src/main/java/bhupendra/ai/launcher/managers/xml/XMLPrefsManager.java"

build_file_code = """
    public static Uri buildFile(Context context, File file) {
        Uri uri;
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            uri = Uri.fromFile(file);
        }
        else {
            uri = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", file);
        }
        return uri;
    }
"""

with open(fs_path, "r") as f:
    fs_content = f.read()

# Add buildFile before the last closing brace
fs_content = fs_content.rsplit("}", 1)[0] + build_file_code + "\n}\n"

with open(fs_path, "w") as f:
    f.write(fs_content)

with open(xml_prefs_path, "r") as f:
    xml_content = f.read()

xml_content = xml_content.replace("Tuils.init(context);", "bhupendra.ai.launcher.managers.FileSystemManager.init(context);")

with open(xml_prefs_path, "w") as f:
    f.write(xml_content)

