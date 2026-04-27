package bhupendra.ai.launcher.managers.notifications.reply;

import bhupendra.ai.launcher.managers.FileSystemManager;


import android.annotation.TargetApi;
import android.app.Notification;
import android.app.RemoteInput;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import bhupendra.ai.launcher.BuildConfig;
import bhupendra.ai.launcher.R;
import bhupendra.ai.launcher.managers.xml.XMLPrefsManager;
import bhupendra.ai.launcher.managers.xml.classes.XMLPrefsElement;
import bhupendra.ai.launcher.managers.xml.classes.XMLPrefsList;
import bhupendra.ai.launcher.managers.xml.classes.XMLPrefsSave;
import bhupendra.ai.launcher.managers.xml.options.Reply;
import bhupendra.ai.launcher.tuils.PrivateIOReceiver;
import bhupendra.ai.launcher.tuils.Tuils;

import static bhupendra.ai.launcher.managers.xml.XMLPrefsManager.VALUE_ATTRIBUTE;
import static bhupendra.ai.launcher.managers.xml.XMLPrefsManager.set;

public class ReplyManager implements XMLPrefsElement {

    public static String PATH = "reply.xml";
    public static String NAME = "REPLY";
    public static String ACTION = BuildConfig.APPLICATION_ID + ".reply";
    public static String ID = "id";
    public static String WHAT = "what";
    public static String ACTION_UPDATE = BuildConfig.APPLICATION_ID + ".update";

    // Map: PackageName -> last seen NotificationWear
    private Map<String, NotificationWear> notificationWears;

    private BroadcastReceiver receiver;

    public static ReplyManager instance;
    private XMLPrefsList values;

    private boolean enabled;

    private Context context;

    @Override
    public String path() {
        return PATH;
    }

    public ReplyManager(Context context) {
        notificationWears = new HashMap<>();
        values = new XMLPrefsList();
        this.context = context;

        instance = this;

        load();

        enabled = Boolean.parseBoolean(values.get(Reply.reply_enabled).value);
        if(!enabled) {
            notificationWears = null;
        } else {
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION);
            filter.addAction(ACTION_UPDATE);

            receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if(intent.getAction().equals(ACTION)) {
                        String pkg = intent.getStringExtra(ID);
                        String what = intent.getStringExtra(WHAT);

                        if(what == null) {
                            check(pkg);
                        } else {
                            replyTo(ReplyManager.this.context, pkg, what);
                        }
                    } else if(intent.getAction().equals(ACTION_UPDATE)) {
                        load();
                    }
                }
            };

            LocalBroadcastManager.getInstance(context.getApplicationContext()).registerReceiver(receiver, filter);
        }
    }

    private void load() {
        List<Reply> enums = new ArrayList<>(Arrays.asList(Reply.values()));

        File file = new File(FileSystemManager.getFolder(), PATH);

        Object[] o;
        try {
            o = XMLPrefsManager.buildDocument(file, NAME);
            if(o == null) {
                return;
            }
        } catch (Exception e) {
            Tuils.log(e);
            return;
        }

        org.w3c.dom.Document d = (org.w3c.dom.Document) o[0];
        org.w3c.dom.Element root = (org.w3c.dom.Element) o[1];

        org.w3c.dom.NodeList nodes = root.getElementsByTagName("*");

        try {
            for (int count = 0; count < nodes.getLength(); count++) {
                final org.w3c.dom.Node node = nodes.item(count);
                String nn = node.getNodeName();

                if (Tuils.find(nn, enums) != -1) {
                    values.add(nn, node.getAttributes().getNamedItem(VALUE_ATTRIBUTE).getNodeValue());

                    for (int en = 0; en < enums.size(); en++) {
                        if (enums.get(en).label().equals(nn)) {
                            enums.remove(en);
                            break;
                        }
                    }
                }
            }

            if (enums.size() > 0) {
                for (XMLPrefsSave s : enums) {
                    String value = s.defaultValue();

                    org.w3c.dom.Element em = d.createElement(s.label());
                    em.setAttribute(VALUE_ATTRIBUTE, value);
                    root.appendChild(em);

                    values.add(s.label(), value);
                }

                XMLPrefsManager.writeTo(d, file);
            }
        } catch (Exception e) {
            Tuils.log(e);
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void onNotification(StatusBarNotification notification, CharSequence text, String title) {
        if(!enabled) return;

        String pkg = notification.getPackageName();
        NotificationWear w = extractWearNotification(notification);
        if(w == null) return;

        notificationWears.put(pkg, w);
    }

    public void replyTo(Context context, String pkg, String what) {
        if(!enabled || pkg == null) return;

        NotificationWear wear = notificationWears.get(pkg);
        if(wear == null) {
            wear = findActiveNotification(pkg);
        }

        if(wear != null) replyTo(context, wear, what);
        else Tuils.sendOutput(context, context.getString(R.string.reply_app_not_found) + Tuils.SPACE + pkg);
    }

    private NotificationWear findActiveNotification(String pkg) {
        bhupendra.ai.launcher.managers.notifications.NotificationService service = 
            bhupendra.ai.launcher.managers.notifications.NotificationService.instance;
        if (service == null) return null;

        StatusBarNotification[] sbns = service.getActiveNotifications();
        if (sbns == null) return null;

        for (StatusBarNotification sbn : sbns) {
            if (sbn.getPackageName().equals(pkg)) {
                NotificationWear w = extractWearNotification(sbn);
                if (w != null) {
                    return w;
                }
            }
        }
        return null;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
    private void replyTo(Context context, NotificationWear notificationWear, String what) {
        RemoteInput[] remoteInputs = notificationWear.remoteInputs;

        Bundle localBundle = notificationWear.bundle;

        Intent i = new Intent(PrivateIOReceiver.ACTION_REPLY);
        i.putExtra(PrivateIOReceiver.BUNDLE, localBundle);
        i.putExtra(PrivateIOReceiver.REMOTE_INPUTS, remoteInputs);
        i.putExtra(PrivateIOReceiver.TEXT, what);
        i.putExtra(PrivateIOReceiver.PENDING_INTENT, notificationWear.pendingIntent);
        i.putExtra(PrivateIOReceiver.ID, notificationWear.id);

        LocalBroadcastManager.getInstance(context.getApplicationContext()).sendBroadcast(i);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
    private NotificationWear extractWearNotification(StatusBarNotification statusBarNotification) {
        NotificationWear notificationWear = new NotificationWear();

        Notification.WearableExtender wearableExtender = new Notification.WearableExtender(statusBarNotification.getNotification());
        for(Notification.Action action : wearableExtender.getActions()) {
            RemoteInput[] rs = action.getRemoteInputs();
            if(rs != null && rs.length > 0) {
                notificationWear.remoteInputs = rs;
                notificationWear.pendingIntent = action.actionIntent;
                break;
            }
        }

        if (notificationWear.pendingIntent == null) {
            Notification n = statusBarNotification.getNotification();
            if (n.actions != null) {
                for (Notification.Action action : n.actions) {
                    if (action.getRemoteInputs() != null && action.getRemoteInputs().length > 0) {
                        notificationWear.remoteInputs = action.getRemoteInputs();
                        notificationWear.pendingIntent = action.actionIntent;
                        break;
                    }
                }
            }
        }

        if (notificationWear.pendingIntent == null) return null;

        notificationWear.bundle = statusBarNotification.getNotification().extras;
        notificationWear.id = statusBarNotification.getId();
        notificationWear.text = notificationWear.bundle.getCharSequence(Notification.EXTRA_TEXT);

        return notificationWear;
    }

    public void dispose(Context context) {
        try {
            LocalBroadcastManager.getInstance(context.getApplicationContext()).unregisterReceiver(receiver);
        } catch (Exception e) {}

        if(notificationWears != null) {
            notificationWears.clear();
            notificationWears = null;
        }
        if(values != null) {
            values.list.clear();
            values = null;
        }

        instance = null;
    }

    @Override
    public XMLPrefsList getValues() {
        return values;
    }

    @Override
    public void write(XMLPrefsSave save, String value) {
        set(new File(FileSystemManager.getFolder(), PATH), save.label(), new String[] {VALUE_ATTRIBUTE}, new String[] {value});
    }

    @Override
    public String[] delete() {
        return null;
    }

    public void check(String pkg) {
        if(!enabled || pkg == null) return;

        NotificationWear wear = notificationWears.get(pkg);
        if(wear == null) {
            wear = findActiveNotification(pkg);
        }

        if(wear == null) {
            Tuils.sendOutput(context, R.string.reply_notification_not_found);
            return;
        }

        Tuils.sendOutput(context, wear.text != null ? wear.text : context.getString(R.string.reply_notification_found_active));
    }

    public List<String> getQuickReplyAppPackages() {
        List<String> pkgs = new ArrayList<>();
        if (notificationWears != null) {
            pkgs.addAll(notificationWears.keySet());
        }
        
        bhupendra.ai.launcher.managers.notifications.NotificationService service = 
            bhupendra.ai.launcher.managers.notifications.NotificationService.instance;
        if (service != null) {
            StatusBarNotification[] sbns = service.getActiveNotifications();
            if (sbns != null) {
                for (StatusBarNotification sbn : sbns) {
                    String p = sbn.getPackageName();
                    if (!pkgs.contains(p)) {
                        if (extractWearNotification(sbn) != null) {
                            pkgs.add(p);
                        }
                    }
                }
            }
        }
        return pkgs;
    }

}
