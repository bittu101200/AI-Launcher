package bhupendra.ai.launcher.managers;

import android.Manifest;
import android.app.Activity;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import bhupendra.ai.launcher.LauncherActivity;

public final class PermissionManager {

    public enum PermissionKind {
        RUNTIME,
        SPECIAL
    }

    public static final class PermissionRequirement {
        public final String id;
        public final String title;
        public final String note;
        public final PermissionKind kind;
        public final String[] permissions;
        public final boolean granted;
        public final boolean available;

        private PermissionRequirement(String id, String title, String note, PermissionKind kind,
                                      String[] permissions, boolean granted, boolean available) {
            this.id = id;
            this.title = title;
            this.note = note;
            this.kind = kind;
            this.permissions = permissions;
            this.granted = granted;
            this.available = available;
        }

        public String statusLabel() {
            if (!available) return "not required";
            return granted ? "enabled" : "missing";
        }
    }

    private PermissionManager() {}

    public static List<PermissionRequirement> getRequirements(Context context) {
        List<PermissionRequirement> requirements = new ArrayList<>();
        addRuntime(requirements, context, "notifications", "Notifications",
                "Shows T-UI foreground/status notifications and media controls.",
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
                Manifest.permission.POST_NOTIFICATIONS);
        addRuntime(requirements, context, "location", "Location",
                "Enables the location command and location-aware AI tools.",
                true,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION);
        addRuntime(requirements, context, "contacts", "Contacts",
                "Lets commands and AI tools find, call, add, and remove contacts.",
                true,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.WRITE_CONTACTS);
        addRuntime(requirements, context, "phone", "Phone",
                "Enables direct call commands and network/mobile-state labels.",
                true,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.READ_PHONE_STATE);
        addRuntime(requirements, context, "camera", "Camera",
                "Enables flashlight/torch commands on devices that expose flash through camera.",
                true,
                Manifest.permission.CAMERA);
        addRuntime(requirements, context, "media", "Media storage",
                "Lets T-UI browse user media outside its private app folder.",
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO);
        addRuntime(requirements, context, "legacy_storage_read", "Shared storage read",
                "Lets Android 12 and older browse shared files outside T-UI's private folder.",
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU,
                Manifest.permission.READ_EXTERNAL_STORAGE);
        addRuntime(requirements, context, "legacy_storage_write", "Shared storage write",
                "Lets Android 9 and older write shared files directly.",
                Build.VERSION.SDK_INT < Build.VERSION_CODES.Q,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        requirements.add(special("notification_access", "Notification access",
                "Allows T-UI to read selected notifications and display important messages in the terminal.",
                hasNotificationAccess(context),
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2));
        requirements.add(special("usage_stats", "Usage access",
                "Allows app usage/app-state detection for launcher intelligence and app capabilities.",
                hasUsageStatsAccess(context),
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP));
        requirements.add(special("all_files", "All files access",
                "Allows broad file commands outside the app-private folder on Android 11+.",
                hasAllFilesAccess(context),
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R));
        requirements.add(special("overlay", "Display over other apps",
                "Allows overlay-style launcher behavior when enabled by supported features.",
                canDrawOverlays(context),
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M));
        requirements.add(special("write_settings", "Modify system settings",
                "Allows brightness and other system-setting commands to apply directly.",
                canWriteSettings(context),
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M));

        return Collections.unmodifiableList(requirements);
    }

    public static List<PermissionRequirement> getMissingRequirements(Context context) {
        List<PermissionRequirement> missing = new ArrayList<>();
        for (PermissionRequirement requirement : getRequirements(context)) {
            if (requirement.available && !requirement.granted) {
                missing.add(requirement);
            }
        }
        return missing;
    }

    public static List<String> getMissingRuntimePermissions(Context context) {
        List<String> missing = new ArrayList<>();
        for (PermissionRequirement requirement : getRequirements(context)) {
            if (requirement.kind != PermissionKind.RUNTIME || !requirement.available || requirement.granted) {
                continue;
            }
            for (String permission : requirement.permissions) {
                if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    missing.add(permission);
                }
            }
        }
        return missing;
    }

    public static boolean isAnyRuntimePermissionMissing(Context context) {
        return !getMissingRuntimePermissions(context).isEmpty();
    }

    public static PermissionRequirement requestNextMissing(Activity activity) {
        for (PermissionRequirement requirement : getRequirements(activity)) {
            if (!requirement.available || requirement.granted) {
                continue;
            }
            if (requirement.kind == PermissionKind.RUNTIME) {
                ActivityCompat.requestPermissions(activity, requirement.permissions, LauncherActivity.COMMAND_REQUEST_PERMISSION);
            } else {
                openSpecialPermission(activity, requirement.id);
            }
            return requirement;
        }
        return null;
    }

    public static boolean checkAndRequestRuntime(Activity activity) {
        for (PermissionRequirement requirement : getRequirements(activity)) {
            if (requirement.kind == PermissionKind.RUNTIME && requirement.available && !requirement.granted) {
                ActivityCompat.requestPermissions(activity, requirement.permissions, LauncherActivity.STARTING_PERMISSION);
                return true;
            }
        }
        return false;
    }

    public static void checkAndRequestSpecial(Activity activity) {
        for (PermissionRequirement requirement : getRequirements(activity)) {
            if (requirement.kind == PermissionKind.SPECIAL && requirement.available && !requirement.granted) {
                openSpecialPermission(activity, requirement.id);
                return;
            }
        }
    }

    public static String buildRequirementsReport(Context context, PermissionRequirement requesting) {
        StringBuilder builder = new StringBuilder("Permission requirements");
        int enabled = 0;
        int missing = 0;

        for (PermissionRequirement requirement : getRequirements(context)) {
            if (!requirement.available) {
                continue;
            }
            if (requirement.granted) enabled++;
            else missing++;
        }

        builder.append("\nEnabled: ").append(enabled).append("; Missing: ").append(missing);
        for (PermissionRequirement requirement : getRequirements(context)) {
            if (!requirement.available) {
                continue;
            }
            builder.append("\n")
                    .append(requirement.granted ? "[ok] " : "[missing] ")
                    .append(requirement.title)
                    .append(" - ")
                    .append(requirement.note);
        }

        if (requesting != null) {
            builder.append("\n\nRequesting next missing permission: ")
                    .append(requesting.title)
                    .append(requesting.kind == PermissionKind.SPECIAL
                            ? " (settings screen opened)."
                            : " (system permission dialog opened).");
        } else if (missing == 0) {
            builder.append("\n\nAll available permissions are enabled.");
        } else {
            builder.append("\n\nOpen this command from the launcher activity to request missing permissions.");
        }

        return builder.toString();
    }

    public static boolean hasNotificationAccess(Context context) {
        return DeviceStateManager.hasNotificationAccess(context);
    }

    public static boolean hasUsageStatsAccess(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return true;
        try {
            PackageManager packageManager = context.getPackageManager();
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(context.getPackageName(), 0);
            AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            if (appOpsManager == null) return false;
            int mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                    applicationInfo.uid, applicationInfo.packageName);
            return mode == AppOpsManager.MODE_ALLOWED;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static boolean canDrawOverlays(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context);
    }

    public static boolean canWriteSettings(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.System.canWrite(context);
    }

    public static boolean hasAllFilesAccess(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager();
    }

    public static void openSpecialPermission(Context context, String id) {
        if ("notification_access".equals(id)) {
            openNotificationAccessSettings(context);
        } else if ("usage_stats".equals(id)) {
            openUsageStatsSettings(context);
        } else if ("all_files".equals(id)) {
            openAllFilesAccessSettings(context);
        } else if ("overlay".equals(id)) {
            openOverlaySettings(context);
        } else if ("write_settings".equals(id)) {
            openWriteSettings(context);
        }
    }

    public static void openNotificationAccessSettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
        startSettings(context, intent);
    }

    public static void openUsageStatsSettings(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            startSettings(context, new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        }
    }

    public static void openOverlaySettings(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + context.getPackageName()));
            startSettings(context, intent);
        }
    }

    public static void openWriteSettings(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                    Uri.parse("package:" + context.getPackageName()));
            startSettings(context, intent);
        }
    }

    public static void openAllFilesAccessSettings(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:" + context.getPackageName()));
                startSettings(context, intent);
            } catch (Exception e) {
                startSettings(context, new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
            }
        }
    }

    private static void addRuntime(List<PermissionRequirement> requirements, Context context, String id,
                                   String title, String note, boolean available, String... permissions) {
        boolean granted = !available || arePermissionsGranted(context, permissions);
        requirements.add(new PermissionRequirement(id, title, note, PermissionKind.RUNTIME,
                permissions, granted, available));
    }

    private static PermissionRequirement special(String id, String title, String note,
                                                 boolean granted, boolean available) {
        return new PermissionRequirement(id, title, note, PermissionKind.SPECIAL,
                new String[0], granted || !available, available);
    }

    private static boolean arePermissionsGranted(Context context, String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private static void startSettings(Context context, Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
        }
    }
}
