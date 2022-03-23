package cordova.plugins.crosswalk;

/*
 * Imports
 */

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;

import android.content.pm.PackageManager;

import java.io.File;

public class Migration extends CordovaPlugin {

    public static final String TAG = "Migration";

    private static boolean hasRun = false;

    private static String XwalkPath = "app_xwalkcore/Default";

    // Root dir for system webview data used by Android 4.4+
    private static String modernWebviewDir = "app_webview";

    // Root dir for system webview data used by Android 4.3 and below
    private static String oldWebviewDir = "app_database";

    // Directory name for local storage files used by Android 4.4+ and XWalk
    private static String modernLocalStorageDir = "Local Storage";

    // Directory name for local storage files used by Android 4.3 and below
    private static String oldLocalStorageDir = "localstorage";

    // Storage directory names used by Android 4.4+ and XWalk
    private static String[] modernAndroidStorage = {
            "Cache",
            "Cookies",
            "Cookies-journal",
            "IndexedDB",
            "databases"
    };

    private Activity activity;
    private Context context;

    private boolean isModernAndroid;
    private File appRoot;
    private File XWalkRoot;
    private File webviewRoot;

    protected Context applicationContext;

    public Migration() {}

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        Log.d(TAG, "initialize()");

        applicationContext = this.cordova.getActivity().getApplicationContext();

        if(!hasRun){
            hasRun = true;
            activity = cordova.getActivity();
            context = activity.getApplicationContext();
            isModernAndroid = Build.VERSION.SDK_INT >= 19;
            run();
        }

        super.initialize(cordova, webView);
    }

    private void run(){
        Log.d(TAG, "running Crosswalk migration shim");

        boolean found = lookForXwalk(context.getFilesDir());
        if(!found){
            lookForXwalk(context.getExternalFilesDir(null));
        }

        if(found){
            migrateData();
        }
    }

    private boolean lookForXwalk(File filesPath){
        File root = getStorageRootFromFiles(filesPath);
        boolean found = testFileExists(root, XwalkPath);
        if(found){
            Log.d(TAG, "found Crosswalk directory");
            appRoot = root;
        }else{
            Log.d(TAG, "Crosswalk directory NOT FOUND");
        }
        return found;
    }

    private void migrateData(){
        XWalkRoot = constructFilePaths(appRoot, XwalkPath);

        webviewRoot = constructFilePaths(appRoot, getWebviewPath());

        boolean hasMigratedData = false;

        if(testFileExists(XWalkRoot, modernLocalStorageDir)){
            Log.d(TAG, "Local Storage data found");
            moveDirFromXWalkToWebView(modernLocalStorageDir, getWebviewLocalStoragePath());
            Log.d(TAG, "Moved Local Storage from XWalk to System Webview");
            hasMigratedData = true;
        }

        if(isModernAndroid){
            for(String dirName : modernAndroidStorage){
                if(testFileExists(XWalkRoot, dirName)) {
                    moveDirFromXWalkToWebView(dirName);
                    Log.d(TAG, "Moved " + dirName + " from XWalk to System Webview");
                    hasMigratedData = true;
                }
            }
        }

        if(hasMigratedData){
            deleteRecursive(XWalkRoot);
            restartCordova();
        }
    }

    private void moveDirFromXWalkToWebView(String dirName){
        File XWalkLocalStorageDir = constructFilePaths(XWalkRoot, dirName);
        File webviewLocalStorageDir = constructFilePaths(webviewRoot, dirName);
        XWalkLocalStorageDir.renameTo(webviewLocalStorageDir);
    }

    private void moveDirFromXWalkToWebView(String sourceDirName, String targetDirName){
        File XWalkLocalStorageDir = constructFilePaths(XWalkRoot, sourceDirName);
        File webviewLocalStorageDir = constructFilePaths(webviewRoot, targetDirName);
        XWalkLocalStorageDir.renameTo(webviewLocalStorageDir);
    }


    private String getWebviewPath(){
        if(isModernAndroid){
            return modernWebviewDir;
        }else{
            return oldWebviewDir;
        }
    }

    private String getWebviewLocalStoragePath(){
        if(isModernAndroid){
            return modernLocalStorageDir;
        }else{
            return oldLocalStorageDir;
        }
    }

    private void restartCordova(){
        Log.d(TAG, "restarting Cordova activity");
        this.doColdRestart();
    }


    private boolean testFileExists(File root, String name) {
        boolean status = false;
        if (!name.equals("")) {
            File newPath = constructFilePaths(root.toString(), name);
            status = newPath.exists();
            Log.d(TAG, "exists '"+newPath.getAbsolutePath()+": "+status);
        }
        return status;
    }

    private File constructFilePaths (File file1, File file2) {
        return constructFilePaths(file1.getAbsolutePath(), file2.getAbsolutePath());
    }

    private File constructFilePaths (File file1, String file2) {
        return constructFilePaths(file1.getAbsolutePath(), file2);
    }

    private File constructFilePaths (String file1, String file2) {
        File newPath;
        if (file2.startsWith(file1)) {
            newPath = new File(file2);
        }
        else {
            newPath = new File(file1 + "/" + file2);
        }
        return newPath;
    }

    private File getStorageRootFromFiles(File filesDir){
        String filesPath = filesDir.getAbsolutePath();
        filesPath = filesPath.replaceAll("/files", "");
        return new File(filesPath);
    }

    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);

        fileOrDirectory.delete();
    }

    /**
     * Performs a full cold app restart - restarts application
     * https://stackoverflow.com/a/22345538/777265
     */
    protected void doColdRestart() {
        String baseError = "Unable to cold restart application: ";
        try {
            Log.d(TAG, "Cold restarting application");
            Context c = applicationContext;
            //check if the context is given
            if (c != null) {
                //fetch the packagemanager so we can get the default launch activity
                // (you can replace this intent with any other activity if you want
                PackageManager pm = c.getPackageManager();
                //check if we got the PackageManager
                if (pm != null) {
                    //create the intent with the default start activity for your application
                    Intent mStartActivity = pm.getLaunchIntentForPackage(
                            c.getPackageName()
                    );
                    if (mStartActivity != null) {
                        //mStartActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        //create a pending intent so the application is restarted after System.exit(0) was called.
                        // We use an AlarmManager to call this intent in 100ms
                        int mPendingIntentId = 223344;
                        PendingIntent mPendingIntent = PendingIntent
                                .getActivity(c, mPendingIntentId, mStartActivity,
                                        PendingIntent.FLAG_CANCEL_CURRENT);
                        AlarmManager mgr = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
                        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
                        Log.i(TAG, "Killing application for cold restart");
                        //kill the application
                        System.exit(0);
                    } else {
                        handleError(baseError+"StartActivity is null");
                    }
                } else {
                    handleError(baseError+"PackageManager is null");
                }
            } else {
                handleError(baseError+"Context is null");
            }
        } catch (Exception ex) {
            handleError(baseError+ ex.getMessage());
        }
    }

    /**
     * Handles an error while executing a plugin API method  in the specified context.
     * Calls the registered Javascript plugin error handler callback.
     * @param errorMsg Error message to pass to the JS error handler
     */
    public void handleError(String errorMsg){
        try {
            logError(errorMsg);
        } catch (Exception e) {
            logError(e.toString());
        }
    }

    public void logError(String msg){
        Log.e(TAG, msg);
    }

}
