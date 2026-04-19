package bhupendra.ai.launcher.ai;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.provider.ContactsContract;
import android.content.ContentProviderOperation;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import org.json.JSONObject;
import java.util.ArrayList;

import bhupendra.ai.launcher.commands.Command;
import bhupendra.ai.launcher.commands.CommandTuils;
import bhupendra.ai.launcher.commands.main.MainPack;
import bhupendra.ai.launcher.managers.xml.XMLPrefsManager;
import bhupendra.ai.launcher.managers.xml.classes.XMLPrefsSave;
import bhupendra.ai.launcher.tuils.Tuils;

import static android.provider.Settings.System.SCREEN_BRIGHTNESS;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;

public class AndroidToolExecutor implements ToolExecutor {

    private static final String TAG = "AndroidToolExecutor";
    private MainPack mainPack;

    public void setMainPack(MainPack mainPack) {
        this.mainPack = mainPack;
    }

    @Override
    public String execute(Context context, Tool tool, String arguments) throws Exception {
        Log.d(TAG, "Executing tool: " + tool.name + " with args: " + arguments);
        if (tool.name.startsWith("launch:")) {
            String packageName = tool.name.substring("launch:".length());
            PackageManager pm = context.getPackageManager();
            Intent launchIntent = pm.getLaunchIntentForPackage(packageName);
            if (launchIntent == null) {
                return "[package not found: " + packageName + "]";
            }
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(launchIntent);
            return "[launched " + tool.description + "]";
        }

        if ("system.execute_command".equals(tool.name)) {
            JSONObject args = new JSONObject(arguments);
            String commandLine = args.getString("command");
            
            if (mainPack == null) {
                // Try to get from AISubsystem if not set directly
                AISubsystem ai = AISubsystem.getInstance();
                if (ai != null) mainPack = ai.getMainPack();
            }
            
            if (mainPack == null) return "[error: mainPack not available]";

            Command command = CommandTuils.parse(commandLine, mainPack);
            if (command == null) return "[error: unknown command: " + commandLine + "]";

            if (command.mArgs != null) {
                mainPack.clear();
                mainPack.set(command.mArgs);
            }
            String output = command.exec(mainPack);
            return output != null ? output : "[executed: " + commandLine + "]";
        }

        if ("system.search_config".equals(tool.name)) {
            JSONObject args = new JSONObject(arguments);
            String query = args.optString("query", "").toLowerCase();
            StringBuilder results = new StringBuilder();
            int count = 0;
            
            for (XMLPrefsManager.XMLPrefsRoot root : XMLPrefsManager.XMLPrefsRoot.values()) {
                for (XMLPrefsSave s : root.enums) {
                    if (s.label().toLowerCase().contains(query) || s.info().toLowerCase().contains(query)) {
                        results.append("- ").append(s.label())
                               .append(" (").append(root.name()).append("): ")
                               .append(s.info())
                               .append(" [current: ").append(XMLPrefsManager.get(s)).append("]\n");
                        count++;
                    }
                }
            }
            
            if (count == 0) return "[no configuration matches found for: " + query + "]";
            return "Found " + count + " matches for '" + query + "':\n" + results.toString();
        }

        if ("system.search_contacts".equals(tool.name)) {
            JSONObject args = new JSONObject(arguments);
            String query = args.optString("query", "").toLowerCase();
            
            if (mainPack == null) {
                AISubsystem ai = AISubsystem.getInstance();
                if (ai != null) mainPack = ai.getMainPack();
            }
            
            if (mainPack == null || mainPack.contacts == null) return "[error: contact manager not available]";
            
            java.util.List<bhupendra.ai.launcher.managers.ContactManager.Contact> contacts = mainPack.contacts.getContacts();
            StringBuilder results = new StringBuilder();
            int count = 0;
            
            for (bhupendra.ai.launcher.managers.ContactManager.Contact c : contacts) {
                String name = c.name.toLowerCase();
                int matchPercent = 0;
                if (name.equals(query)) matchPercent = 100;
                else if (name.startsWith(query)) matchPercent = 90;
                else if (name.contains(query)) matchPercent = 75;
                
                if (matchPercent >= 75) {
                    results.append("- ").append(c.name)
                           .append(" [Match: ").append(matchPercent).append("%]")
                           .append(": ").append(c.numbers.toString()).append("\n");
                    count++;
                }
            }
            
            if (count == 0) return "[no contacts found matching: " + query + "]";
            return "Found " + count + " contacts matching '" + query + "':\n" + results.toString();
        }

        if ("system.set_volume".equals(tool.name)) {
            JSONObject args = new JSONObject(arguments);
            String streamStr = args.getString("stream").toLowerCase();
            int percentage = args.getInt("percentage");
            
            int streamType = android.media.AudioManager.STREAM_MUSIC;
            if (streamStr.contains("ring")) streamType = android.media.AudioManager.STREAM_RING;
            else if (streamStr.contains("media")) streamType = android.media.AudioManager.STREAM_MUSIC;
            else if (streamStr.contains("alarm")) streamType = android.media.AudioManager.STREAM_ALARM;
            else if (streamStr.contains("notification")) streamType = android.media.AudioManager.STREAM_NOTIFICATION;
            else if (streamStr.contains("system")) streamType = android.media.AudioManager.STREAM_SYSTEM;
            else if (streamStr.contains("voice")) streamType = android.media.AudioManager.STREAM_VOICE_CALL;

            android.media.AudioManager am = (android.media.AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            int max = am.getStreamMaxVolume(streamType);
            int volume = (percentage * max) / 100;
            am.setStreamVolume(streamType, volume, android.media.AudioManager.FLAG_SHOW_UI);
            return "[set " + streamStr + " volume to " + percentage + "%]";
        }

        if ("system.memory_store".equals(tool.name)) {
            JSONObject args = new JSONObject(arguments);
            String key = args.getString("key");
            String value = args.getString("value");
            AISubsystem.getInstance().getLongTermMemory().store(key, value);
            return "[memory saved: " + key + "]";
        }

        if ("system.memory_retrieve".equals(tool.name)) {
            JSONObject args = new JSONObject(arguments);
            String query = args.optString("query", "");
            return AISubsystem.getInstance().getLongTermMemory().retrieve(query);
        }

        if ("system.search_web".equals(tool.name)) {
            JSONObject args = new JSONObject(arguments);
            String query = args.getString("query");
            String encoded = java.net.URLEncoder.encode(query, "UTF-8");
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + encoded));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return "[searching for: " + query + "]";
        }

        if ("system.web_search_query".equals(tool.name)) {
            JSONObject args = new JSONObject(arguments);
            String query = args.getString("query");
            String encoded = java.net.URLEncoder.encode(query, "UTF-8");
            // Use DuckDuckGo HTML for cleaner text results without browser
            return WebFetcher.fetch("https://html.duckduckgo.com/html/?q=" + encoded);
        }

        if ("system.web_fetch".equals(tool.name)) {
            JSONObject args = new JSONObject(arguments);
            String url = args.getString("url");
            return WebFetcher.fetch(url);
        }

        if ("system.uninstall_app".equals(tool.name)) {
            JSONObject args = new JSONObject(arguments);
            String packageName = args.getString("packageName");
            Intent intent = new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + packageName));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return "[initiating uninstallation for: " + packageName + "]";
        }

        if ("system.add_contact".equals(tool.name)) {
            JSONObject args = new JSONObject(arguments);
            String name = args.getString("name");
            String phone = args.getString("phone");

            if (androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED ||
                androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                
                if (context instanceof Activity) {
                    androidx.core.app.ActivityCompat.requestPermissions((Activity) context, 
                        new String[]{Manifest.permission.WRITE_CONTACTS, Manifest.permission.READ_CONTACTS}, 
                        bhupendra.ai.launcher.LauncherActivity.COMMAND_REQUEST_PERMISSION);
                }
                return "[waiting for contacts permission - please grant it and try again]";
            }

            ArrayList<ContentProviderOperation> ops = new ArrayList<>();

            ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                    .build());

            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                    .build());

            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone)
                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                    .build());

            try {
                context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
                return "[added contact: " + name + " (" + phone + ")]";
            } catch (Exception e) {
                return "[error adding contact: " + e.getMessage() + "]";
            }
        }

        if ("system.remove_contact".equals(tool.name)) {
            JSONObject args = new JSONObject(arguments);
            String name = args.getString("name");

            if (androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED ||
                androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                
                if (context instanceof Activity) {
                    androidx.core.app.ActivityCompat.requestPermissions((Activity) context, 
                        new String[]{Manifest.permission.WRITE_CONTACTS, Manifest.permission.READ_CONTACTS}, 
                        bhupendra.ai.launcher.LauncherActivity.COMMAND_REQUEST_PERMISSION);
                }
                return "[waiting for contacts permission - please grant it and try again]";
            }

            if (mainPack == null) {
                AISubsystem ai = AISubsystem.getInstance();
                if (ai != null) mainPack = ai.getMainPack();
            }

            if (mainPack == null || mainPack.contacts == null) return "[error: contact manager not available]";

            try {
                mainPack.contacts.delete(name);
                return "[removed contact: " + name + "]";
            } catch (Exception e) {
                return "[error removing contact: " + e.getMessage() + "]";
            }
        }

        if ("system.config".equals(tool.name)) {
            JSONObject args = new JSONObject(arguments);
            String action = args.getString("action"); // "get" or "set"
            String key = args.getString("key");
            
            if (mainPack == null) {
                AISubsystem ai = AISubsystem.getInstance();
                if (ai != null) mainPack = ai.getMainPack();
            }
            
            if (mainPack == null) return "[error: mainPack not available]";

            XMLPrefsSave save = null;
            // Search for the key in all known preference roots
            for (XMLPrefsManager.XMLPrefsRoot root : XMLPrefsManager.XMLPrefsRoot.values()) {
                for (XMLPrefsSave s : root.enums) {
                    if (s.label().equals(key)) {
                        save = s;
                        break;
                    }
                }
                if (save != null) break;
            }

            if (save == null) return "[error: config key not found: " + key + "]";

            if ("get".equals(action)) {
                return key + " = " + XMLPrefsManager.get(String.class, save);
            } else if ("set".equals(action)) {
                String value = args.getString("value");
                save.parent().write(save, value);
                
                if (context instanceof bhupendra.ai.launcher.tuils.interfaces.Reloadable) {
                    ((bhupendra.ai.launcher.tuils.interfaces.Reloadable) context).addMessage(save.parent().path(), save.label() + " -> " + value);
                }
                
                return "[set " + key + " to " + value + "]";
            }
            return "[error: invalid action: " + action + "]";
        }

        if ("system.set_brightness".equals(tool.name)) {
            JSONObject args = new JSONObject(arguments);
            int percentage = args.optInt("percentage", -1);
            if (percentage < 0 || percentage > 100) {
                return "[error: brightness must be 0-100]";
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M && !Settings.System.canWrite(context)) {
                context.startActivity(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                return "[waiting for write settings permission]";
            }

            final int b = percentage * 255 / 100;
            ContentResolver cResolver = context.getContentResolver();
            
            try {
                int autobrightnessState = Settings.System.getInt(cResolver, SCREEN_BRIGHTNESS_MODE);
                if (autobrightnessState == SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                    Settings.System.putInt(cResolver, SCREEN_BRIGHTNESS_MODE, SCREEN_BRIGHTNESS_MODE_MANUAL);
                }
            } catch (Exception e) {
                Log.w(TAG, "Could not check/set brightness mode", e);
            }

            Settings.System.putInt(cResolver, SCREEN_BRIGHTNESS, b);

            if (context instanceof Activity) {
                final Activity activity = (Activity) context;
                activity.runOnUiThread(() -> {
                    Window window = activity.getWindow();
                    WindowManager.LayoutParams lp = window.getAttributes();
                    lp.screenBrightness = (float) b / 255f;
                    window.setAttributes(lp);
                });
            }

            return "[brightness set to " + percentage + "%]";
        }

        if ("system.get_brightness".equals(tool.name)) {
            int b = Settings.System.getInt(context.getContentResolver(), SCREEN_BRIGHTNESS, -1);
            if (b == -1) return "[error: could not read brightness]";
            int percentage = b * 100 / 255;
            return "[current brightness is " + percentage + "%]";
        }

        throw new IllegalArgumentException("Unsupported tool: " + tool.name);
    }
}