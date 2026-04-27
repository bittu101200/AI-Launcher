package bhupendra.ai.launcher.commands.main.raw;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import bhupendra.ai.launcher.LauncherActivity;
import bhupendra.ai.launcher.R;
import bhupendra.ai.launcher.commands.CommandAbstraction;
import bhupendra.ai.launcher.commands.ExecutePack;
import bhupendra.ai.launcher.commands.main.MainPack;
import bhupendra.ai.launcher.tuils.Tuils;

public class call implements CommandAbstraction {

    @Override
    public String exec(ExecutePack pack) {
        final MainPack info = (MainPack) pack;
        if (ContextCompat.checkSelfPermission(info.getContext(), Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(info.getContext(), Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions((Activity) info.getContext(), new String[]{Manifest.permission.READ_CONTACTS, Manifest.permission.CALL_PHONE}, LauncherActivity.COMMAND_REQUEST_PERMISSION);
            return info.getContext().getString(R.string.output_waitingpermission);
        }

        String number = info.getString();
        android.util.Log.d("CALL_CMD", "Resolved argument: " + number);
        if(number == null) return pack.getContext().getString(R.string.invalid_number);

        StringBuilder s = new StringBuilder(Tuils.EMPTYSTRING);
        for(char c : number.toCharArray()) {
            if(c == '#') s.append(Uri.encode("#"));
            else s.append(c);
        }

        android.util.Log.d("CALL_CMD", "Encoded number: " + s.toString());
        Uri uri = Uri.parse("tel:" + s);
        if(uri == null) return pack.getContext().getString(R.string.invalid_number);

        final Intent intent = new Intent(Intent.ACTION_CALL, uri);

        try {
            ((Activity) pack.getContext()).runOnUiThread(() -> info.getContext().startActivity(intent));
        } catch (SecurityException e) {
            return info.getResources().getString(R.string.output_nopermissions);
        }

        return info.getResources().getString(R.string.calling) + " " + number;
    }

    @Override
    public int helpRes() {
        return R.string.help_call;
    }

    @Override
    public int[] argType() {
        return new int[]{CommandAbstraction.CONTACTNUMBER};
    }

    @Override
    public int priority() {
        return 5;
    }

    @Override
    public String onNotArgEnough(ExecutePack info, int nArgs) {
        return info.getContext().getString(helpRes());
    }

    @Override
    public String onArgNotFound(ExecutePack pack, int index) {
        MainPack info = (MainPack) pack;
        return info.getResources().getString(R.string.output_numbernotfound);
    }


    @Override
    public bhupendra.ai.launcher.commands.CommandMetadata getMetadata(android.content.Context context) {
        return new bhupendra.ai.launcher.commands.CommandMetadata("call", "Call someone", null, null, null);
    }
}
