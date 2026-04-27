package bhupendra.ai.launcher.tuils;

import bhupendra.ai.launcher.managers.FileSystemManager;


import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.Parcelable;
import android.os.Process;
import android.os.StatFs;
import android.provider.Settings;
import androidx.core.content.FileProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.telephony.TelephonyManager;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.w3c.dom.Node;
import org.xml.sax.SAXParseException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import dalvik.system.DexFile;
import bhupendra.ai.launcher.BuildConfig;
import bhupendra.ai.launcher.R;
import bhupendra.ai.launcher.commands.main.MainPack;
import bhupendra.ai.launcher.managers.TerminalManager;
import bhupendra.ai.launcher.terminal.TerminalEventBus;
import bhupendra.ai.launcher.managers.music.MusicManager2;
import bhupendra.ai.launcher.managers.music.Song;
import bhupendra.ai.launcher.managers.notifications.NotificationService;
import bhupendra.ai.launcher.managers.xml.XMLPrefsManager;
import bhupendra.ai.launcher.managers.xml.classes.XMLPrefsSave;
import bhupendra.ai.launcher.managers.xml.options.Behavior;
import bhupendra.ai.launcher.managers.xml.options.Ui;
import bhupendra.ai.launcher.tuils.interfaces.OnBatteryUpdate;
import bhupendra.ai.launcher.tuils.stuff.FakeLauncherActivity;

public class Tuils {

    public static final String SPACE = " ";
    public static final String DOUBLE_SPACE = "  ";
    public static final String NEWLINE = "\n";
    public static final String TRIBLE_SPACE = "   ";
    public static final String DOT = ".";
    public static final String EMPTYSTRING = "";
    public static final String MINUS = "-";

    public static Pattern patternNewline = Pattern.compile("%n", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);

    private static Typeface globalTypeface = null;
    public static String fontPath = null;

    


    public static Typeface getTypeface(Context context) {
        if(globalTypeface == null) {
            try {
                XMLPrefsManager.loadCommons(context);
            } catch (Exception e) {
                return null;
            }

            boolean systemFont = XMLPrefsManager.getBoolean(Ui.system_font);
            if(systemFont) globalTypeface = Typeface.DEFAULT;
            else {
                File tui = FileSystemManager.getFolder();
                if(tui == null) {
                    return Typeface.createFromAsset(context.getAssets(), "lucida_console.ttf");
                }

                Pattern p = Pattern.compile(".[ot]tf$");

                File font = null;
                for(File f : tui.listFiles()) {
                    String name = f.getName();
                    if(p.matcher(name).find()) {
                        font = f;
                        fontPath = f.getAbsolutePath();
                        break;
                    }
                }

                if(font != null) {
                    try {
                        globalTypeface = Typeface.createFromFile(font);
                        if(globalTypeface == null) throw new UnsupportedOperationException();
                    } catch (Exception e) {
                        globalTypeface = null;
                    }
                }
            }

            if(globalTypeface == null) globalTypeface = systemFont ? Typeface.DEFAULT : Typeface.createFromAsset(context.getAssets(), "lucida_console.ttf");
        }
        return globalTypeface;
    }

    public static void cancelFont() {
        globalTypeface = null;
        fontPath = null;
    }

    public static String locationName(Context context, double lat, double lng) {
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        List<Address> addresses = null;
        try {
            addresses = geocoder.getFromLocation(lat, lng, 1);
            return addresses.get(0).getAddressLine(2);
        } catch (Exception e) {
            return null;
        }
    }



    public static boolean arrayContains(int[] array, int value) {
        if(array == null) return false;

        for(int i : array) {
            if(i == value) {
                return true;
            }
        }
        return false;
    }



    
    









    public static String convertStreamToString(java.io.InputStream is) {
        if (is == null) return Tuils.EMPTYSTRING;

        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : Tuils.EMPTYSTRING;
    }





    public static float dpToPx(Context context, float valueInDp) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, valueInDp, metrics);
    }









    public static Intent webPage(String url) {
        return new Intent(Intent.ACTION_VIEW, Uri.parse(url));
    }













    public static double percentage(double part, double total) {
        return round(part * 100 / total, 2);
    }

    public static double formatSize(long bytes, int unit) {
        double convert = 1048576.0;
        double smallConvert = 1024.0;

        double result;

        switch (unit) {
            case TERA:
                result = (bytes / convert) / convert;
                break;
            case GIGA:
                result = (bytes / convert) / smallConvert;
                break;
            case MEGA:
                result = bytes / convert;
                break;
            case KILO:
                result = bytes / smallConvert;
                break;
            case BYTE:
                result = bytes;
                break;
            default: return -1;
        }

        return round(result, 2);
    }
















    public static int convertSpToPixels(float sp, Context context) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, context.getResources().getDisplayMetrics());
    }



//    static final int WEATHER_TIMEOUT = 6000;
//    public static boolean location(Context context, final ArgsRunnable whenFound, final Runnable notFound, final Handler handler) {
//        final LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
//        if(locationManager == null) return false;
//
//        final LocationListener locationListener = new LocationListener() {
//            @Override
//            public void onLocationChanged(Location location) {
//                whenFound.run(location.getLatitude(), location.getLongitude());
//            }
//
//            @Override
//            public void onStatusChanged(String provider, int status, Bundle extras) {
//            }
//
//            @Override
//            public void onProviderEnabled(String provider) {
//            }
//
//            @Override
//            public void onProviderDisabled(String provider) {
//            }
//        };
//
//        boolean gpsProvider = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
//        boolean networkProvider = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
//        boolean passiveProvider = locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER);
//
//        if (!gpsStatus && !networkStatus) return false;
//
//        try {
//            locationManager.requestSingleUpdate(gpsStatus ? LocationManager.GPS_PROVIDER : LocationManager.NETWORK_PROVIDER, locationListener, Looper.getMainLooper());
//        } catch (SecurityException e) {
//            Tuils.log(e);
//            FileSystemManager.toFile(e);
//            return false;
//        }
//
//        Location location;
//        try {
//            Location[] ls = {
//                    locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER),
//                    locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER),
//                    locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)};
//
//            location = ls[0];
//            for(int c = 1; c < ls.length; c++) {
//                if(location == null) location = ls[c];
//                else if(ls[c] != null && location.getTime() < ls[c].getTime()) location = ls[c];
//            }
//        } catch (SecurityException e) {
//            FileSystemManager.toFile(e);
//            return false;
//        }
//
//        if(handler != null) {
//            handler.postDelayed(notFound, WEATHER_TIMEOUT);
//            handler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    if(locationManager != null) locationManager.removeUpdates(locationListener);
//                }
//            }, WEATHER_TIMEOUT);
//        }
//
//        return true;
//    }

//    public static Location getLocation(Context context) {
//        final LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
//        if(locationManager == null) return null;
//
//        Location location;
//        try {
//            Location[] ls = {
//                    locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER),
//                    locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER),
//                    locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)};
//
//            location = ls[0];
//            for(int c = 1; c < ls.length; c++) {
//                if(location == null) location = ls[c];
//                else if(ls[c] != null && location.getTime() < ls[c].getTime()) location = ls[c];
//            }
//
//            return location;
//        } catch (SecurityException e) {
//            FileSystemManager.toFile(e);
//            return null;
//        }
//    }

    public abstract static class ArgsRunnable implements Runnable {
        private Object[] args;

        public void setArgs(Object... args) {
            this.args = args;
        }

        public void run(Object... args) {
            setArgs(args);
            run();
        }

        public <T> T get(Class<T> c, int index) {
            if(index < args.length) return (T) args[index];
            return null;
        }
    }









    public static void deepView(View v) {
        Tuils.log(v.toString());

        if(!(v instanceof ViewGroup)) return;
        ViewGroup g = (ViewGroup) v;

        Tuils.log(g.getChildCount());
        for(int c = 0; c < g.getChildCount(); c++) deepView(g.getChildAt(c));

        Tuils.log("end of parents of: " + v.toString());
    }

    private static View.OnClickListener deepClickListener = v -> Tuils.log(v.toString());

    public static void deepClickView(View v) {
        v.setOnClickListener(deepClickListener);

        if(!(v instanceof ViewGroup)) return;
        ViewGroup g = (ViewGroup) v;

        for(int c = 0; c < g.getChildCount(); c++) deepClickView(g.getChildAt(c));
    }

    public static void scaleImage(ImageView view, int newX, int newY) throws NoSuchElementException {
        // Get bitmap from the the ImageView.
        Bitmap bitmap = null;

        try {
            Drawable drawing = view.getDrawable();
            bitmap = ((BitmapDrawable) drawing).getBitmap();
        } catch (NullPointerException e) {
            throw new NoSuchElementException("No drawable on given view");
        }

        // Get current dimensions AND the desired bounding box
        int width = 0;

        try {
            width = bitmap.getWidth();
        } catch (NullPointerException e) {
            throw new NoSuchElementException("Can't find bitmap on given view/drawable");
        }

        int height = bitmap.getHeight();
        int xBounding = dpToPx(view.getContext(), newX);
        int yBounding = dpToPx(view.getContext(), newY);

        // Determine how much to scale: the dimension requiring less scaling is
        // closer to the its side. This way the image always stays inside your
        // bounding box AND either x/y axis touches it.
        float xScale = ((float) xBounding) / width;
        float yScale = ((float) yBounding) / height;
        float scale = (xScale <= yScale) ? xScale : yScale;

        // Create a matrix for the scaling and add the scaling data
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);

        // Create a new bitmap and convert it to a format understood by the ImageView
        Bitmap scaledBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
        width = scaledBitmap.getWidth(); // re-use
        height = scaledBitmap.getHeight(); // re-use
        BitmapDrawable result = new BitmapDrawable(scaledBitmap);

        // Apply the scaled bitmap
        view.setImageDrawable(result);

        // Now change ImageView's dimensions to match the scaled image
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) view.getLayoutParams();
        params.width = width;
        params.height = height;
        view.setLayoutParams(params);
    }

    public static int dpToPx(Context context, int dp) {
        float density = context.getApplicationContext().getResources().getDisplayMetrics().density;
        return Math.round((float)dp * density);
    }

    public static void sendOutput(Context context, int res) {
        sendOutput(Integer.MAX_VALUE, context, res);
    }

    public static void sendOutput(int color, Context context, int res) {
        sendOutput(color, context, context.getString(res));
    }

    public static void sendOutput(Context context, int res, int type) {
        sendOutput(Integer.MAX_VALUE, context, res, type);
    }

    public static void sendOutput(int color, Context context, int res, int type) {
        sendOutput(color, context, context.getString(res), type);
    }

    public static void sendOutput(Context context, CharSequence s) {
        sendOutput(Integer.MAX_VALUE, context, s);
    }

    public static void sendOutput(int color, Context context, CharSequence s) {
        sendOutput(color, context, s, TerminalManager.CATEGORY_OUTPUT);
    }

    public static void sendOutput(Context context, CharSequence s, int type) {
        sendOutput(Integer.MAX_VALUE, context, s, type);
    }

    public static void sendOutput(int color, Context context, CharSequence s, int type) {
        if (shouldSuppressOutput(s)) return;
        android.util.Log.i("AI_OUTPUT", s.toString());
        if (TerminalEventBus.get().post(new TerminalEventBus.OutputEvent(s, color, type, null, null, null))) {
            return;
        }
        Intent intent = new Intent(PrivateIOReceiver.ACTION_OUTPUT);
        intent.putExtra(PrivateIOReceiver.TEXT, s);
        intent.putExtra(PrivateIOReceiver.COLOR, color);
        intent.putExtra(PrivateIOReceiver.TYPE, type);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public static void sendOutput(MainPack mainPack, CharSequence s, int type) {
        sendOutput(mainPack.commandColor, mainPack.context, s, type);
    }

    public static void sendOutput(Context context, CharSequence s, int type, Object action) {
        sendOutput(Integer.MAX_VALUE, context, s, type, action);
    }

    public static void sendOutput(int color, Context context, CharSequence s, int type, Object action) {
        if (shouldSuppressOutput(s)) return;
        android.util.Log.i("AI_OUTPUT", s.toString());
        if (TerminalEventBus.get().post(new TerminalEventBus.OutputEvent(s, color, type, action, null, null))) {
            return;
        }
        Intent intent = new Intent(PrivateIOReceiver.ACTION_OUTPUT);
        intent.putExtra(PrivateIOReceiver.TEXT, s);
        intent.putExtra(PrivateIOReceiver.COLOR, color);
        intent.putExtra(PrivateIOReceiver.TYPE, type);

        if(action instanceof String) intent.putExtra(PrivateIOReceiver.ACTION, (String) action);
        else if(action instanceof Parcelable) intent.putExtra(PrivateIOReceiver.ACTION, (Parcelable) action);

        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public static void sendOutput(Context context, CharSequence s, int type, Object action, Object longAction) {
        sendOutput(Integer.MAX_VALUE, context, s, type, action, longAction);
    }

    public static void sendOutput(int color, Context context, CharSequence s, int type, Object action, Object longAction) {
        if (shouldSuppressOutput(s)) return;
        android.util.Log.i("AI_OUTPUT", s.toString());
        if (TerminalEventBus.get().post(new TerminalEventBus.OutputEvent(s, color, type, action, longAction, null))) {
            return;
        }
        Intent intent = new Intent(PrivateIOReceiver.ACTION_OUTPUT);
        intent.putExtra(PrivateIOReceiver.TEXT, s);
        intent.putExtra(PrivateIOReceiver.COLOR, color);
        intent.putExtra(PrivateIOReceiver.TYPE, type);

        if(action instanceof String) intent.putExtra(PrivateIOReceiver.ACTION, (String) action);
        else if(action instanceof Parcelable) intent.putExtra(PrivateIOReceiver.ACTION, (Parcelable) action);

        if(longAction instanceof String) intent.putExtra(PrivateIOReceiver.LONG_ACTION, (String) longAction);
        else if(longAction instanceof Parcelable) intent.putExtra(PrivateIOReceiver.LONG_ACTION, (Parcelable) longAction);

        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    private static boolean shouldSuppressOutput(CharSequence s) {
        if (s == null) return true;
        String text = s.toString().trim();
        return text.length() == 0 || "null".equalsIgnoreCase(text);
    }

    public static void sendInput(Context context, String text) {
        if (TerminalEventBus.get().post(new TerminalEventBus.InputEvent(text, null))) {
            return;
        }
        Intent intent = new Intent(PrivateIOReceiver.ACTION_INPUT);
        intent.putExtra(PrivateIOReceiver.TEXT, text);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public static final int TERA = 0;
    public static final int GIGA = 1;
    public static final int MEGA = 2;
    public static final int KILO = 3;
    public static final int BYTE = 4;

    





    public static double round(double value, int places) {
        if (places < 0) places = 0;

        try {
            BigDecimal bd = new BigDecimal(value);
            bd = bd.setScale(places, RoundingMode.HALF_UP);
            return bd.doubleValue();
        } catch (Exception e) {
            return value;
        }
    }

    public static List<String> getClassesInPackage(String packageName, Context c) throws IOException {
        List<String> classes = new ArrayList<>();
        String packageCodePath = c.getPackageCodePath();
        DexFile df = new DexFile(packageCodePath);
        for (Enumeration<String> iter = df.entries(); iter.hasMoreElements(); ) {
            String className = iter.nextElement();
            if (className.contains(packageName) && !className.contains("$")) {
                classes.add(className.substring(className.lastIndexOf(".") + 1, className.length()));
            }
        }

        return classes;
    }

    public static int scale(int[] from, int[] to, int n) {
        return (to[1] - to[0])*(n - from[0])/(from[1] - from[0]) + to[0];
    }



    private static String getNicePath(String filePath) {
        if(filePath == null) return "null";

        String home = XMLPrefsManager.get(File.class, Behavior.home_path).getAbsolutePath();

        if(filePath.equals(home)) {
            return "~";
        } else if(filePath.startsWith(home)) {
            return "~" + filePath.replace(home, Tuils.EMPTYSTRING);
        } else {
            return filePath;
        }
    }

    public static int find(Object o, Object[] array) {
        return find(o, Arrays.asList(array));
    }

    public static int find(Object o, List list) {
        for(int count = 0; count < list.size(); count++) {
            Object x = list.get(count);
            if(x == null) continue;

            if(o == x) return count;

            if (o instanceof XMLPrefsSave) {
                try {
                    if (((XMLPrefsSave) o).label().equals((String) x)) return count;
                } catch (Exception e) {}
            }

            if (o instanceof String && x instanceof XMLPrefsSave) {
                try {
                    if (((XMLPrefsSave) x).label().equals((String) o)) return count;
                } catch (Exception e) {}
            }

            try {
                if (o.equals(x) || x.equals(o)) return count;
            } catch (Exception e) {
                continue;
            }
        }
        return -1;
    }

    static Pattern pd = Pattern.compile("%d", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
    static Pattern pu = Pattern.compile("%u", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
    static Pattern pp = Pattern.compile("%p", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
    public static String getHint(String currentPath) {
        if(!XMLPrefsManager.getBoolean(Ui.show_session_info)) return null;

        String format = XMLPrefsManager.get(Behavior.session_info_format);
        if(format.length() == 0) return null;

        String deviceName = XMLPrefsManager.get(Ui.deviceName);
        if(deviceName == null || deviceName.length() == 0) {
            deviceName = Build.DEVICE;
        }

        String username = XMLPrefsManager.get(Ui.username);
        if(username == null) username = Tuils.EMPTYSTRING;

        format = pd.matcher(format).replaceAll(Matcher.quoteReplacement(deviceName));
        format = pu.matcher(format).replaceAll(Matcher.quoteReplacement(username));
        format = pp.matcher(format).replaceAll(Matcher.quoteReplacement(Tuils.getNicePath(currentPath)));

        return format;
    }

    public static int findPrefix(List<String> list, String prefix) {
        for (int count = 0; count < list.size(); count++)
            if (list.get(count).startsWith(prefix))
                return count;
        return -1;
    }

    public static int mmToPx(DisplayMetrics metrics, int mm) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, mm, metrics);
    }

    public static void insertHeaders(List<String> s, boolean newLine) {
        char current = 0;
        for (int count = 0; count < s.size(); count++) {
            String st = s.get(count).trim().toUpperCase();
            if(st.length() < 0) continue;

            char c = st.charAt(0);
            if (current != c) {
                s.add(count, (newLine ? NEWLINE : EMPTYSTRING) + c + (newLine ? NEWLINE : EMPTYSTRING));
                current = c;
            }
        }
    }











    public static String nodeToString(Node node) {
        try {
            TransformerFactory transfac = TransformerFactory.newInstance();
            Transformer trans = transfac.newTransformer();
            trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            trans.setOutputProperty(OutputKeys.INDENT, "yes");
            StringWriter sw = new StringWriter();
            StreamResult result = new StreamResult(sw);
            DOMSource source = new DOMSource(node);
            trans.transform(source, result);

            return sw.toString();
        }
        catch (TransformerException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void log(Object o) {
//        Log.e("andre", Arrays.toString(Thread.currentThread().getStackTrace()));

        if(o instanceof Throwable) {
            Log.e("andre", "", (Throwable) o);
        } else {
            String text;
            if(o instanceof Object[]) text = Arrays.toString((Object[]) o);
            else text = o.toString();
            Log.e("andre", text);
        }
    }

    public static void log(Object o, Object o2) {
        if(o instanceof Object[] && o2 instanceof Object[]){
            Log.e("andre", Arrays.toString((Object[]) o) + " -- " + Arrays.toString((Object[]) o2));
        } else {
            Log.e("andre", String.valueOf(o) + " -- " + String.valueOf(o2));
        }
    }

    public static void log(Object o, PrintStream to) {
//        Log.e("andre", Arrays.toString(Thread.currentThread().getStackTrace()));

        if(o instanceof Throwable) {
            ((Throwable) o).printStackTrace(to);
        } else {
            String text;
            if(o instanceof Object[]) text = Arrays.toString((Object[]) o);
            else text = o.toString();

            try {
                to.write(text.getBytes());
            } catch (IOException e) {
                Tuils.log(e);
            }
        }
    }

    public static void log(Object o, Object o2, OutputStream to) {
        try {
            if(o instanceof Object[] && o2 instanceof Object[]){
                to.write((Arrays.toString((Object[]) o) + " -- " + Arrays.toString((Object[]) o2)).getBytes());
            } else {
                to.write((String.valueOf(o) + " -- " + String.valueOf(o2)).getBytes());
            }
        } catch (Exception e) {
            Tuils.log(e);
        }
    }



    public static <T> T getDefaultValue(Class<T> clazz) {
        return (T) Array.get(Array.newInstance(clazz, 1), 0);
    }













    


    public static String getStackTrace(final Throwable throwable) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        throwable.printStackTrace(pw);
        return sw.getBuffer().toString();
    }





//    return 0 if only digit












    public static String getTextFromClipboard(Context context) {
        try {
            ClipboardManager manager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData.Item item = manager.getPrimaryClip().getItemAt(0);
            return item.getText().toString();
        } catch (Exception e) {
            return null;
        }
    }

    public static int dpToPx(Resources resources, int dp) {
        DisplayMetrics displayMetrics = resources.getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    private static final int FILEUPDATE_DELAY = 100;






    




    public static void setCursorDrawableColor(EditText editText, int color) {
        try {
            Field fCursorDrawableRes = TextView.class.getDeclaredField("mCursorDrawableRes");
            fCursorDrawableRes.setAccessible(true);
            int mCursorDrawableRes = fCursorDrawableRes.getInt(editText);
            Field fEditor = TextView.class.getDeclaredField("mEditor");
            fEditor.setAccessible(true);
            Object editor = fEditor.get(editText);
            Class<?> clazz = editor.getClass();
            Field fCursorDrawable = clazz.getDeclaredField("mCursorDrawable");
            fCursorDrawable.setAccessible(true);
            Drawable[] drawables = new Drawable[2];
            drawables[0] = editText.getContext().getResources().getDrawable(mCursorDrawableRes);
            drawables[1] = editText.getContext().getResources().getDrawable(mCursorDrawableRes);
            drawables[0].setColorFilter(color, PorterDuff.Mode.SRC_IN);
            drawables[1].setColorFilter(color, PorterDuff.Mode.SRC_IN);
            fCursorDrawable.set(editor, drawables);
        } catch (Throwable ignored) {}
    }

    public static int pendingIntentFlags(int flags) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if ((flags & android.app.PendingIntent.FLAG_MUTABLE) != 0) {
                return flags;
            }
            return flags | android.app.PendingIntent.FLAG_IMMUTABLE;
        }
        return flags;
    }



    public static void sendXMLParseError(Context context, String PATH, SAXParseException e) {
        Tuils.sendOutput(
                Color.RED,
                context, context.getString(R.string.output_xmlproblem1) + Tuils.SPACE + PATH + context.getString(R.string.output_xmlproblem2) + Tuils.NEWLINE + context.getString(R.string.output_errorlabel) +
                "File: " + e.getSystemId() + Tuils.NEWLINE +
                "Message" + e.getMessage() + Tuils.NEWLINE +
                "Line" + e.getLineNumber() + Tuils.NEWLINE +
                "Column" + e.getColumnNumber());
    }

    public static void sendXMLParseError(Context context, String PATH) {
        Tuils.sendOutput(Color.RED, context, context.getString(R.string.output_xmlproblem1) + Tuils.SPACE + PATH + context.getString(R.string.output_xmlproblem2));
    }



    private interface RegexAction {
        void apply(SpannableStringBuilder builder, int start, int end);
    }

    private static void applyRegex(SpannableStringBuilder ssb, String pattern, RegexAction action) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(ssb.toString());
        int offset = 0;
        while (m.find()) {
            int start = m.start() - offset;
            int end = m.end() - offset;
            int oldLen = ssb.length();
            action.apply(ssb, start, end);
            offset += (oldLen - ssb.length());
        }
    }
}
