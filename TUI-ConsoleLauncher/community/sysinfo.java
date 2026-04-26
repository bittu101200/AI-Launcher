package de.reckendrees.systems.tui.expert.commands.main.raw;

import static android.content.Context.SENSOR_SERVICE;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
import static android.support.v4.app.ActivityCompat.requestPermissions;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.ConfigurationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.nfc.NfcAdapter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.CellSignalStrength;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.NetworkInterface;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Scanner;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import de.reckendrees.systems.tui.expert.BuildConfig;
import de.reckendrees.systems.tui.expert.R;
import de.reckendrees.systems.tui.expert.commands.CommandAbstraction;
import de.reckendrees.systems.tui.expert.commands.ExecutePack;
import de.reckendrees.systems.tui.expert.commands.main.MainPack;
import de.reckendrees.systems.tui.expert.tuils.Tuils;

/**
 * Created by Ryda® on 25/01/25.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class sysinfo implements CommandAbstraction {
    private String BatIcon;
    private String BatInfo;
    private String wifiIcon;
    private String mobileIcon;
    private String bluetoothIcon;

    public String[] getAllAppsPackageName(Context context){
        PackageManager packageManager = context.getPackageManager();
        @SuppressLint("QueryPermissionsNeeded") List<ApplicationInfo> packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);

        @SuppressLint("QueryPermissionsNeeded") List<ApplicationInfo> infoList = packages;
        String[] packageNames = new String[infoList.size()];
        int i ;
        for(i=0 ; i < infoList.size() ; i++){
            packageNames[i] = infoList.get(i).packageName;
        }
        return packageNames;
    }

    /**
     * Gets os codename.
     */
    public final String getAndroidOSName() {
        final String codename;
        switch (Build.VERSION.SDK_INT) {
            case Build.VERSION_CODES.BASE:
                codename = "First Android Version. Yay ! - Android 1.0 ( September 2008 )";
                break;
            case Build.VERSION_CODES.BASE_1_1:
                codename = "Base Android 1.1 ( February 2009 )";
                break;
            case Build.VERSION_CODES.CUPCAKE:
                codename = "Cupcake - Android 1.5 ( April 2009 )";
                break;
            case Build.VERSION_CODES.DONUT:
                codename = "Donut - Android 1.6 ( September 2009 )";
                break;
            case Build.VERSION_CODES.ECLAIR:
                codename = "Eclair - Android 2.0 ( October 2009 )";
                break;
            case Build.VERSION_CODES.ECLAIR_0_1:
                codename = "Eclair - Android 2.0.1 ( December 2009 )";
                break;
            case Build.VERSION_CODES.ECLAIR_MR1:
                codename = "Eclair - Android 2.1  ( January 2010 )";
                break;
            case Build.VERSION_CODES.FROYO:
                codename = "Froyo - Android 2.2 in May 2010";
                break;
            case Build.VERSION_CODES.GINGERBREAD:
                codename = "Gingerbread - Android 2.3 in December 2010";
                break;
            case Build.VERSION_CODES.GINGERBREAD_MR1:
                codename = "Gingerbread - Android 2.3.3 in February 2011";
                break;
            case Build.VERSION_CODES.HONEYCOMB:
                codename = "Honeycomb - Android 3.0 in February 2011";
                break;
            case Build.VERSION_CODES.HONEYCOMB_MR1:
                codename = "Honeycomb - Android 3.1 in May 2011";
                break;
            case Build.VERSION_CODES.HONEYCOMB_MR2:
                codename = "Honeycomb - Android 3.2 in July 2011";
                break;
            case Build.VERSION_CODES.ICE_CREAM_SANDWICH:
                codename = "Ice Cream Sandwich - Android 4.0 in October 2011";
                break;
            case Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1:
                codename = "Ice Cream Sandwich - Android 4.03 in December 2011";
                break;
            case Build.VERSION_CODES.JELLY_BEAN:
                codename = "Jelly Bean - Android 4.1 in July 2012";
                break;
            case Build.VERSION_CODES.JELLY_BEAN_MR1:
                codename = "Jelly Bean - Android 4.2 in November 2012";
                break;
            case Build.VERSION_CODES.JELLY_BEAN_MR2:
                codename = "Jelly Bean - Android 4.3 in July 2013";
                break;
            case Build.VERSION_CODES.KITKAT:
                codename = "Kitkat - Android 4.4 in October 2013";
                break;
            case Build.VERSION_CODES.KITKAT_WATCH:
                codename = "Kitkat Watch - Android 4.4W in June 2014";
                break;
            case Build.VERSION_CODES.LOLLIPOP:
                codename = "Lollipop - Android 5.0 in November 2014";
                break;
            case Build.VERSION_CODES.LOLLIPOP_MR1:
                codename = "Lollipop - Android 5.1 in March 2015";
                break;
            case Build.VERSION_CODES.M:
                codename = "Marshmallow - Android 6.0 in October 2015";
                break;
            case Build.VERSION_CODES.N:
                codename = "Nougat - Android 7.0 in August 2016";
                break;
            case Build.VERSION_CODES.N_MR1:
                codename = "Nougat MR1 - Android 7.1 in October 2016";
                break;
            case Build.VERSION_CODES.O:
                codename = "Oreo - Android 8.0 in August 2017";
                break;
            case Build.VERSION_CODES.O_MR1:
                codename = "Oreo - Android 8.1 in December 2017";
                break;
            case Build.VERSION_CODES.P:
                codename = "Pie - Android 9 in August 2018";
                break;
            case Build.VERSION_CODES.Q:
                codename = "Quince Tart - Android 10 in September 2019";
                break;
            case Build.VERSION_CODES.R:
                codename = "Red Velvet - Android 11 in September 2020.";
                break;
            case 31:
            case 32:
                codename = "Android 12(Snow Cone) - October 4, 2021";
                break;
            case 33:
                codename = "Android 13(Tiramisu) - August 15, 2022";
                break;
            case 34:
                codename = "Android 14(Upside Down Cake) - October 4, 2023";
                break;
            case 35:
                codename = "Android 15(Vanilla Ice Cream) - September 3, 2024";
                break;
            case 36:
                codename = "Android 16(Baklava) - March 13, 2025";
                break;
            default:
                codename = "?";
                break;
        }
        return codename;
    }

    public String getKernelVersion() {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec("uname -a");
        } catch (IOException e) {
            e.printStackTrace();
        }
        InputStream is;
        try {
            assert process != null;
            if (process.waitFor() == 0) {
                is = process.getInputStream();
            } else {
                is = process.getErrorStream();
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line = br.readLine();
            br.close();
            return line;
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
            return "Error";
        }
    }

    public static boolean isWifiConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            for (Network network : connectivityManager.getAllNetworks()) {
                NetworkInfo networkInfo = connectivityManager.getNetworkInfo(network);
                if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI && networkInfo.isConnected()) {
                    return true;
                }
            }
        }
        return false;
    }

    @SuppressLint("ObsoleteSdkInt")
    static String getWifiMacAddress() {
        try {
            String interfaceName = "wlan0";
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                if (!intf.getName().equalsIgnoreCase(interfaceName)) {
                    continue;
                }
                byte[] mac = new byte[0];
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD) {
                    mac = intf.getHardwareAddress();
                }
                if (mac == null) {
                    return "";
                }
                StringBuilder buf = new StringBuilder();
                for (byte aMac : mac) {
                    buf.append(String.format("%02X:", aMac));
                }
                if (buf.length() > 0) {
                    buf.deleteCharAt(buf.length() - 1);
                }
                return buf.toString();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "";
    }

    /**
     * @return whether adb debugging is enabled or not in boolean
     */
    @SuppressLint("ObsoleteSdkInt")
    public boolean isADBDebuggingEnabled(Context context){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CUPCAKE) {
            return Settings.Secure.getInt(context.getContentResolver(), "adb_enabled" , 0) > 0 ;
        }
        return false;
    }

    public static boolean isMobileDataConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            for (Network network : connectivityManager.getAllNetworks()) {
                NetworkInfo networkInfo = connectivityManager.getNetworkInfo(network);
                if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE && networkInfo.isConnected()) {
                    return true;
                }
            }
        }
        return false;
    }

    static String getProcessor() {
        String Final = "";
        try {
            StringBuilder sb = new StringBuilder();
            if (new File("/proc/cpuinfo").exists()) {
                try {
                    BufferedReader br = new BufferedReader(new FileReader("/proc/cpuinfo"));
                    String aLine;
                    while ((aLine = br.readLine()) != null) {
                        String _append = aLine + "ndeviceinfo";
                        sb.append(_append);
                    }
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                String cpuinfo[] = sb.toString().split(":");
                for (int a = 0; a < cpuinfo.length; a++) {
                    if (cpuinfo[a].toLowerCase().contains("processor")) {
                        int getlastindex = cpuinfo[a + 1].indexOf("ndeviceinfo");
                        Final = cpuinfo[a + 1].substring(1, getlastindex);
                        break;
                    }
                }
                if (Final.equals("0") || Final.equals("")) {
                    for (int a = 0; a < cpuinfo.length; a++) {
                        if (cpuinfo[a].contains("model name")) {
                            int getlastindex = cpuinfo[a + 1].indexOf("ndeviceinfo");
                            Final = cpuinfo[a + 1].substring(1, getlastindex);
                            break;
                        }
                    }
                }
                if (Final.equals("") || Final.equals("0")) {
                    Final = "Unknown";
                }
            } else {
                Final = "Unknown";
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return Final;
    }

    static String getProcessorHardware() {
        String Final = "";
        try {
            StringBuilder sb = new StringBuilder();
            if (new File("/proc/cpuinfo").exists()) {
                try {
                    BufferedReader br = new BufferedReader(new FileReader("/proc/cpuinfo"));
                    String aLine;
                    while ((aLine = br.readLine()) != null) {
                        String _append = aLine + "ndeviceinfo";
                        sb.append(_append);
                    }
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                String cpuinfo[] = sb.toString().split(":");
                for (int a = 0; a < cpuinfo.length; a++) {
                    if (cpuinfo[a].toLowerCase().contains("hardware")) {
                        int getlastindex = cpuinfo[a + 1].indexOf("ndeviceinfo");
                        Final = cpuinfo[a + 1].substring(1, getlastindex);
                        break;
                    } else {
                        Final = "Unknown";
                    }
                }
            } else {
                Final = "Unknown";
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return Final;
    }

    static String getCPUGoverner() {
        String aLine = "";
        if (new File("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor").exists()) {
            try {
                BufferedReader br = new BufferedReader(new FileReader("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor"));
                aLine = br.readLine();
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return aLine;
    }

    public static int getNumCores() {
        //Private Class to display only CPU devices in the directory listing
        class CpuFilter implements FileFilter {
            @Override
            public boolean accept(File pathname) {
                //Check if filename is "cpu", followed by a single digit number
                return Pattern.matches("cpu[0-9]+", pathname.getName());
            }
        }

        try {
            //Get directory containing CPU info
            File dir = new File("/sys/devices/system/cpu/");
            //Filter to only list the devices we care about
            File[] files = dir.listFiles(new CpuFilter());
            //Return the number of cores (virtual CPU devices)
            return Objects.requireNonNull(files).length;
        } catch (Exception e) {
            //Default to return 1 core
            return 1;
        }
    }

    public float getCpuTemp() {
        Process p;
        try {
            p = Runtime.getRuntime().exec("cat sys/class/thermal/thermal_zone0/temp");
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line = reader.readLine();

            return Float.parseFloat(line) / 1000.0f;

        } catch (Exception e) {
            e.printStackTrace();
            return 0.0f;
        }
    }

      static boolean isRooted() {
        String buildTags = android.os.Build.TAGS;
        return buildTags != null && buildTags.contains("test-keys") || canExecuteCommand("/system/xbin/which su") || canExecuteCommand("/system/bin/which su") || canExecuteCommand("which su");
    }

    private static boolean canExecuteCommand(String command) {
        try {
            int exitValue = Runtime.getRuntime().exec(command).waitFor();
            return exitValue == 0;
        } catch (Exception e) {
            return false;
        }
    }

    static String GetSELinuxMode() {
        StringBuilder output = new StringBuilder();
        Process p;
        try {
            p = Runtime.getRuntime().exec("getenforce");
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return "Not Supported";
        }
        String response = output.toString();
        if ("Enforcing".equals(response)) {
            return "Enforcing";
        } else if ("Permissive".equals(response)) {
            return "Permissive";
        } else {
            return "Unable to determine";
        }
    }
    /**
     * Get the network type, for example Wifi, mobile, wimax, or none.
     */
    public static String getNetworkType(Context context) {
        ConnectivityManager manager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null) return "TYPE_UNKNOWN";

        NetworkInfo info = manager.getActiveNetworkInfo();

        return (info == null) ? "TYPE_UNKNOWN" : info.getTypeName();
    }

     private static int readSystemFileAsInt(final String systemFile){
        InputStream inputStream;
        try {
            final Process process = new ProcessBuilder(new String[] {"/system/bin/cat" , systemFile}).start();
            inputStream = process.getInputStream();
            final String content = readFully(inputStream);
            return Integer.parseInt(content);
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }

    }

    public static String readFully(final InputStream pInputStream) throws IOException {
        final StringBuilder sb = new StringBuilder();
        final Scanner sc = new Scanner(pInputStream);
        while(sc.hasNextLine()) {
            sb.append(sc.nextLine());
        }
        return sb.toString();
    }

    private static MatchResult matchSystemFile(final String systemFile, final String pattern, final int horizon) throws Exception {
        InputStream in;
        try {
            final Process process = new ProcessBuilder(new String[] { "/system/bin/cat", systemFile }).start();

            in = process.getInputStream();
            final Scanner scanner = new Scanner(in);

            final boolean matchFound = scanner.findWithinHorizon(pattern, horizon) != null;
            if(matchFound) {
                return scanner.match();
            } else {
                throw new Exception();
            }
        } catch (final IOException e) {
            throw new Exception(e);
        }
    }

    /**
     * BogoMips (from "bogus" and MIPS) is a crude measurement of CPU speed made by the Linux kernel
     * when it boots to calibrate an internal busy-loop.
     * @return float
     */
    public static float getBogoMips() throws Exception {
        final MatchResult matchResult = sysinfo.matchSystemFile("/proc/cpuinfo", "BogoMIPS[\\s]*:[\\s]*(\\d+\\.\\d+)[\\s]*\n", 1000);

        try {
            if(matchResult.groupCount() > 0) {
                return Float.parseFloat(matchResult.group(1));
            } else {
                throw new Exception();
            }
        } catch (final NumberFormatException e) {
            throw new Exception(e);
        }
    }

    /**
     * To check whether your device is 34-bit or 64-bit
     * @return boolean
     */
    public static String is64Bit(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (Build.SUPPORTED_64_BIT_ABIS.length > 0) {
                return "64-bit";
           }
                return "34-bit";
        }
        return "34-bit";
    }

    /**
     * It returns the security patch date
     * Since Build.VERSION.SECURITY_PATCH is added in api 23
     * Added other method to get the latest security patch date
     */
    public String getSecurityPathDate() {
        if (Build.VERSION.SDK_INT >= 23) {
            return Build.VERSION.SECURITY_PATCH; // Tingkat patch keamanan yang dapat dilihat pengguna.
        } else {
            try {
                Process process = new ProcessBuilder()
                        .command("/system/bin/getprop")
                        .redirectErrorStream(true)
                        .start();
                InputStream is = process.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String line;
                StringBuilder str = new StringBuilder();

                while ((line = br.readLine()) != null) {
                    str.append(line).append("\n");
                    if (str.toString().contains("security_patch")) {
                        String[] split = line.split(":");
                        if (split.length == 2) {
                            return split[1];
                        }
                        break;
                    }
                }
                br.close();
                process.destroy();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "";
    }

     //=================-=-=-=-=-=-=================
    @SuppressLint({"HardwareIds", "SwitchIntDef"})
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public String exec(ExecutePack pack) throws Exception {
        MainPack info = (MainPack) pack;

//         Gets language.
        String getLanguage = Locale.getDefault().getLanguage(); //en
        String getDisplayName = Locale.getDefault().getDisplayName(); // English(United States)

//         Get the current timezone of the device.
        String getTimeZone = Calendar.getInstance().getTimeZone().getID();

//         To get the device's rotation angle
        int getOrientation = pack.context.getResources().getConfiguration().orientation;
        String Orientation = null;
        switch (getOrientation) {
            case 1:
                Orientation = "Portrait";
                break;
            case 2:
                Orientation = "Landscape";
                break;
            case 0:
                Orientation = "Undefined";
                break;
        }

//        display
        //* To get rotation of the device
        int getRotation;
        WindowManager wm = (WindowManager) pack.context.getSystemService(Context.WINDOW_SERVICE);
        int angle;
        int rotation = wm.getDefaultDisplay().getRotation();
        switch (rotation) {
            case Surface.ROTATION_90:
                angle = 90; //-90
                break;
            case Surface.ROTATION_180:
                angle = 180;
                break;
            case Surface.ROTATION_270:
                angle = 270; //+90
                break;
            default:
                angle = 0;
                break;

        }
        getRotation = angle;

        Display display = wm.getDefaultDisplay();
        int getScreenDisplayID = display.getDisplayId();
        String displayName = display.getName();
        int minLum = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            minLum = (int) display.getHdrCapabilities().getDesiredMinLuminance();
        }
        int maxLum = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            maxLum = (int) display.getHdrCapabilities().getDesiredMaxLuminance();
        }
        String hardDisLuminance = "Min: " + minLum + " Max: " + maxLum;

        int NavigationBarHeight;
        DisplayMetrics metrics = new DisplayMetrics();

        display.getMetrics(metrics);
        float ScreenDensity = metrics.density;
        int usableWidth = metrics.widthPixels;
        int usableHeight = metrics.heightPixels;
        int pixX = (int) metrics.xdpi;
        int pixY = (int) metrics.ydpi;
        String hardDisPixelPerIn = "X: " + pixX + " Y: " + pixY;

        //* To get the physical size of the device
        float widthInches = metrics.widthPixels / metrics.xdpi;
        float heightInches = metrics.heightPixels / metrics.ydpi;
        double sizeInch = Math.sqrt(Math.pow(widthInches, 2) + Math.pow(heightInches, 2));

        display.getRealMetrics(metrics);
        int realWidth = metrics.widthPixels;
        int realHeight = metrics.heightPixels;

        if (realHeight > usableHeight) {
            NavigationBarHeight = realHeight - usableHeight;
        } else {
            NavigationBarHeight = 0;
        }

        //* To get the font scale of the display
        float getFontScale;
        getFontScale = pack.context.getResources().getConfiguration().fontScale;

        //* To get the device's display's refresh rate in frame per second
        float getRefreshRate;
        getRefreshRate = display.getRefreshRate();

        // Configuration configuration = new Configuration();
        Configuration configuration = pack.context.getResources().getConfiguration();

        //* To check Hdr Capabilities of the screen
        String isHdrCapable = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (display.isHdr()) {
                isHdrCapable = "Supported";
            } else {
                isHdrCapable = "Not Supported";
            }
        }

        //* To check if night mode is active or not
        boolean isNightModeActive = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            isNightModeActive = configuration.isNightModeActive();
        }

        //* To check if Screen is rounded or not
        boolean isScreenRound;
        isScreenRound = configuration.isScreenRound();

        //* To check if screen wide color gamut or not
        boolean is_ScreenWideColorGamut = false;
        configuration = pack.context.getResources().getConfiguration();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            is_ScreenWideColorGamut = configuration.isScreenWideColorGamut();
        }
        String isScreenWideColorGamut;
        if (is_ScreenWideColorGamut) {
            isScreenWideColorGamut = "supported";
        } else {
            isScreenWideColorGamut = "not_supported";
        }

        //* To get the screen timeout value of the android device
        int getScreenTimeout;
        try {
            getScreenTimeout = Settings.System.getInt(pack.context.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
            getScreenTimeout = 0;
        }

//        Operating System
        // String Android_version = Build.VERSION.RELEASE; // String versi yang dapat dilihat pengguna.
        String Preview_SDK = String.valueOf(Build.VERSION.PREVIEW_SDK_INT); // Revisi pratinjau pengembang dari SDK prarilis.
        String build_version_sdk = String.valueOf(Build.VERSION.SDK_INT); // API Level
        String BASE_OS = Build.VERSION.BASE_OS;
        String build_id = Build.ID;
        String version_code_name = Build.VERSION.CODENAME;
        String host = Build.HOST;
        String build_tags = Build.TAGS;
        String build_user = Build.USER;
        String Flavor = Get_FromBuildProp("flavor");

        String getPlayServicesVersion;
        try {
            PackageInfo packageInfo = pack.context.getPackageManager().getPackageInfo("com.google.android.gms", 0);
            getPlayServicesVersion = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            getPlayServicesVersion = "GPlayServicesVersion: " + e;
        }

        String javaVendor = System.getProperty("java.vendor");// 	Java Runtime Environment vendor
        String javajavaVendorUrl = System.getProperty("java.vendor.url");// 	Java vendor URL
        String javahome = System.getProperty("java.home");// 	Java installation directory
        String javavmSpecificationVersion = System.getProperty("java.vm.specification.version");// 	Java Virtual Machine specification version
        String java_vm_specification_vendor = System.getProperty("java.vm.specification.vendor");// 	Java Virtual Machine specification vendor
        String java_vm_specification_name = System.getProperty("java.vm.specification.name");// 	Java Virtual Machine specification name
        String java_vm_version = System.getProperty("java.vm.version");// 	Java Virtual Machine implementation version
        String java_vm_vendor = System.getProperty("java.vm.vendor");// 	Java Virtual Machine implementation vendor
        String java_vm_name = System.getProperty("java.vm.name");// 	Java Virtual Machine implementation name
        String java_specification_vendor = ("java.specification.vendor ");//	Java Runtime Environment specification vendor
        String java_specification_name = System.getProperty("java.specification.name");// 	Java Runtime Environment specification name
        String java_class_version = System.getProperty("java.class.version");// 	Java class format version number
        String java_library_path = System.getProperty("java.library.path");// 	List of paths to search when loading libraries
        String java_io_tmpdir = System.getProperty("java.io.tmpdir");// 	Default temp file path
        String java_compiler = System.getProperty("java.compiler");// 	Name of JIT compiler to use
        String java_ext_dirs = System.getProperty("java.ext.dirs"); // 	Path of extension directory or directories
        String java_name = System.getProperty("os.name");//  	Operating system name
        String java_arch = System.getProperty("os.arch");// 	Operating system architecture
        String java_version = System.getProperty("os.version");//  	Operating system version
        String user_name = System.getProperty("user.name");//  	User's account name
        String user_home = System.getProperty("user.home");//  	User's home directory
        String user_dir = System.getProperty("user.dir");//  	User's current working directory

        String Home = Get_FromBuildProp("browser_homepag");
        String os_name = System.getProperty("os.name");
        String system_boot_loader_version = Build.BOOTLOADER;
        long build_time = Build.TIME;
        long[] upTime = getUpTime();
        String sysOsUpTime = upTime[0] + " Days, " + upTime[1] + ":" + upTime[2] + ":" + upTime[3];
        final boolean IS_EMULATOR = Get_FromBuildProp("kernel.qemu") == "1";
        String MIN_TARGET_SDK = "MIN_TARGET_SDK: " + Get_FromBuildProp("min_supported_target_sdk") + Get_FromBuildProp("minmatch");
        String NOTIFICATION_SOUND = Get_FromBuildProp("notification_sound");
        String ALARM_ALERT = Get_FromBuildProp("alarm_alert");
        String RINGTONE = Get_FromBuildProp("ringtone");
        String DATA_ENCRYPTION = Get_FromBuildProp("crypto.state");
        String TYPE_ENCRYPTION = Get_FromBuildProp("crypto.type");
        String NAME_ENCRYPTION = Get_FromBuildProp("crypto.volume.filenames_mode");

//        device info
        String display_version = Build.DISPLAY;
        String build_board = Build.BOARD;
        String brand_name = Build.BRAND;
        String version_incremental = Build.VERSION.INCREMENTAL; //Nilai internal yang digunakan oleh kontrol sumber yang mendasari untuk merepresentasikan versi ini.
        String build_device = Build.DEVICE;
        String device_unique_fingerprint = Build.FINGERPRINT;
        String hardware = Build.HARDWARE;
        String manufacturer = Build.MANUFACTURER;
        String device_model = Build.MODEL;

        //* Android ID It is a 64-bit hex string which is generated on the device's first boot. Generally it won't be changed unless is factory reset.
        String device_unique_id = Settings.Secure.getString(pack.context.getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);

        String product = Build.PRODUCT;
        String radio_version = Build.getRadioVersion();
        @SuppressLint("HardwareIds") String serial = Build.SERIAL;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            serial = Build.getSerial();
        }
        String CPUARCH = Build.CPU_ABI;
        String SUPPORTED_ABIS = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            SUPPORTED_ABIS = Arrays.toString(Build.SUPPORTED_ABIS);
        }

//        List of available sensors
        SensorManager sm = (SensorManager) pack.context.getSystemService(SENSOR_SERVICE);
        List<Sensor> list = sm.getSensorList(Sensor.TYPE_ALL);
        String SensorsList = "";
        for (Sensor s : list) {
            SensorsList = s.getName(); // + "\n";
        }

        int getTotalNumberOfSensors;
        getTotalNumberOfSensors = sm.getSensorList(Sensor.TYPE_ALL).size();

//        wifi
        boolean wifiConnected = isWifiConnected(pack.context);
        if (wifiConnected) {
            // wifiConnected = true
            wifiIcon = "\uF1EB ";
        } else {
            // wifiConnected = false
            wifiIcon = "\uF204 ";
        }

//        Memory
        ActivityManager activityManager = (ActivityManager) pack.context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        Runtime runtime = Runtime.getRuntime();
        String storagePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        StatFs statFs = new StatFs(storagePath);

        long tRam = memoryInfo.totalMem; // "Total Memory: "
        long aRam = memoryInfo.availMem; // "Available Memory: "
        long mRam = runtime.maxMemory(); // "Runtime Maximum Memory: "
        long rtRam = runtime.totalMemory(); // "Runtime Total Memory: "
        long rfRam = runtime.freeMemory(); // "Runtime Free Memory: "
        long uRam = tRam - aRam; // "Usable Memory: "
        float aRamInPer = ((float) aRam / tRam) * 100;

        long tSto = statFs.getTotalBytes();
        long aSto = statFs.getAvailableBytes();
        long uSto = tSto - aSto;
        float aStoInPer = ((float) aSto / tSto) * 100;

//        battery
        Intent batteryIntent = info.context.getApplicationContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        boolean isPresent = batteryIntent.getBooleanExtra("present", false);
        int rawlevel = batteryIntent.getIntExtra("level", -1);
        double scale = batteryIntent.getIntExtra("scale", -1);
        String technology = batteryIntent.getStringExtra("technology");
        int plugged = batteryIntent.getIntExtra("plugged", -1);
        int health = batteryIntent.getIntExtra("health", 0);
        int status = batteryIntent.getIntExtra("status", 0);
        double voltage = batteryIntent.getIntExtra("voltage", 0);
        int temperature = batteryIntent.getIntExtra("temperature", 0);
        float firehnhite = (temperature * (9 / 5)) + 32;

        if (isPresent) {
            BatInfo = "BATTERY";
        } else {
            BatInfo = "?";
        }

        int level = -1;
        if (rawlevel >= 0 && scale > 0) {
            level = (int) ((rawlevel * 100) / scale);
        }

        if (getStatusString(status) == "Charging") {
            BatIcon = "\uD83D\uDD0C";
        }

        if (getStatusString(status) == "Discharging" || getStatusString(status) == "Not Charging") {
            if (level <= 10) {
                BatIcon = "\uF244 ";
            } else if (level <= 25) {
                BatIcon = "\uF243 ";
            } else if (level <= 50) {
                BatIcon = "\uF242 ";
            } else if (level <= 75) {
                BatIcon = "\uF241 ";
            } else {
                BatIcon = "\uF240 ";
            }
        }

        BatteryManager batteryManager = (BatteryManager) pack.context.getSystemService(Context.BATTERY_SERVICE);
        String LevelText = "";
        if (getStatusString(status) == "Charging") {
            BatIcon = "\uD83D\uDD0C";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                long timeFullyCharged = batteryManager.computeChargeTimeRemaining();
                long hours = timeFullyCharged / (1000 * 60 * 60);
                long minutes = (timeFullyCharged % (1000 * 60 * 60)) / (1000 * 60);
                long seconds = ((timeFullyCharged % (1000 * 60 * 60)) % (1000 * 60)) / 1000;
                LevelText = (hours + "h " + minutes + "m " + seconds + "s");
            }
        }
        if (getStatusString(status) == "Unknown" || getStatusString(status) == "Full") {
            BatIcon = "\uF240 ";
        }

//        mobile
        boolean mobileOn = isMobileDataConnected(pack.context);
        if (mobileOn) {
            // wifiConnected = true
            mobileIcon = "\uF012 ";
        } else {
            // wifiConnected = false
            mobileIcon = "\uF204 ";
        }

//        Phone type either it is GSM  or CDMA or SIP
        TelephonyManager telephonyManager = (TelephonyManager) pack.context.getSystemService(Context.TELEPHONY_SERVICE);

        String getPhoneType;
        int phoneType = telephonyManager.getPhoneType();
        if (phoneType == TelephonyManager.PHONE_TYPE_CDMA) {
            getPhoneType = "CDMA";
        } else if (phoneType == TelephonyManager.PHONE_TYPE_GSM) {
            getPhoneType = "GSM";
        } else if (phoneType == TelephonyManager.PHONE_TYPE_SIP) {
            getPhoneType = "SIP";
        } else {
            getPhoneType = "";
        }

        String isSimNetworkLocked;
        if ((telephonyManager != null) && (telephonyManager.getSimState() == TelephonyManager.SIM_STATE_NETWORK_LOCKED)) {
            isSimNetworkLocked = "YES";
        } else {
            isSimNetworkLocked = "NO";
        }

//        brightness
        ContentResolver cResolver = pack.context.getApplicationContext().getContentResolver();
        int b = 0;
        try {
            b = Settings.System.getInt(cResolver, SCREEN_BRIGHTNESS);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        b = b * 100 / 255;

        int autobrightnessState = Integer.MIN_VALUE;
        try {
            autobrightnessState = Settings.System.getInt(cResolver, SCREEN_BRIGHTNESS_MODE);
        } catch (Exception e) {
            e.printStackTrace();
        }

//        location
        LocationManager lm = (LocationManager) pack.context.getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        if (ActivityCompat.checkSelfPermission(pack.context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(pack.context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions((Activity) pack.context, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            return pack.context.getString(R.string.output_waitingpermission);
        }

        Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location == null) {
            location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        } else if (location == null) {
            location = lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
        }

//        bluetooth
        BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();
        boolean bluetoothOn;

        if (bta == null) bluetoothOn = false;
        else bluetoothOn = bta.isEnabled();

        if (bluetoothOn) {
            // bluetoothOn = true
            bluetoothIcon = "\uF293 ";
        } else {
            // bluetoothOn = false
            bluetoothIcon = "\uF294 ";
        }
        PackageManager pm = pack.context.getPackageManager();
        boolean isBT = pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);
        boolean isBTE = pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
        String bt_Name = bta.getName();
        int bt_State = bta.getState();
        int bt_ScanMode = bta.getScanMode();
        String bt_Address = bta.getAddress();
        int bt_MaximumAdvertisingDataLength = 0;
        boolean bt_Le2MPhySupported = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            bt_MaximumAdvertisingDataLength = bta.getLeMaximumAdvertisingDataLength();
            bt_Le2MPhySupported = bta.isLe2MPhySupported();
        }
        int bt_hashCode = bta.hashCode();
        boolean bt_Discovering = bta.isDiscovering();
        boolean bt_MultipleAdvertisementSupported = bta.isMultipleAdvertisementSupported();
        @SuppressLint("HardwareIds")
        String getBluetoothAddress = (bta != null) ? bta.getAddress() : "";

        // Checks whether USB Host is supported or not
        boolean isUsbHostSupported;
        String Usb_Host = "NotSupport";
        isUsbHostSupported = pack.context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_USB_HOST);
        if (isUsbHostSupported) {
            Usb_Host = "Supported";
        }

//        nfc
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(info.context.getApplicationContext());

        boolean nfcOn = false;
        if (nfcAdapter != null) {
            nfcOn = nfcAdapter.isEnabled();
        }

//        camera
        // Check if camera is available or not , FEATURE_CAMERA_ANY will look for all cameras(including rear cameras)
        boolean isCameraAvailable;
        isCameraAvailable = pack.context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);

        //* Check if flash is available or not in the camera
        boolean isFlashAvailable;
        isFlashAvailable = pack.context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);

        //* Get all the cameraId lists
        String[] getCameraIds;
        CameraManager cameraManager = (CameraManager) pack.context.getSystemService(Context.CAMERA_SERVICE);
        try {
            getCameraIds = cameraManager.getCameraIdList();
        } catch (CameraAccessException e) {
            e.printStackTrace();
            getCameraIds = null;
        }

        //* Get the number of the cameras in the device
        //* It calls the getCameraIds() method which will give all the cameraIds lists and then return length
        int getNumberOfCameras;
        getNumberOfCameras = getCameraIds.length;

        final int REQUEST_CODE = 101;
        if (ContextCompat.checkSelfPermission(pack.context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            // if permissions are not provided we are requesting for permissions.
            requestPermissions((Activity) pack.context, new String[]{Manifest.permission.READ_PHONE_STATE}, REQUEST_CODE);
            return pack.context.getString(R.string.output_waitingpermission);
        }

//        SimInfo
        // number of sim slot available in android device
        int getNumberOfSimSlot;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            SubscriptionManager subscriptionManager = SubscriptionManager.from(pack.context);
            getNumberOfSimSlot = subscriptionManager.getActiveSubscriptionInfoCountMax();
        } else {
            // A method have to be implemented to get number of simSlot in less than Lollipop
            getNumberOfSimSlot = 1;
        }

        SubscriptionManager sManager = (SubscriptionManager) pack.context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        SubscriptionInfo SimInfo = sManager.getActiveSubscriptionInfoForSimSlotIndex(0);
        SubscriptionInfo SimInfo1 = sManager.getActiveSubscriptionInfoForSimSlotIndex(1);
        SubscriptionInfo SimInfo2 = sManager.getActiveSubscriptionInfoForSimSlotIndex(2);
        String PNumber = null;
        String get_Imei = null;
        CharSequence CarrierName = null;
        CharSequence SIMOperator = null;
        String IccId = null;
        String Mcc = null;
        String Mnc = null;
        String hashCode = null;

        switch (getNumberOfSimSlot) {
            case 0:
                break;
            case 1:
                PNumber = SimInfo.getNumber();
                get_Imei = telephonyManager.getDeviceId(0);
                CarrierName = SimInfo.getCarrierName();
                SIMOperator = SimInfo.getDisplayName();
                IccId = SimInfo.getIccId();
                Mcc = String.valueOf(SimInfo.getMcc());
                Mnc = String.valueOf(SimInfo.getMnc());
                hashCode = String.valueOf(SimInfo.hashCode());
                break;
            case 2:
                PNumber = SimInfo.getNumber() + ", " + SimInfo1.getNumber();
                get_Imei = telephonyManager.getDeviceId(0) + ", " + telephonyManager.getDeviceId(1);
                CarrierName = SimInfo.getCarrierName() + ", " + SimInfo1.getCarrierName();
                SIMOperator = SimInfo.getDisplayName() + ", " + SimInfo1.getDisplayName();
                IccId = SimInfo.getIccId() + ", " + SimInfo1.getIccId();
                Mcc = SimInfo.getMcc() + ", " + SimInfo1.getMcc();
                Mnc = SimInfo.getMnc() + ", " + SimInfo1.getMnc();
                hashCode = SimInfo.hashCode() + ", " + SimInfo1.hashCode();
                break;
            case 3:
                PNumber = SimInfo.getNumber() + ", " + SimInfo1.getNumber() + ", " + SimInfo2.getNumber();
                get_Imei = telephonyManager.getDeviceId(0) + ", " + telephonyManager.getDeviceId(1) + ", " + telephonyManager.getDeviceId(2);
                CarrierName = SimInfo.getCarrierName() + ", " + SimInfo1.getCarrierName() + ", " + SimInfo2.getCarrierName();
                SIMOperator = SimInfo.getDisplayName() + ", " + SimInfo1.getDisplayName() + ", " + SimInfo2.getDisplayName();
                IccId = SimInfo.getIccId() + ", " + SimInfo1.getIccId() + ", " + SimInfo2.getIccId();
                Mcc = SimInfo.getMcc() + ", " + SimInfo1.getMcc() + ", " + SimInfo2.getMcc();
                Mnc = SimInfo.getMnc() + ", " + SimInfo1.getMnc() + ", " + SimInfo2.getMnc();
                hashCode = SimInfo.hashCode() + ", " + SimInfo1.hashCode() + ", " + SimInfo2.hashCode();
                break;
        }

//         WLAN MAC address for a device
//         Your application will require the permission “android.permission.ACCESS_WIFI_STATE” given in the manifest file.
        WifiManager m_wm = (WifiManager) pack.context.getSystemService(Context.WIFI_SERVICE);
        String m_wlanMacAdd = m_wm.getConnectionInfo().getMacAddress();

//         IMSI 
        String get_imsi = telephonyManager.getSubscriberId();

//         IMEI: (International Mobile Equipment Identity)          

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            switch (getNumberOfSimSlot) {
                case 0:
                    break;
                case 1:
                    get_Imei = telephonyManager.getImei(0);
                    break;
                case 2:
                    get_Imei = telephonyManager.getImei(0) + ", " + telephonyManager.getImei(1);
                    break;
                case 3:
                    get_Imei = telephonyManager.getImei(0) + ", " + telephonyManager.getImei(1) + ", " + telephonyManager.getImei(2);
                    break;
            }
        }

        String netCellSignalStrength = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (telephonyManager.getSignalStrength() != null) {
                for (CellSignalStrength cellSignalStrength : telephonyManager.getSignalStrength().getCellSignalStrengths()) {
                    netCellSignalStrength = "dBm " + cellSignalStrength.getDbm();
                }
            }
        } else {
            try {
                Method method = TelephonyManager.class.getMethod("getSignalStrength");
                Object signalStrength = method.invoke(telephonyManager);
                if (signalStrength != null) {
                    Method getDbmMethod = signalStrength.getClass().getMethod("getDbm");
                    Object dbmObject = getDbmMethod.invoke(signalStrength);
                    if (dbmObject != null) {
                        int dbm = (Integer) dbmObject;
                        netCellSignalStrength = "dBm " + dbm;
                    }
                }
            } catch (Exception e) {
                netCellSignalStrength = "not_available: " + e;
            }
        }

//        GPU
        //* To check if GPU is supported or not
        boolean isGPUSupported;
        final ConfigurationInfo configurationInfo = activityManager
                .getDeviceConfigurationInfo();
        isGPUSupported = configurationInfo.reqGlEsVersion >= 0x20000;

        //* To get the minimum cpu frequency in kiloHertz.
        int getMinimumFrequency = sysinfo.readSystemFileAsInt("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_min_freq");

        //* To get the maximum cpu frequency in kiloHertz.
        int getMaximumFrequency = sysinfo.readSystemFileAsInt("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq");

        //* To get the clock speed of the CPU
        int getClockSpeed = sysinfo.readSystemFileAsInt("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq");

        //* Get minimum scaling frequency of the CPU kiloHertz(in int)
        //int getMinScalingFrequency = sysinfo.readSystemFileAsInt("/sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq");

        //* Get maximum scaling frequency of the cpu kiloHertz(in int)
        //int getMaxScalingFrequency = sysinfo.readSystemFileAsInt("/sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq");

        // -------------------------------------------------------------
        return Tuils.NEWLINE +
                "  __i    " + info.res.getString(R.string.phone_type) + Tuils.SPACE + getPhoneType + Tuils.NEWLINE +
                " |---|   " + Tuils.NEWLINE +
                " |[_]|   " + "   TUI-E Version:" + Tuils.SPACE + BuildConfig.VERSION_NAME + " (code: " + BuildConfig.VERSION_CODE + ")" + Tuils.NEWLINE +
                " |:::|   " + (BuildConfig.DEBUG ? BuildConfig.BUILD_TYPE : Tuils.EMPTYSTRING) + Tuils.NEWLINE +
                " |:::|   " + os_name + Tuils.NEWLINE +
                " `\u2216   \u2216  " + Orientation + Tuils.NEWLINE +
                "    \u2216_=_\u2216 " + Tuils.NEWLINE + Tuils.NEWLINE +

                "--- \uE70e  " + info.res.getString(R.string.Operating_System) + "---" + Tuils.NEWLINE +
                info.res.getString(R.string.version_release) + Tuils.SPACE + "(" + getAndroidOSName() + ") - " + is64Bit() + Tuils.NEWLINE +
                info.res.getString(R.string.version_code_name) + Tuils.SPACE + version_code_name + Tuils.NEWLINE +
                info.res.getString(R.string.android_base_os_version_name) + Tuils.SPACE + BASE_OS + Tuils.NEWLINE +
                info.res.getString(R.string.Preview_SDK) + Tuils.SPACE + Preview_SDK + Tuils.NEWLINE +
                info.res.getString(R.string.build_version_sdk) + Tuils.SPACE + build_version_sdk + Tuils.SPACE + MIN_TARGET_SDK + Tuils.NEWLINE +
                info.res.getString(R.string.build_id) + Tuils.SPACE + build_id + Tuils.NEWLINE +
                info.res.getString(R.string.Kernel_Version) + Tuils.SPACE + getKernelVersion() + Tuils.NEWLINE +
                info.res.getString(R.string.host) + Tuils.SPACE + host + Tuils.NEWLINE +
                info.res.getString(R.string.build_tags) + Tuils.SPACE + build_tags + Tuils.NEWLINE +
                info.res.getString(R.string.build_user) + Tuils.SPACE + build_user + Tuils.NEWLINE +
                "Flavor" + Tuils.SPACE + Flavor + Tuils.NEWLINE +
                "  " + info.res.getString(R.string.language) + Tuils.SPACE + getLanguage + " - " + getDisplayName + Tuils.NEWLINE +
                info.res.getString(R.string.Time_Zone) + Tuils.SPACE + getTimeZone + Tuils.NEWLINE +
                info.res.getString(R.string.system_boot_loader_version) + Tuils.SPACE + system_boot_loader_version + Tuils.NEWLINE +
                info.res.getString(R.string.GetSELinuxMode) + Tuils.SPACE + GetSELinuxMode() + Tuils.NEWLINE +
                info.res.getString(R.string.Security_Patch) + Tuils.SPACE + getSecurityPathDate() + Tuils.NEWLINE +
                "/data Status:" + Tuils.SPACE + DATA_ENCRYPTION + Tuils.SPACE + "[" + TYPE_ENCRYPTION + "]" + Tuils.SPACE + NAME_ENCRYPTION + Tuils.NEWLINE +
                info.res.getString(R.string.isRooted) + Tuils.SPACE + isRooted() + Tuils.NEWLINE +
                info.res.getString(R.string.build_time) + Tuils.SPACE + DateFormat.format("dd-MM-yyyy | hh:mm a", new Date(build_time)).toString() + Tuils.NEWLINE +
                info.res.getString(R.string.sys_Os_UpTime) + Tuils.SPACE + sysOsUpTime + Tuils.NEWLINE +
                info.res.getString(R.string.GPlay_Services_Version) + Tuils.SPACE + getPlayServicesVersion + Tuils.NEWLINE +
                //"advertisingId:" + Tuils.SPACE + advertisingId + Tuils.NEWLINE +
                info.res.getString(R.string.IsEmulator) + Tuils.SPACE + IS_EMULATOR + Tuils.NEWLINE +
                "NOTIFICATION_SOUND:" + Tuils.SPACE + NOTIFICATION_SOUND + Tuils.NEWLINE +
                "ALARM_ALERT:" + Tuils.SPACE + ALARM_ALERT + Tuils.NEWLINE +
                "RINGTONE:" + Tuils.SPACE + RINGTONE + Tuils.NEWLINE +
                "HomePage:" + Tuils.SPACE + Home + Tuils.NEWLINE +

                "Java Runtime Environment vendor:"+ Tuils.SPACE +  javaVendor + Tuils.NEWLINE +
                "Java vendor URL:"+ Tuils.SPACE +  javajavaVendorUrl + Tuils.NEWLINE +
                "Java installation directory:"+ Tuils.SPACE +  javahome + Tuils.NEWLINE +
                "Java Virtual Machine specification version:"+ Tuils.SPACE +  javavmSpecificationVersion + Tuils.NEWLINE +
                "Java Virtual Machine specification vendor:"+ Tuils.SPACE +  java_vm_specification_vendor + Tuils.NEWLINE +
                "Java Virtual Machine specification name:"+ Tuils.SPACE +  java_vm_specification_name + Tuils.NEWLINE +
                "Java Virtual Machine implementation version:"+ Tuils.SPACE +  java_vm_version + Tuils.NEWLINE +
                "Java Virtual Machine implementation vendor:"+ Tuils.SPACE +  java_vm_vendor + Tuils.NEWLINE +
                "Java Virtual Machine implementation name:"+ Tuils.SPACE +  java_vm_name + Tuils.NEWLINE +
                "Java Runtime Environment specification vendor:"+ Tuils.SPACE +  java_specification_vendor + Tuils.NEWLINE +
                "Java Runtime Environment specification name:"+ Tuils.SPACE +  java_specification_name + Tuils.NEWLINE +
                "Java class format version number:"+ Tuils.SPACE +  java_class_version + Tuils.NEWLINE +
                "List of paths to search when loading libraries:"+ Tuils.SPACE +  java_library_path + Tuils.NEWLINE +
                "Default temp file path:"+ Tuils.SPACE +  java_io_tmpdir + Tuils.NEWLINE +
                "Name of JIT compiler to use:"+ Tuils.SPACE +  java_compiler + Tuils.NEWLINE +
                "Path of extension directory or directories:"+ Tuils.SPACE +  java_ext_dirs + Tuils.NEWLINE +
                "Operating system name:"+ Tuils.SPACE +  java_name + Tuils.NEWLINE +
                "Operating system architecture:"+ Tuils.SPACE +  java_arch + Tuils.NEWLINE +
                "Operating system version:"+ Tuils.SPACE +  java_version + Tuils.NEWLINE +
                "User's account name:"+ Tuils.SPACE +  user_name + Tuils.NEWLINE +
                "User's home directory:"+ Tuils.SPACE +  user_home + Tuils.NEWLINE +
                "User's current working directory:" + Tuils.SPACE +  user_dir + Tuils.NEWLINE +

                Tuils.NEWLINE + "--- \uf42c  " + info.res.getString(R.string.Device_Info) + "---" + Tuils.NEWLINE +
                info.res.getString(R.string.build_board) + Tuils.SPACE + build_board + Tuils.NEWLINE +
                info.res.getString(R.string.brand_name) + Tuils.SPACE + brand_name.toUpperCase() + Tuils.NEWLINE +
                info.res.getString(R.string.hardware) + Tuils.SPACE + hardware + Tuils.NEWLINE +
                info.res.getString(R.string.manufacturer) + Tuils.SPACE + manufacturer + Tuils.NEWLINE +
                info.res.getString(R.string.build_device) + Tuils.SPACE + build_device + Tuils.NEWLINE +
                "Device Name:" + Tuils.SPACE + Settings.Global.getString(pack.context.getContentResolver(), "device_name") + Tuils.NEWLINE +
                info.res.getString(R.string.device_model) + Tuils.SPACE + device_model + Tuils.NEWLINE +
                info.res.getString(R.string.device_unique_id) + Tuils.SPACE + device_unique_id + Tuils.NEWLINE +
                "IMSI:" + Tuils.SPACE + get_imsi + Tuils.NEWLINE +
                info.res.getString(R.string.radio_version) + Tuils.SPACE + radio_version + Tuils.NEWLINE +
                info.res.getString(R.string.product) + Tuils.SPACE + product + Tuils.NEWLINE +
                info.res.getString(R.string.serial) + Tuils.SPACE + serial + Tuils.NEWLINE +
                info.res.getString(R.string.version_incremental) + Tuils.SPACE + version_incremental + Tuils.NEWLINE +
                info.res.getString(R.string.device_unique_fingerprint) + Tuils.SPACE + device_unique_fingerprint + Tuils.NEWLINE +
                info.res.getString(R.string.is_ADB_Debugging_Enabled) + Tuils.SPACE + isADBDebuggingEnabled(pack.context) + Tuils.NEWLINE +
                info.res.getString(R.string.Usb_Host) + Tuils.SPACE + Usb_Host + Tuils.NEWLINE +

                Tuils.NEWLINE + "--- Display ---" + Tuils.NEWLINE +
                info.res.getString(R.string.display_version) + Tuils.SPACE + display_version + Tuils.NEWLINE +
                "\uF042   " + info.res.getString(R.string.brightness_label) + Tuils.SPACE + (autobrightnessState == SCREEN_BRIGHTNESS_MODE_AUTOMATIC ? "(auto) " : Tuils.EMPTYSTRING) + b + "%" + Tuils.NEWLINE +
                info.res.getString(R.string.Screen_Timeout) + Tuils.SPACE + (getScreenTimeout / 1000) + Tuils.NEWLINE +
                info.res.getString(R.string.ScreenDisplayID) + Tuils.SPACE + getScreenDisplayID + Tuils.NEWLINE +
                info.res.getString(R.string.DisplayName) + Tuils.SPACE + displayName + Tuils.NEWLINE +
                info.res.getString(R.string.ScreenUsable) + Tuils.SPACE + usableWidth + " x " + usableHeight + " pixel " + Tuils.NEWLINE +
                info.res.getString(R.string.ScreenReal) + Tuils.SPACE + realWidth + " x " + realHeight + " pixel " + Tuils.NEWLINE +
                info.res.getString(R.string.NavigationBarHeight) + Tuils.SPACE + NavigationBarHeight + Tuils.NEWLINE +
                info.res.getString(R.string.ScreenSize) + Tuils.SPACE + String.format(Locale.US, "%.1f", sizeInch) + " In" + Tuils.NEWLINE +
                info.res.getString(R.string.isHdrCapable) + Tuils.SPACE + isHdrCapable + Tuils.NEWLINE +
                info.res.getString(R.string.densityDpi) + Tuils.SPACE + metrics.densityDpi + " dpi" + Tuils.NEWLINE +
                info.res.getString(R.string.ScreenDensity) + Tuils.SPACE + ScreenDensity + Tuils.NEWLINE +
                info.res.getString(R.string.FontScale) + Tuils.SPACE + getFontScale + Tuils.NEWLINE +
                info.res.getString(R.string.RefreshRate) + Tuils.SPACE + getRefreshRate + " Hz" + Tuils.NEWLINE +
                info.res.getString(R.string.isNightModeActive) + Tuils.SPACE + isNightModeActive + Tuils.NEWLINE +
                info.res.getString(R.string.isScreenRound) + Tuils.SPACE + isScreenRound + Tuils.NEWLINE +
                info.res.getString(R.string.isScreenWideColorGamut) + Tuils.SPACE + isScreenWideColorGamut + Tuils.NEWLINE +
                info.res.getString(R.string.Rotation) + Tuils.SPACE + getRotation + Tuils.NEWLINE +
                info.res.getString(R.string.Orientation) + Tuils.SPACE + Orientation + Tuils.NEWLINE +
                info.res.getString(R.string.hardDisLuminance) + Tuils.SPACE + hardDisLuminance + Tuils.NEWLINE +
                info.res.getString(R.string.hardDisPixelPerIn) + Tuils.SPACE + hardDisPixelPerIn + Tuils.NEWLINE +
                //Tuils.NEWLINE + display + Tuils.NEWLINE +

                Tuils.NEWLINE + "--- \uF986  CPU ---" + Tuils.NEWLINE +
                info.res.getString(R.string.Processor) + Tuils.SPACE + getProcessor() + Tuils.NEWLINE +
                info.res.getString(R.string.ProcessorHardware) + Tuils.SPACE + getProcessorHardware() + Tuils.NEWLINE +
                info.res.getString(R.string.CPU_Governer) + Tuils.SPACE + getCPUGoverner() + Tuils.NEWLINE +
                info.res.getString(R.string.CPUARCH_label) + Tuils.SPACE + CPUARCH + " (Core " + getNumCores() + ") " + Tuils.NEWLINE +
                info.res.getString(R.string.SUPPORTED_CPUARCH_label) + Tuils.SPACE + SUPPORTED_ABIS + Tuils.NEWLINE +
                info.res.getString(R.string.CPU_Temp) + Tuils.SPACE + getCpuTemp() + Tuils.NEWLINE +
                info.res.getString(R.string.Frequency) + Tuils.SPACE +
                Tuils.SPACE + (getMinimumFrequency / 1000) + " MHz" + Tuils.SPACE +
                "-" + Tuils.SPACE + (getMaximumFrequency / 1000) + " MHz" + Tuils.NEWLINE +
                info.res.getString(R.string.ClockSpeed) + Tuils.SPACE + (getClockSpeed / 1000) + " MHz" + Tuils.NEWLINE +
                info.res.getString(R.string.BogoMips) + Tuils.SPACE + getBogoMips() + Tuils.NEWLINE +
                info.res.getString(R.string.isGPUSupported) + Tuils.SPACE + isGPUSupported + Tuils.NEWLINE +

                Tuils.NEWLINE + "--- \uF9a6  Memory ---" + Tuils.NEWLINE +
                info.res.getString(R.string.RamSize) + Tuils.SPACE + formatSize(tRam) + Tuils.NEWLINE +
                info.res.getString(R.string.RamUsable) + Tuils.SPACE + formatSize(uRam) + Tuils.NEWLINE +
                info.res.getString(R.string.RamAvailable) + Tuils.SPACE + formatSize(aRam) + " - " + String.format(Locale.US, "%.2f", aRamInPer) + "%" + Tuils.NEWLINE +
                "Runtime Maximum Memory:" + Tuils.SPACE + formatSize(mRam) + Tuils.NEWLINE +
                "Runtime Total Memory:" + Tuils.SPACE + formatSize(rtRam) + Tuils.NEWLINE +
                "Runtime Free Memory::" + Tuils.SPACE + formatSize(rfRam) + Tuils.NEWLINE +

                Tuils.NEWLINE + "--- \uf7c9  Storage ---" + Tuils.NEWLINE +
                info.res.getString(R.string.StorageDirectories) + Tuils.SPACE + storagePath + Tuils.NEWLINE +
                info.res.getString(R.string.StorageSize) + Tuils.SPACE + formatSize(tSto) + Tuils.NEWLINE +
                info.res.getString(R.string.StorageUsable) + Tuils.SPACE + formatSize(uSto) + Tuils.NEWLINE +
                info.res.getString(R.string.StorageAvailable) + Tuils.SPACE + formatSize(aSto) + " - " + String.format(Locale.US, "%.2f", aStoInPer) + "%" + Tuils.NEWLINE + Tuils.NEWLINE +
                //info.res.getString(R.string.isExternalStorageAvailable) + Tuils.SPACE + externalStorageAvailable() + Tuils.NEWLINE +

                "     ╭━╮     " + "--- " + BatIcon + Tuils.SPACE + BatInfo + Tuils.SPACE + getBatteryCapacity(info.context) + " mAh ---" + Tuils.NEWLINE +
                "  ╭━━┃█┃━━╮  " + Tuils.NEWLINE +
                "  ┃███████┃  " + info.res.getString(R.string.battery_label) + Tuils.SPACE + level + "%" + Tuils.NEWLINE +
                "  ┃███████┃  " + info.res.getString(R.string.battery_Technology) + Tuils.SPACE + technology + Tuils.NEWLINE +
                "  ┃███████┃  " + info.res.getString(R.string.battery_Plugged) + Tuils.SPACE + getPlugTypeString(plugged) + Tuils.NEWLINE +
                "  ┃███████┃  " + info.res.getString(R.string.battery_Health) + Tuils.SPACE + getHealthString(health) + Tuils.NEWLINE +
                "  ┃███████┃  " + info.res.getString(R.string.battery_Status) + Tuils.SPACE + getStatusString(status) + Tuils.SPACE + LevelText + Tuils.NEWLINE +
                "  ┃███████┃  " + info.res.getString(R.string.battery_Voltage) + Tuils.SPACE + (voltage / 1000) + "v" + Tuils.NEWLINE +
                "  ┃███████┃  " + info.res.getString(R.string.battery_Temperature) + Tuils.SPACE + (temperature / 10) + (char) 0x00B0 + "C" + " / " + (firehnhite / 10) + (char) 0x00B0 + "F" + Tuils.NEWLINE +
                "  ╰━━━━━━━╯  " + Tuils.NEWLINE +

                Tuils.NEWLINE + "--- wifi ---" + Tuils.NEWLINE +
                wifiIcon + "  " + info.res.getString(R.string.wifi_label) + Tuils.SPACE + wifiConnected + Tuils.SPACE + "(" + getWifiMacAddress() + ")" + Tuils.NEWLINE +
                "m_wlanMacAddAddress:" + Tuils.SPACE + m_wlanMacAdd + Tuils.NEWLINE +

                Tuils.NEWLINE + "--- mobile data ---" + Tuils.NEWLINE +
                mobileIcon + "  " + info.res.getString(R.string.mobile_data_label) + Tuils.SPACE + mobileOn + Tuils.NEWLINE +
                info.res.getString(R.string.NetworkType) + Tuils.SPACE + getNetworkType(pack.context) + Tuils.NEWLINE +
                info.res.getString(R.string.NumberOfSimSlot) + Tuils.SPACE + getNumberOfSimSlot + Tuils.NEWLINE +
                "Phone Number:" + Tuils.SPACE + PNumber + Tuils.NEWLINE +
                "CarrierName:" + Tuils.SPACE + CarrierName + Tuils.NEWLINE +
                "SIMOperator:" + Tuils.SPACE + SIMOperator + Tuils.NEWLINE +
                info.res.getString(R.string.IMEI) + Tuils.SPACE + get_Imei + Tuils.NEWLINE +
                "IccId:" + Tuils.SPACE + IccId + Tuils.NEWLINE +
                "Mcc:" + Tuils.SPACE + Mcc + Tuils.NEWLINE +
                "Mnc:" + Tuils.SPACE + Mnc + Tuils.NEWLINE +
                "hashCode:" + Tuils.SPACE + hashCode + Tuils.NEWLINE +

                info.res.getString(R.string.netCellSignalStrength) + Tuils.SPACE + netCellSignalStrength + Tuils.NEWLINE +
                info.res.getString(R.string.SimNetworkLocked) + Tuils.SPACE + isSimNetworkLocked + Tuils.NEWLINE +

                Tuils.NEWLINE + "--- " + bluetoothIcon + " Bluetooth ---" + Tuils.NEWLINE +
                "isBT" + Tuils.SPACE + isBT + Tuils.NEWLINE +
                "isBTE" + Tuils.SPACE + isBTE + Tuils.NEWLINE +
                info.res.getString(R.string.bluetooth_label) + Tuils.SPACE + bluetoothOn + Tuils.NEWLINE +
                info.res.getString(R.string.Bluetooth_Address) + Tuils.SPACE + getBluetoothAddress + Tuils.NEWLINE +
                "ScanMode:" + Tuils.SPACE + bt_ScanMode + Tuils.NEWLINE +
                "Adapter:" + Tuils.SPACE + bta + Tuils.NEWLINE +
                "Name:" + Tuils.SPACE + bt_Name + Tuils.NEWLINE +
                "State:" + Tuils.SPACE + bt_State + Tuils.NEWLINE +
                "Address:" + Tuils.SPACE + bt_Address + Tuils.NEWLINE +
                "MaximumAdvertisingDataLength:" + Tuils.SPACE + bt_MaximumAdvertisingDataLength + Tuils.NEWLINE +
                "Discovering:" + Tuils.SPACE + bt_Discovering + Tuils.NEWLINE +
                "hashCode:" + Tuils.SPACE + bt_hashCode + Tuils.NEWLINE +
                "Le2MPhySupported:" + Tuils.SPACE + bt_Le2MPhySupported + Tuils.NEWLINE +
                "MultipleAdvertisementSupported:" + Tuils.SPACE + bt_MultipleAdvertisementSupported + Tuils.NEWLINE +

                Tuils.NEWLINE + "--- \uF21D  Gps ---" + Tuils.NEWLINE +
                info.res.getString(R.string.location_label) + Tuils.SPACE + (gps_enabled || network_enabled) + Tuils.NEWLINE +
                info.res.getString(R.string.Location_Address) + Tuils.SPACE + location + Tuils.NEWLINE +

                Tuils.NEWLINE + "--- \uf42c  nfc ---" + Tuils.NEWLINE +
                info.res.getString(R.string.nfc_label) + Tuils.SPACE + nfcOn + Tuils.NEWLINE +

                Tuils.NEWLINE + "--- Camera ---" + Tuils.NEWLINE +
                info.res.getString(R.string.isCameraAvailable) + Tuils.SPACE + isCameraAvailable + Tuils.NEWLINE +
                info.res.getString(R.string.isFlashAvailable) + Tuils.SPACE + isFlashAvailable + Tuils.NEWLINE +
                info.res.getString(R.string.NumberOfCameras) + Tuils.SPACE + getNumberOfCameras + Tuils.NEWLINE +
                info.res.getString(R.string.CameraIds) + Tuils.SPACE + Arrays.toString(getCameraIds) + Tuils.NEWLINE +

                Tuils.NEWLINE + "--- SENSORS ---" + Tuils.NEWLINE +
                info.res.getString(R.string.SensorsList) + Tuils.SPACE + SensorsList + Tuils.NEWLINE +
                info.res.getString(R.string.getTotalNumberOfSensors) + Tuils.SPACE + getTotalNumberOfSensors + Tuils.NEWLINE +
                Tuils.NEWLINE + "--- Apps Package Name ---" + Tuils.NEWLINE +
                " All Apps Package Name:" + Tuils.SPACE + Arrays.toString(getAllAppsPackageName(pack.context)) + Tuils.NEWLINE +

                "----------------------------------------------";
    }

     private long[] getUpTime(){
        long[] upTime = new long[4];
        long uptimeMillis = SystemClock.elapsedRealtime();
        long uptimeSeconds = uptimeMillis / 1000;
        long days = uptimeSeconds / (60 * 60 * 24);
        long hours = (uptimeSeconds % (60 * 60 * 24)) / (60 * 60);
        long minutes = (uptimeSeconds % (60 * 60)) / 60;
        long seconds = uptimeSeconds % 60;
        upTime[0] = days;
        upTime[1] = hours;
        upTime[2] = minutes;
        upTime[3] = seconds;
        return upTime;
    }

    public static String formatSize(long bytes) {
        if (-1000 < bytes && bytes < 1000) {
            return bytes + " B";
        }
        CharacterIterator ci = new StringCharacterIterator("kMGTPE");
        while (bytes <= -999950 || bytes >= 999950) {
            bytes /= 1000;
            ci.next();
        }
        return String.format(Locale.US, "%.1f %cB", bytes / 1000.0, ci.current());
    }

    private String getStatusString(int status) {
        String statusString = "Unknown";
        switch (status) {
            case BatteryManager.BATTERY_STATUS_CHARGING:
                statusString = "Charging";
                break;
            case BatteryManager.BATTERY_STATUS_DISCHARGING:
                statusString = "Discharging";
                break;
            case BatteryManager.BATTERY_STATUS_FULL:
                statusString = "Full";
                break;
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                statusString = "Not Charging";
                break;
        }
        return statusString;
    }
    @SuppressLint("PrivateApi")
    static int getBatteryCapacity(Context context) {
        double batteryCapacity = 0;
        final String POWER_PROFILE_CLASS = "com.android.internal.os.PowerProfile";
        try {
            @SuppressLint("PrivateApi") Object mPowerProfile = Class.forName(POWER_PROFILE_CLASS).getConstructor(Context.class).newInstance(context);
            batteryCapacity = (Double) Class.forName(POWER_PROFILE_CLASS).getMethod("getAveragePower", java.lang.String.class).invoke(mPowerProfile, "battery.capacity");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return (int) batteryCapacity;
    }

    private String getHealthString(int health) {
        String healthString = "Unknown";
        switch (health) {
            case BatteryManager.BATTERY_HEALTH_DEAD:
                healthString = "Dead";
                break;
            case BatteryManager.BATTERY_HEALTH_GOOD:
                healthString = "Good";
                break;
            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
                healthString = "Over Voltage";
                break;
            case BatteryManager.BATTERY_HEALTH_OVERHEAT:
                healthString = "Over Heat";
                break;
            case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
                healthString = "Unspecified Failure";
                break;
        }
        return healthString;
    }

    private String getPlugTypeString(int plugged) {
        String plugType = "Battery";
        switch (plugged) {
            case BatteryManager.BATTERY_PLUGGED_AC:
                plugType = "AC";
                break;
            case BatteryManager.BATTERY_PLUGGED_USB:
                plugType = "USB";
                break;
            case BatteryManager.BATTERY_PLUGGED_WIRELESS:
                plugType = "WIRELESS";
                break;
        }
        return plugType;
    }

    public boolean isExternalStorageAvailable() {
        try {
            return android.os.Environment.getExternalStorageState().equals(
                    android.os.Environment.MEDIA_MOUNTED);
        }catch (Exception e){
            return false;
        }
    }

    public String externalStorageAvailable() {
        if (isExternalStorageAvailable()) {
            return "Available";
        } else {
            return "Not Available";
        }
    }

    //static String GetFromBuildProp(String PropKey) {
    //    Process p;
    //    String propvalue = "";
    //    try {
    //        p = new ProcessBuilder("/system/bin/getprop", PropKey).redirectErrorStream(true).start();
    //        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
    //        String line;
    //        while ((line = br.readLine()) != null) {
    //            propvalue = line;
    //        }
    //        p.destroy();
    //    } catch (IOException e) {
    //        e.printStackTrace();
    //    }
    //    return propvalue;
    // }

     static String Get_FromBuildProp(String PropKey) {
              try {
                Process process = new ProcessBuilder()
                        .command("/system/bin/getprop")
                        .redirectErrorStream(true)
                        .start();
                InputStream is = process.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String line;
                StringBuilder str = new StringBuilder();

                while ((line = br.readLine()) != null) {
                    str.append(line).append("\n");
                    if (str.toString().contains(PropKey)) {
                        String[] split = line.split(":");
                        if (split.length == 2) {
                            return split[1];
                        }
                        break;
                    }
                }
                br.close();
                process.destroy();
            } catch (IOException e) {
                e.printStackTrace();
            }
         return "";
     }

  //=======::::::=======
         @Override
        public int[] argType () {
            return new int[0];
        }

        @Override
        public int priority () {
            return 1;
        }

        @Override
        public int helpRes () {
            return R.string.help_status;
        }

        @Override
        public String onArgNotFound (ExecutePack info,int index){
            return null;
        }

        @Override
        public String onNotArgEnough (ExecutePack info,int nArgs){
            return null;
        }

}