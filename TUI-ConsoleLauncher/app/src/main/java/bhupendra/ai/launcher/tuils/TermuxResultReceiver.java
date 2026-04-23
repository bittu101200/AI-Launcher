package bhupendra.ai.launcher.tuils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import bhupendra.ai.launcher.tuils.Tuils;

public class TermuxResultReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        
        android.util.Log.d("TermuxResultReceiver", "onReceive action: " + intent.getAction());

        Bundle rootBundle = intent.getExtras();
        if (rootBundle == null) {
            Tuils.log("TermuxResultReceiver: No root bundle");
            return;
        }

        // Termux wraps the results in a nested bundle named "result"
        Bundle bundle = intent.getBundleExtra("result");
        if (bundle == null) {
            android.util.Log.d("TermuxResultReceiver", "No 'result' bundle, trying fallbacks");
            bundle = intent.getBundleExtra("EXTRA_PLUGIN_RESULT_BUNDLE");
        }
        if (bundle == null) bundle = intent.getBundleExtra("result_bundle");
        if (bundle == null) bundle = rootBundle;

        String stdout = bundle.getString("stdout");
        String stderr = bundle.getString("stderr");
        int exitCode = bundle.getInt("exitCode", -1);
        
        android.util.Log.d("TermuxResultReceiver", "exitCode=" + exitCode + " stdout=" + (stdout != null ? stdout.length() : "null"));

        int requestCode = intent.getIntExtra("request_code", -1);
        if (requestCode != -1) {
            StringBuilder fullResult = new StringBuilder();
            if (stdout != null && !stdout.trim().isEmpty()) {
                fullResult.append(stdout.trim());
            }
            if (stderr != null && !stderr.trim().isEmpty()) {
                if (fullResult.length() > 0) fullResult.append("\n");
                fullResult.append("Error: ").append(stderr.trim());
            }
            
            String result = fullResult.toString();
            if (result.isEmpty()) result = "[Process finished with exit code " + exitCode + "]";
            TermuxManager.onResultReceived(requestCode, result);
        }

        if (stdout != null && !stdout.trim().isEmpty()) {
            Tuils.sendOutput(context, stdout.trim());
        }

        if (stderr != null && !stderr.trim().isEmpty()) {
            Tuils.sendOutput(Color.RED, context, stderr.trim());
        }

        if ((stdout == null || stdout.trim().isEmpty()) && (stderr == null || stderr.trim().isEmpty())) {
            Tuils.sendOutput(context, "Termux: Process finished (Exit " + exitCode + ")");
        }
    }
}
