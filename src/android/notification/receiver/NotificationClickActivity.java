package de.appplant.cordova.plugin.notification.receiver;

import static android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT;
import static android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP;

import static de.appplant.cordova.plugin.notification.Options.EXTRA_LAUNCH;
import static de.appplant.cordova.plugin.notification.Request.EXTRA_LAST;
import static de.appplant.cordova.plugin.notification.action.Action.CLICK_ACTION_ID;
import static de.appplant.cordova.plugin.notification.action.Action.EXTRA_ID;

import androidx.core.app.RemoteInput;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import org.json.JSONException;
import org.json.JSONObject;

import de.appplant.cordova.plugin.localnotification.LocalNotification;
import de.appplant.cordova.plugin.notification.Manager;
import de.appplant.cordova.plugin.notification.Notification;

/**
 * This activity recieves the notification pending intent for Android 12 to handle notification
 * trampoline restrictions.
 */
public class NotificationClickActivity extends Activity {

    private Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.intent        = getIntent();

        if (intent == null)
            return;

        Bundle bundle      = intent.getExtras();
        Context context    = getApplicationContext();
        if (bundle == null)
            return;

        int toastId        = bundle.getInt(Notification.EXTRA_ID);
        Notification toast = Manager.getInstance(context).get(toastId);

        if (toast == null)
            return;

        onClick(toast, bundle);
        this.intent = null;
    }

    public void onClick(Notification notification, Bundle bundle) {
        String action   = getIntent().getExtras().getString(EXTRA_ID, CLICK_ACTION_ID);
        JSONObject data = new JSONObject();

        setTextInput(action, data);
        launchAppIf();

        LocalNotification.fireEvent(action, notification, data);

        if (notification.getOptions().isSticky())
            return;

        if (isLast()) {
            notification.cancel();
        } else {
            notification.clear();
        }
    }

    /**
     * If the notification was the last scheduled one by request.
     */
    private boolean isLast() {
        return getIntent().getBooleanExtra(EXTRA_LAST, false);
    }


    /**
     * Set the text if any remote input is given.
     *
     * @param action The action where to look for.
     * @param data   The object to extend.
     */
    private void setTextInput(String action, JSONObject data) {
        Bundle input = RemoteInput.getResultsFromIntent(getIntent());

        if (input == null)
            return;

        try {
            data.put("text", input.getCharSequence(action));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Launch app if requested by user.
     */
    private void launchAppIf() {
        boolean doLaunch = getIntent().getBooleanExtra(EXTRA_LAUNCH, true);

        if (!doLaunch)
            return;

        launchApp();
    }

    /**
     * Launch main intent from package.
     */
    protected void launchApp() {
        Context context = getApplicationContext();
        String pkgName  = context.getPackageName();
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(pkgName);

        if (intent == null)
            return;

        intent.addFlags(
            FLAG_ACTIVITY_REORDER_TO_FRONT
                | FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }

}
