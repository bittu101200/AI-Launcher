package bhupendra.ai.launcher.managers;

import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import java.io.File;
import android.app.ActivityManager.MemoryInfo;
import android.annotation.TargetApi;
import android.net.Uri;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.util.List;
import java.util.ArrayList;
import android.text.TextUtils;
import android.os.Process;
import android.content.BroadcastReceiver;
import bhupendra.ai.launcher.BuildConfig;
import bhupendra.ai.launcher.managers.xml.XMLPrefsManager;
import bhupendra.ai.launcher.managers.xml.options.Behavior;
import bhupendra.ai.launcher.tuils.interfaces.OnBatteryUpdate;
import bhupendra.ai.launcher.tuils.stuff.FakeLauncherActivity;
import bhupendra.ai.launcher.managers.notifications.NotificationService;


import bhupendra.ai.launcher.tuils.Tuils;


public class DeviceStateManager {

    private static BroadcastReceiver batteryReceiver = null;
    private static long total = -1;
    private static OnBatteryUpdate batteryUpdate;

    public static Intent requestAdmin(ComponentName component, String explanation) {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, explanation);
            return intent;
        }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
        public static void openSettingsPage(Context c, String packageName) {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", packageName, null);
            intent.setData(uri);
            c.startActivity(intent);
        }

    public static String getNetworkType(Context context) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_STATE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                return "unknown";
            }
            try {
                TelephonyManager mTelephonyManager = (TelephonyManager)
                        context.getSystemService(Context.TELEPHONY_SERVICE);
                int networkType = mTelephonyManager.getNetworkType();
                switch (networkType) {
                case TelephonyManager.NETWORK_TYPE_GPRS:
                case TelephonyManager.NETWORK_TYPE_EDGE:
                case TelephonyManager.NETWORK_TYPE_CDMA:
                case TelephonyManager.NETWORK_TYPE_1xRTT:
                case TelephonyManager.NETWORK_TYPE_IDEN:
                    return "2g";
                case TelephonyManager.NETWORK_TYPE_UMTS:
                case TelephonyManager.NETWORK_TYPE_EVDO_0:
                case TelephonyManager.NETWORK_TYPE_EVDO_A:
                case TelephonyManager.NETWORK_TYPE_HSDPA:
                case TelephonyManager.NETWORK_TYPE_HSUPA:
                case TelephonyManager.NETWORK_TYPE_HSPA:
                case TelephonyManager.NETWORK_TYPE_EVDO_B:
                case TelephonyManager.NETWORK_TYPE_EHRPD:
                case TelephonyManager.NETWORK_TYPE_HSPAP:
                    return "3g";
                case TelephonyManager.NETWORK_TYPE_LTE:
                    return "4g";
                case TelephonyManager.NETWORK_TYPE_NR:
                    return "5g";
                default:
                    return "unknown";
            }
            } catch (SecurityException e) {
                return "unknown";
            }
        }

    public static boolean hasInternetAccess() {
            try {
                HttpURLConnection urlc = (HttpURLConnection) (new URL("https://clients3.google.com/generate_204").openConnection());
                return (urlc.getResponseCode() == 204 && urlc.getContentLength() == 0);
            } catch (IOException e) {
                return false;
            }
        }

    public static long totalRam() {
            if(total > 0) return total;
    
            BufferedReader reader;
            try {
                reader = new BufferedReader(new InputStreamReader(new FileInputStream("/proc/meminfo")));
    
                String line;
                while((line = reader.readLine()) != null) {
                    if(line.startsWith("MemTotal")) {
                        line = line.replaceAll("\\D+", Tuils.EMPTYSTRING);
                        return Long.parseLong(line);
                    }
                }
            } catch (Exception e) {}
            return 0;
        }

    public static double freeRam(ActivityManager mgr, MemoryInfo info) {
            mgr.getMemoryInfo(info);
            return info.availMem;
        }

    public static void resetPreferredLauncherAndOpenChooser(Context context) {
            PackageManager packageManager = context.getPackageManager();
            ComponentName componentName = new ComponentName(context, FakeLauncherActivity.class);
            packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
    
            Intent selector = new Intent(Intent.ACTION_MAIN);
            selector.addCategory(Intent.CATEGORY_HOME);
            selector.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(selector);
    
            packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, PackageManager.DONT_KILL_APP);
        }

    public static boolean isMyLauncherDefault(PackageManager packageManager) {
            final IntentFilter filter = new IntentFilter(Intent.ACTION_MAIN);
            filter.addCategory(Intent.CATEGORY_HOME);
    
            List<IntentFilter> filters = new ArrayList<>();
            filters.add(filter);
    
            final String myPackageName = BuildConfig.APPLICATION_ID;
            List<ComponentName> activities = new ArrayList<>();
    
            // You can use name of your package here as third argument
            packageManager.getPreferredActivities(filters, activities, null);
    
            for (ComponentName activity : activities) {
                if (myPackageName.equals(activity.getPackageName())) {
                    return true;
                }
            }
            return false;
        }

    public static double getTotaleSpace(File dir, int unit) {
            if(dir == null) return -1;
    
            StatFs statFs = new StatFs(dir.getAbsolutePath());
            long blocks = statFs.getBlockCount();
            return Tuils.formatSize(blocks * statFs.getBlockSize(), unit);
        }

    public static double getAvailableSpace(File dir, int unit) {
            if(dir == null) return -1;
    
            StatFs statFs = new StatFs(dir.getAbsolutePath());
            long blocks = statFs.getAvailableBlocks();
            return Tuils.formatSize(blocks * statFs.getBlockSize(), unit);
        }

    public static double getTotalExternalMemorySize(int unit) {
            try {
                return getTotaleSpace(XMLPrefsManager.get(File.class, Behavior.external_storage_path), unit);
            } catch (Exception e) {
                return -1;
            }
        }

    public static double getAvailableExternalMemorySize(int unit) {
            try {
                return getAvailableSpace(XMLPrefsManager.get(File.class, Behavior.external_storage_path), unit);
            } catch (Exception e) {
                return -1;
            }
        }

    public static double getTotalInternalMemorySize(int unit) {
            return getTotaleSpace(Environment.getDataDirectory(), unit);
        }

    public static double getAvailableInternalMemorySize(int unit) {
            return getAvailableSpace(Environment.getDataDirectory(), unit);
        }

    public static boolean hasNotificationAccess(Context context) {
            String pkgName = BuildConfig.APPLICATION_ID;
            final String flat = Settings.Secure.getString(context.getContentResolver(), "enabled_notification_listeners");
            if (!TextUtils.isEmpty(flat)) {
                final String[] names = flat.split(":");
                for (int i = 0; i < names.length; i++) {
                    final ComponentName cn = ComponentName.unflattenFromString(names[i]);
                    if (cn != null) {
                        if (TextUtils.equals(pkgName, cn.getPackageName())) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

    public static boolean notificationServiceIsRunning(Context context) {
            ComponentName collectorComponent = new ComponentName(context, NotificationService.class);
            ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            boolean collectorRunning = false;
            List<ActivityManager.RunningServiceInfo> runningServices = manager.getRunningServices(Integer.MAX_VALUE);
            if (runningServices == null ) {
                return false;
            }
    
            for (ActivityManager.RunningServiceInfo service : runningServices) {
                if (service.service.equals(collectorComponent)) {
                    if (service.pid == Process.myPid()) {
                        collectorRunning = true;
                    }
                }
            }
    
            return collectorRunning;
        }

    public static void unregisterBatteryReceiver(Context context) {
            try {
                if(batteryReceiver != null) {
                    context.unregisterReceiver(batteryReceiver);
                    batteryReceiver = null;
                }
            } catch (Exception ignored) {}
        }

    public static void registerBatteryReceiver(Context context, OnBatteryUpdate listener) {
            try {
                batteryReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if(batteryUpdate == null) return;
    
                        switch (intent.getAction()) {
                            case Intent.ACTION_BATTERY_CHANGED:
                                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                                batteryUpdate.update(level);
                                break;
                            case Intent.ACTION_POWER_CONNECTED:
                                batteryUpdate.onCharging();
                                break;
                            case Intent.ACTION_POWER_DISCONNECTED:
                                batteryUpdate.onNotCharging();
                                break;
                        }
                    }
                };
    
                IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                iFilter.addAction(Intent.ACTION_POWER_CONNECTED);
                iFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
    
                context.registerReceiver(batteryReceiver, iFilter);
    
                batteryUpdate = listener;
            } catch (Exception e) {
                FileSystemManager.toFile(e);
            }
        }


    public static boolean isWifiConnected(Context context) {
        android.net.ConnectivityManager connManager = (android.net.ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connManager == null) return false;
        android.net.NetworkInfo mWifi = connManager.getNetworkInfo(android.net.ConnectivityManager.TYPE_WIFI);
        return mWifi != null && mWifi.isConnected();
    }

    public static int getBatteryLevel(Context context) {
        android.content.Intent batteryIntent = context.getApplicationContext().registerReceiver(null, new android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED));
        if (batteryIntent == null) return -1;
        int rawlevel = batteryIntent.getIntExtra("level", -1);
        int scale = batteryIntent.getIntExtra("scale", -1);
        if (rawlevel >= 0 && scale > 0) {
            return (int) (rawlevel * 100.0 / scale);
        }
        return -1;
    }

    public static boolean isMobileDataEnabled(Context context) {
        android.net.ConnectivityManager connManager = (android.net.ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connManager == null) return false;
        try {
            java.lang.reflect.Method method = connManager.getClass().getDeclaredMethod("getMobileDataEnabled");
            method.setAccessible(true);
            return (Boolean) method.invoke(connManager);
        } catch (Exception e) {
            return false;
        }
    }

    public static int getBrightnessPercentage(Context context) {
        try {
            int b = android.provider.Settings.System.getInt(context.getContentResolver(), android.provider.Settings.System.SCREEN_BRIGHTNESS);
            return b * 100 / 255;
        } catch (Exception e) {
            return 0;
        }
    }

    public static boolean isAutoBrightnessEnabled(Context context) {
        try {
            int mode = android.provider.Settings.System.getInt(context.getContentResolver(), android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE);
            return mode == android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isLocationEnabled(Context context) {
        android.location.LocationManager lm = (android.location.LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) return false;
        boolean gps_enabled = false;
        boolean network_enabled = false;
        try { gps_enabled = lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER); } catch(Exception ex) {}
        try { network_enabled = lm.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER); } catch(Exception ex) {}
        return gps_enabled || network_enabled;
    }

    public static boolean isBluetoothEnabled() {
        android.bluetooth.BluetoothAdapter adapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter();
        return adapter != null && adapter.isEnabled();
    }

}
