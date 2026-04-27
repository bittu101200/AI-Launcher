package bhupendra.ai.launcher.managers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import bhupendra.ai.launcher.MainManager;

public class CronManager {

    private static final String TAG = "CronManager";
    private static final String PREFS_NAME = "cron_tasks";
    private static final String ACTION_SCHEDULED_EXEC = "bhupendra.ai.launcher.SCHEDULED_EXEC";
    
    private final Context context;
    private final SharedPreferences prefs;
    private final AlarmManager alarmManager;

    public CronManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    public static class Task {
        public String id;
        public String command;
        public long timestamp; // When it should run
        public boolean recurring;
        public long interval; // Interval in ms if recurring

        public Task(String id, String command, long timestamp) {
            this.id = id;
            this.command = command;
            this.timestamp = timestamp;
        }

        public JSONObject toJSON() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("id", id);
            json.put("command", command);
            json.put("timestamp", timestamp);
            json.put("recurring", recurring);
            json.put("interval", interval);
            return json;
        }

        public static Task fromJSON(JSONObject json) throws JSONException {
            Task task = new Task(json.getString("id"), json.getString("command"), json.getLong("timestamp"));
            task.recurring = json.optBoolean("recurring", false);
            task.interval = json.optLong("interval", 0);
            return task;
        }
    }

    public synchronized String addOneTimeTask(String command, long delayMs) {
        String id = "task_" + System.currentTimeMillis();
        long triggerAt = System.currentTimeMillis() + delayMs;
        Task task = new Task(id, command, triggerAt);
        saveTask(task);
        scheduleAlarm(task);
        return id;
    }

    public synchronized List<Task> getAllTasks() {
        List<Task> tasks = new ArrayList<>();
        Map<String, ?> all = prefs.getAll();
        long now = System.currentTimeMillis();
        SharedPreferences.Editor editor = prefs.edit();
        boolean changed = false;

        for (Map.Entry<String, ?> entry : all.entrySet()) {
            try {
                Task task = Task.fromJSON(new JSONObject((String) entry.getValue()));
                if (task.timestamp < now - 60000) { // Cleanup tasks more than 1 min old
                    editor.remove(task.id);
                    changed = true;
                    continue;
                }
                tasks.add(task);
            } catch (Exception e) {
                Log.e(TAG, "Error parsing task: " + entry.getKey(), e);
                editor.remove(entry.getKey());
                changed = true;
            }
        }
        if (changed) editor.apply();
        return tasks;
    }

    public synchronized void cancelTask(String id) {
        prefs.edit().remove(id).apply();
        Intent intent = new Intent(context, ScheduledTaskReceiver.class);
        intent.setAction(ACTION_SCHEDULED_EXEC);
        PendingIntent pi = PendingIntent.getBroadcast(context, id.hashCode(), intent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        alarmManager.cancel(pi);
    }

    private void saveTask(Task task) {
        try {
            prefs.edit().putString(task.id, task.toJSON().toString()).apply();
        } catch (JSONException e) {
            Log.e(TAG, "Error saving task", e);
        }
    }

    private void scheduleAlarm(Task task) {
        Intent intent = new Intent(context, ScheduledTaskReceiver.class);
        intent.setAction(ACTION_SCHEDULED_EXEC);
        intent.putExtra("task_id", task.id);
        intent.putExtra("command", task.command);

        int piFlags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        
        PendingIntent pi = PendingIntent.getBroadcast(context, task.id.hashCode(), intent, piFlags);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, task.timestamp, pi);
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, task.timestamp, pi);
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, task.timestamp, pi);
        }
        Log.d(TAG, "Scheduled alarm for task " + task.id + " at " + task.timestamp);
    }

    public static class ScheduledTaskReceiver extends android.content.BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String taskId = intent.getStringExtra("task_id");
            String command = intent.getStringExtra("command");
            Log.d(TAG, "Received alarm for task: " + taskId + " command: " + command);

            if (command != null) {
                Intent execIntent = new Intent(MainManager.ACTION_EXEC);
                execIntent.putExtra(MainManager.CMD, command);
                execIntent.putExtra(MainManager.CMD_COUNT, MainManager.commandCount + 1);
                context.sendBroadcast(execIntent);
            }

            if (taskId != null) {
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().remove(taskId).apply();
            }
        }
    }
}
