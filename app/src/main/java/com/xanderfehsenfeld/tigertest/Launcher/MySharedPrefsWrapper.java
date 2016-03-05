package com.xanderfehsenfeld.tigertest.Launcher;

import android.app.Activity;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Created by Xander on 2/24/16.
 */
public class MySharedPrefsWrapper {

    public static final String PREF_DEFAULTS_SET = "PREF_DEFAULTS_SET";
    public static final String PREF_IS_CONTINUOUS = "PREF_IS_CONTINUOUS";
    public static final String TAG = "MySharedPrefsWrapper";
    public static final String PREF_SOUND_ON = "PREF_SOUND_ON";
    public static final String PREF_TIME_LIMIT = "PREF_TIME_LIMIT";
    public static final String PREF_CONNECT_TIME_LIMIT = "PREF_CONNECT_TIME_LIMIT";
    public static final String PREF_TEST_COUNT = "PREF_TEST_COUNT";
    public static final String PREF_INTERNET_ON = "PREF_INTERNET_ON";
    public static final String PREF_ACTIVITY_RUNNING = "PREF_ACTIVITY_RUNNING ";
    public static final String DEFAULTS_SET = "DEFAULTS_SET";


    Activity a;
    public SharedPreferences prefs;
    public SharedPreferences.Editor prefEditer;

    public MySharedPrefsWrapper(Activity a){
        this.a = a;

        initPrefs();

    }

    private void initPrefs(){
        /* user preferences */
        prefs = a.getPreferences(a.MODE_PRIVATE);

        prefEditer = prefs.edit();

        if (!prefs.getBoolean(DEFAULTS_SET, false)) {

            /* set default values */
            prefEditer.putBoolean(PREF_IS_CONTINUOUS, false);
            prefEditer.putBoolean(PREF_SOUND_ON, true);
            prefEditer.putInt(PREF_TIME_LIMIT, 5);
            prefEditer.putInt(PREF_CONNECT_TIME_LIMIT, 10000);
            prefEditer.putInt(PREF_TEST_COUNT, 0);
            prefEditer.putBoolean(PREF_INTERNET_ON, true);
            prefEditer.putBoolean(PREF_ACTIVITY_RUNNING, false);
            prefEditer.putBoolean(DEFAULTS_SET, true);

            prefEditer.commit();

            Log.d(TAG, "default prefs set");
        } else { Log.d(TAG, "default prefs ALREADY set");}
    }

    public void setTestCount( int newCount ){
        prefEditer.putInt(PREF_TEST_COUNT, newCount);
        Log.d(TAG, "set test count to: " + newCount);
        prefEditer.commit();

    }

    public int getTestCount(  ){
        return prefs.getInt(PREF_TEST_COUNT, 0);
    }

    public boolean getIsContinuous(){return prefs.getBoolean(PREF_IS_CONTINUOUS, false);}

    public boolean getIsActivityRunning(){
        return prefs.getBoolean(PREF_ACTIVITY_RUNNING, false);
    }


}
