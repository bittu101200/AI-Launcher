package bhupendra.ai.launcher.ui.views;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Vibrator;
import android.os.Bundle;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import bhupendra.ai.launcher.MainManager;
import bhupendra.ai.launcher.R;
import bhupendra.ai.launcher.managers.notifications.NotificationManager;
import bhupendra.ai.launcher.managers.notifications.NotificationService;
import bhupendra.ai.launcher.managers.xml.XMLPrefsManager;
import bhupendra.ai.launcher.managers.xml.options.Notifications;
import bhupendra.ai.launcher.tuils.PrivateIOReceiver;
import bhupendra.ai.launcher.tuils.Tuils;

/**
 * Created by francescoandreuzzi on 22/10/2017.
 */

public class LongClickableSpan extends ClickableSpan {

    public static int longPressVibrateDuration = -1;

    private Object clickO, longClickO;
    private String longIntentKey;

    private static boolean set = false, showMenu;
    private static boolean showExcludeApp, showExcludeNotification, showReply;

    public LongClickableSpan(Object clickAction, Object longClickAction) {
        this.clickO = clickAction;
        this.longClickO = longClickAction;
        this.longIntentKey = null;
    }

    public LongClickableSpan(Object clickAction) {
        this.clickO = clickAction;
        this.longClickO = null;
        this.longIntentKey = null;
    }

    public LongClickableSpan(Object clickAction, Object longClickAction, String longIntentKey) {
        this.clickO = clickAction;
        this.longClickO = longClickAction;
        this.longIntentKey = longIntentKey;
    }

    public LongClickableSpan(Object clickAction, String longIntentKey) {
        this.clickO = clickAction;
        this.longClickO = null;
        this.longIntentKey = longIntentKey;
    }

    public LongClickableSpan(String longIntentKey) {
        this.clickO = null;
        this.longClickO = null;
        this.longIntentKey = longIntentKey;
    }

    public void updateDrawState(TextPaint ds) {}

    @Override
    public void onClick(View widget) {
        execute(widget, clickO, false);
    }

    public void onLongClick(View widget) {
        if(execute(widget, longClickO, longIntentKey, true) && longPressVibrateDuration > 0) ((Vibrator) widget.getContext().getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE)).vibrate(longPressVibrateDuration);
    }

    private static boolean execute(View v, Object o, boolean isLongClick) {
        return execute(v, o, null, isLongClick);
    }

    private static void executeIntent(Context context, PendingIntent pi, String pkg) {
        if (pi == null && pkg == null) return;

        try {
            // Collapse status bar/system dialogs
            context.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
        } catch (Exception e) {}

        Bundle options = null;
        if (android.os.Build.VERSION.SDK_INT >= 34) { // Android 14+
            try {
                android.app.ActivityOptions actOptions = android.app.ActivityOptions.makeBasic();
                actOptions.setPendingIntentBackgroundActivityStartMode(android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED);
                options = actOptions.toBundle();
            } catch (Exception e) {
                Tuils.log("ActivityOptions error", e);
            }
        }

        boolean success = false;
        if (pi != null) {
            try {
                if (options != null) {
                    pi.send(context, 0, null, null, null, null, options);
                } else {
                    pi.send();
                }
                success = true;
            } catch (Exception e) {
                Tuils.log("PendingIntent.send failed", e);
            }
        }

        // Fallback: If PI failed or is null, try to launch the app by package
        if (!success && pkg != null) {
            try {
                Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(pkg);
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(launchIntent, options);
                    success = true;
                }
            } catch (Exception e) {
                Tuils.log("Fallback launch failed", e);
            }
        }

        if (!success) {
            Toast.makeText(context, "Could not open app", Toast.LENGTH_SHORT).show();
        }
    }

    private static boolean execute(final View v, Object o, String intentKey, boolean isLongClick) {
        if(o == null) return false;

        if(!set) {
            set = true;

            showExcludeApp = XMLPrefsManager.getBoolean(Notifications.notification_popup_exclude_app);
            showExcludeNotification = XMLPrefsManager.getBoolean(Notifications.notification_popup_exclude_notification);
            showReply = XMLPrefsManager.getBoolean(Notifications.notification_popup_reply);

            showMenu = (showExcludeApp && showExcludeNotification) || (showExcludeApp && showReply) || (showExcludeNotification && showReply);
        }

        if(o instanceof String) {
            Intent intent = new Intent(intentKey != null ? intentKey : MainManager.ACTION_EXEC);
            intent.putExtra(PrivateIOReceiver.TEXT, (String) o);

            if(intentKey == null || intentKey.equals(MainManager.ACTION_EXEC)) {
                intent.putExtra(MainManager.NEED_WRITE_INPUT, false);
                intent.putExtra(MainManager.CMD_COUNT, MainManager.commandCount);
            }

            LocalBroadcastManager.getInstance(v.getContext().getApplicationContext()).sendBroadcast(intent);
        } else if(o instanceof PendingIntent) {
            executeIntent(v.getContext(), (PendingIntent) o, null);
        } else if(o instanceof Uri) {
            Intent i = new Intent(Intent.ACTION_VIEW, (Uri) o);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            try {
                v.getContext().startActivity(i);
            } catch (Exception e) {
                Tuils.sendOutput(Color.RED, v.getContext(), e.toString());
            }
        } else if(o instanceof NotificationService.Notification) {
            final NotificationService.Notification n = (NotificationService.Notification) o;

            if (isLongClick) {
                int count = 0;
                if(n.pendingIntent != null) count++;
                if(showExcludeApp) count++;
                if(showExcludeNotification) count++;
                if(showReply) count++;

                boolean forceMenu = count > 1;

                if(showMenu || forceMenu) {
                    PopupMenu menu = new PopupMenu(v.getContext().getApplicationContext(), v);
                    menu.getMenuInflater().inflate(R.menu.notification_menu, menu.getMenu());

                    menu.getMenu().findItem(R.id.open_notification).setVisible(n.pendingIntent != null);
                    menu.getMenu().findItem(R.id.exclude_app).setVisible(showExcludeApp);
                    menu.getMenu().findItem(R.id.exclude_notification).setVisible(showExcludeNotification);
                    menu.getMenu().findItem(R.id.reply_notification).setVisible(showReply);

                        menu.setOnMenuItemClickListener(item -> {
                            int id = item.getItemId();

                            if (id == R.id.open_notification) {
                                executeIntent(v.getContext(), n.pendingIntent, n.pkg);
                            } else if (id == R.id.exclude_app) {
                                NotificationManager.setState(n.pkg, false);
                            } else if (id == R.id.exclude_notification) {
                                Tuils.log(n.text);
                                NotificationManager.addFilter(n.text, -1);
                            } else if (id == R.id.reply_notification) {
                                Intent intent = new Intent(PrivateIOReceiver.ACTION_INPUT);
                                intent.putExtra(PrivateIOReceiver.TEXT, "reply -to " + n.pkg + Tuils.SPACE);

                                LocalBroadcastManager.getInstance(v.getContext().getApplicationContext()).sendBroadcast(intent);
                            } else {
                                return false;
                            }

                            return true;
                        });

                        menu.show();
                    } else {
                        if (showReply) {
                            Intent intent = new Intent(PrivateIOReceiver.ACTION_INPUT);
                            intent.putExtra(PrivateIOReceiver.TEXT, "reply -to " + n.pkg + Tuils.SPACE);

                            LocalBroadcastManager.getInstance(v.getContext().getApplicationContext()).sendBroadcast(intent);
                        } else if (showExcludeNotification) {
                            NotificationManager.addFilter(n.text, -1);
                        } else if (showExcludeApp) {
                            NotificationManager.setState(n.pkg, false);
                        } else {
                            executeIntent(v.getContext(), n.pendingIntent, n.pkg);
                        }
                }
            } else {
                executeIntent(v.getContext(), n.pendingIntent, n.pkg);
            }
        }

        return true;
    }
}
