package bhupendra.ai.launcher.ui.status;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import java.util.Calendar;
import bhupendra.ai.launcher.R;
import bhupendra.ai.launcher.UIManager;
import bhupendra.ai.launcher.LabelUpdater;
import bhupendra.ai.launcher.managers.HTMLExtractManager;
import bhupendra.ai.launcher.managers.TextProcessor;
import bhupendra.ai.launcher.managers.TuiLocationManager;
import bhupendra.ai.launcher.managers.xml.XMLPrefsManager;
import bhupendra.ai.launcher.managers.xml.options.Behavior;
import bhupendra.ai.launcher.managers.xml.options.Theme;
import bhupendra.ai.launcher.managers.xml.options.Ui;
import bhupendra.ai.launcher.tuils.Tuils;

public class WeatherUIController {

    private final Context mContext;
    private final Handler handler;
    private final LabelUpdater labelUpdater;

    private int weatherDelay;
    private double lastLatitude, lastLongitude;
    private String location;
    private boolean fixedLocation = false;
    private boolean weatherPerformedStartupRun = false;
    private WeatherRunnable weatherRunnable;
    private int weatherColor;
    private boolean showWeatherUpdate;
    private BroadcastReceiver receiver;
    private boolean isRegistered = false;

    public WeatherUIController(Context context, Handler handler, LabelUpdater labelUpdater) {
        this.mContext = context;
        this.handler = handler;
        this.labelUpdater = labelUpdater;
    }

    public void init() {
        if (XMLPrefsManager.getBoolean(Ui.show_weather)) {
            weatherColor = XMLPrefsManager.getColor(Theme.weather_color);
            showWeatherUpdate = XMLPrefsManager.getBoolean(Behavior.show_weather_updates);
            weatherRunnable = new WeatherRunnable();
            registerReceiver();

            String where = XMLPrefsManager.get(Behavior.weather_location);
            if (where != null && (where.contains(",") || TextProcessor.isNumber(where))) {
                handler.post(weatherRunnable);
            }
        }
    }

    private void registerReceiver() {
        if (isRegistered) return;
        IntentFilter filter = new IntentFilter();
        filter.addAction(UIManager.ACTION_WEATHER);
        filter.addAction(UIManager.ACTION_WEATHER_GOT_LOCATION);
        filter.addAction(UIManager.ACTION_WEATHER_DELAY);
        filter.addAction(UIManager.ACTION_WEATHER_MANUAL_UPDATE);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action == null) return;

                if (action.equals(UIManager.ACTION_WEATHER)) {
                    Calendar c = Calendar.getInstance();
                    CharSequence s = intent.getCharSequenceExtra(XMLPrefsManager.VALUE_ATTRIBUTE);
                    if (s == null) s = intent.getStringExtra(XMLPrefsManager.VALUE_ATTRIBUTE);
                    if (s == null) return;

                    s = TextProcessor.span(context, s, weatherColor, labelUpdater.getLabelSize(UIManager.Label.weather));
                    labelUpdater.updateText(UIManager.Label.weather, s);

                    if (showWeatherUpdate) {
                        String message = context.getString(R.string.weather_updated) + Tuils.SPACE + c.get(Calendar.HOUR_OF_DAY) + "." + c.get(Calendar.MINUTE) + Tuils.SPACE + "(" + lastLatitude + ", " + lastLongitude + ")";
                        Tuils.sendOutput(context, message, bhupendra.ai.launcher.managers.TerminalManager.CATEGORY_OUTPUT);
                    }
                } else if (action.equals(UIManager.ACTION_WEATHER_GOT_LOCATION)) {
                    if (intent.getBooleanExtra(TuiLocationManager.FAIL, false)) {
                        if (weatherRunnable != null) {
                            handler.removeCallbacks(weatherRunnable);
                        }
                        weatherRunnable = null;

                        CharSequence s = TextProcessor.span(context, context.getString(R.string.location_error), weatherColor, labelUpdater.getLabelSize(UIManager.Label.weather));
                        labelUpdater.updateText(UIManager.Label.weather, s);
                    } else {
                        lastLatitude = intent.getDoubleExtra(TuiLocationManager.LATITUDE, 0);
                        lastLongitude = intent.getDoubleExtra(TuiLocationManager.LONGITUDE, 0);
                        location = Tuils.locationName(context, lastLatitude, lastLongitude);

                        if (!weatherPerformedStartupRun || XMLPrefsManager.wasChanged(Behavior.weather_key, false)) {
                            if (weatherRunnable != null) {
                                handler.removeCallbacks(weatherRunnable);
                            }
                            handler.post(weatherRunnable);
                        }
                    }
                } else if (action.equals(UIManager.ACTION_WEATHER_DELAY)) {
                    Calendar c = Calendar.getInstance();
                    c.setTimeInMillis(System.currentTimeMillis() + 1000 * 10);

                    if (showWeatherUpdate) {
                        String message = context.getString(R.string.weather_error) + Tuils.SPACE + c.get(Calendar.HOUR_OF_DAY) + "." + c.get(Calendar.MINUTE);
                        Tuils.sendOutput(context, message, bhupendra.ai.launcher.managers.TerminalManager.CATEGORY_OUTPUT);
                    }

                    if (weatherRunnable != null) {
                        handler.removeCallbacks(weatherRunnable);
                        handler.postDelayed(weatherRunnable, 1000 * 60);
                    }
                } else if (action.equals(UIManager.ACTION_WEATHER_MANUAL_UPDATE)) {
                    if (weatherRunnable != null) {
                        handler.removeCallbacks(weatherRunnable);
                        handler.post(weatherRunnable);
                    }
                }
            }
        };

        LocalBroadcastManager.getInstance(mContext.getApplicationContext()).registerReceiver(receiver, filter);
        isRegistered = true;
    }

    private void unregisterReceiver() {
        if (isRegistered && receiver != null) {
            LocalBroadcastManager.getInstance(mContext.getApplicationContext()).unregisterReceiver(receiver);
            isRegistered = false;
        }
    }

    public void pause() {
        if (handler != null && weatherRunnable != null) {
            handler.removeCallbacks(weatherRunnable);
        }
    }

    public void resume() {
        if (handler != null && weatherRunnable != null && XMLPrefsManager.getBoolean(Ui.show_weather)) {
            String where = XMLPrefsManager.get(Behavior.weather_location);
            if (where != null && (where.contains(",") || TextProcessor.isNumber(where))) {
                handler.removeCallbacks(weatherRunnable);
                handler.post(weatherRunnable);
            }
        }
    }

    public void dispose() {
        pause();
        unregisterReceiver();
    }

    public boolean applyWeatherVisibility(boolean visible) {
        if (!visible) {
            pause();
            labelUpdater.updateText(UIManager.Label.weather, Tuils.EMPTYSTRING);
            return true;
        }

        weatherColor = XMLPrefsManager.getColor(Theme.weather_color);
        showWeatherUpdate = XMLPrefsManager.getBoolean(Behavior.show_weather_updates);
        if (weatherRunnable == null) {
            weatherRunnable = new WeatherRunnable();
        }
        if (handler != null) {
            handler.removeCallbacks(weatherRunnable);
            registerReceiver();
            String where = XMLPrefsManager.get(Behavior.weather_location);
            if (where != null && (where.contains(",") || TextProcessor.isNumber(where))) {
                handler.post(weatherRunnable);
            }
        }
        return true;
    }

    private class WeatherRunnable implements Runnable {
        String key;
        String url;

        public WeatherRunnable() {
            if (XMLPrefsManager.wasChanged(Behavior.weather_key, false)) {
                weatherDelay = XMLPrefsManager.getInt(Behavior.weather_update_time);
                key = XMLPrefsManager.get(Behavior.weather_key);
            } else {
                key = Behavior.weather_key.defaultValue();
                weatherDelay = 60 * 60;
            }
            weatherDelay *= 1000;

            String where = XMLPrefsManager.get(Behavior.weather_location);
            if (where == null || where.length() == 0 || (!TextProcessor.isNumber(where) && !where.contains(","))) {
                TuiLocationManager l = TuiLocationManager.instance(mContext);
                l.add(UIManager.ACTION_WEATHER_GOT_LOCATION);
            } else {
                fixedLocation = true;
                if (where.contains(",")) {
                    String[] split = where.split(",");
                    where = "lat=" + split[0] + "&lon=" + split[1];
                } else {
                    where = "id=" + where;
                }
                setUrl(where);
            }
        }

        @Override
        public void run() {
            weatherPerformedStartupRun = true;
            if (!fixedLocation) {
                setUrl(lastLatitude, lastLongitude);
            }
            send();
            if (handler != null) {
                handler.postDelayed(this, weatherDelay);
            }
        }

        private void send() {
            if (url == null) return;
            Intent i = new Intent(HTMLExtractManager.ACTION_WEATHER);
            i.putExtra(XMLPrefsManager.VALUE_ATTRIBUTE, url);
            i.putExtra(HTMLExtractManager.BROADCAST_COUNT, HTMLExtractManager.broadcastCount);
            LocalBroadcastManager.getInstance(mContext.getApplicationContext()).sendBroadcast(i);
        }

        private void setUrl(String where) {
            url = "https://api.openweathermap.org/data/2.5/weather?" + where + "&appid=" + key + "&units=" + XMLPrefsManager.get(Behavior.weather_temperature_measure);
        }

        private void setUrl(double latitude, double longitude) {
            url = "https://api.openweathermap.org/data/2.5/weather?" + "lat=" + latitude + "&lon=" + longitude + "&appid=" + key + "&units=" + XMLPrefsManager.get(Behavior.weather_temperature_measure);
        }
    }
}
