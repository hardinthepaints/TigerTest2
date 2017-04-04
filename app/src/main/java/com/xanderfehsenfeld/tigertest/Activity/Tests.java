package com.xanderfehsenfeld.tigertest.Activity;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.Log;

import com.xanderfehsenfeld.tigertest.Service.DB.FeedReaderDbHelper;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

/**
 * Created by Xander on 2/22/16.
 *
 * Used for testing the app
 */
public class Tests {


    private static final String TAG = "Tests";


    public static final String TEST_RECEIVER_STRING = "myTestResultReceiver";
    public MyTestResultReceiver myTestResultResultReceiver;

    MainActivity ma;
    public Tests(MainActivity ma){
        this.ma = ma;
        myTestResultResultReceiver = new MyTestResultReceiver(null);

    }

    public void runTests(){
        testDBService();
    }

    private void testDBService(){
        testWithConnectivity();
    }

    /* test permissions */
    public static final String PERMISSION_INTERNET = "android.permission.INTERNET";
    public static final String PERMISSION_WIFI_STATE = "android.permission.ACCESS_WIFI_STATE";
    public static final String PERMISSION_NETWORK_STATE  = "android.permission.ACCESS_NETWORK_STATE";
    public static final String PERMISSION_FINE_LOCATION = "android.permission.ACCESS_FINE_LOCATION";
    public static final String PERMISSION_COARSE_LOCATION = "android.permission.ACCESS_COARSE_LOCATION";
    public static final String PERMISSION_VIBRATE = "android.permission.VIBRATE";
    public static final String PERMISSION_CHANGE_NETWORK_STATE = "android.permission.CHANGE_NETWORK_STATE";


    public static final String[] permissionsToCheck = new String[]{
            PERMISSION_COARSE_LOCATION, PERMISSION_FINE_LOCATION, PERMISSION_INTERNET,
            PERMISSION_NETWORK_STATE, PERMISSION_VIBRATE, PERMISSION_WIFI_STATE, PERMISSION_CHANGE_NETWORK_STATE
    };

    /* update ui with results
*/
    class MyTestResultReceiver extends ResultReceiver
    {
        public MyTestResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            if(resultCode == 100){
            }
            else if(resultCode == 200){
            }
            else{
                boolean wasDeleted = resultData.getBoolean("wasDeleted");
                String uuid = resultData.getString(FeedReaderDbHelper.ID_STRING);
                Log.d(TAG, "received result from DatabaseManagerService: " + uuid + ", " + wasDeleted);
            }
        }
    }


    /**checkPermission
     *      Check to see if you have a permission
     * @param permission
     * @return
     */
    public static boolean checkPermission(String permission, Context c)
    {
        //String permission = "android.permission.WRITE_EXTERNAL_STORAGE";
        int res = c.checkCallingOrSelfPermission(permission);
        return (res == PackageManager.PERMISSION_GRANTED);
    }


    private void testWithConnectivity(){

        for (int i = 0; i < 3; i ++) {
            HashMap<String, String> testRecord = (HashMap<String, String>) FeedReaderDbHelper.types.clone();
            testRecord.put(FeedReaderDbHelper.ID_STRING, "testrecord" + i);
            ma.db.saveRecord(testRecord);
        }

    }

    public void setMobileDataEnabled(Context context, boolean enabled) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        final ConnectivityManager conman = (ConnectivityManager)  context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final Class conmanClass = Class.forName(conman.getClass().getName());
        final Field connectivityManagerField = conmanClass.getDeclaredField("mService");
        connectivityManagerField.setAccessible(true);
        final Object connectivityManager = connectivityManagerField.get(conman);
        final Class connectivityManagerClass =  Class.forName(connectivityManager.getClass().getName());
        final Method setMobileDataEnabledMethod = connectivityManagerClass.getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
        setMobileDataEnabledMethod.setAccessible(true);

        setMobileDataEnabledMethod.invoke(connectivityManager, enabled);
    }
}
