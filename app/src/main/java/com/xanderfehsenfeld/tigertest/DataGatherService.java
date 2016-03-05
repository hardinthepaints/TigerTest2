package com.xanderfehsenfeld.tigertest;


/**
 * created by Xander
 */

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import com.xanderfehsenfeld.tigertest.GPS.GPSTracker;
import com.xanderfehsenfeld.tigertest.Launcher.MySharedPrefsWrapper;
import com.xanderfehsenfeld.tigertest.Launcher.SpeedTester;
import com.xanderfehsenfeld.tigertest.Launcher.Tests;
import com.xanderfehsenfeld.tigertest.LocalDB.DatabaseManagerService;
import com.xanderfehsenfeld.tigertest.LocalDB.FeedReaderDbHelper;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;

public class DataGatherService extends DatabaseManagerService
{

    private static final int MSG_UPDATE_STATUS = 0;
    private static final int MSG_UPDATE_CONNECTION_TIME = 1;
    private static final int MSG_COMPLETE_STATUS = 2;
    public static final int RECEIVED_FROM_TESTER = -4;
    public static final int PUT_META_DATA = -1;
    public static final int TIMED_OUT = -2;
    public static final int WIFI_CONNECTED_RESULT = -3;
    public static final int IS_ALLOWED_NETWORK = -5;
    public static final int MSG_SETTINGS_CHANGE = -10;
    public static final String TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final int MSG_SENT_SHARED_PREFS = -11;
    public String DOWNLOAD_FILE_URL = "http://www.smdc.army.mil/smdcphoto_gallery/Missiles/IFT_13B_Launch/IFT13b-3-02.jpg";

    private static final String TAG = "DataGatherService";

    public static final int MSG_START_TEST = 1;


    /* send data back to MainActivity */
    //ResultReceiver resultReceiver;

    /* TODO remove after testing */
    //ResultReceiver mTestResultReceiver;

    /* keep speech recognizer going */
    CountDownTimer mTimer;

    /* db with wrapper class */
    private boolean mTransferInProgress = false;
    private Location mLastLocation;
    private Geocoder mGeocoder;
    private GPSTracker mGPSTRacker;
    private HashMap<String, String> data;
    public CountDownTimer mCountDownTimer;
    private boolean isContinuous = false;
    private int mConnectionTime;
    //public int TIME_LIMIT = 5;
    //public int CONNECT_TIMEOUT = 0;
    private boolean activityStopped = true;


    @Override
    public void onCreate()
    {
        super.onCreate();


        Log.d(TAG, "onCreate"); //$NON-NLS-1$

        mServerMessenger = new Messenger(new IncomingHandler(this));

        initDataSources();


    }

    private void initDataSources(){
        mGPSTRacker = new GPSTracker(this);
        mGeocoder = new Geocoder(this);

    }

    /**
     * Our Slave worker that does actually all the work
     */
    protected final SpeedTester mSpeedTester = new SpeedTester(this);

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(TAG, "onStartCommand"); //$NON-NLS-1$
        return START_STICKY;

    }
    /* IncomingHandler */
    protected class IncomingHandler extends Handler
    {
        private WeakReference<DatabaseManagerService> mtarget;

        IncomingHandler(DatabaseManagerService target)
        {
            mtarget = new WeakReference<DatabaseManagerService>(target);
            Log.d(TAG, "IncomingHandler");
        }


        @Override
        public void handleMessage(Message msg)
        {
            final DatabaseManagerService target = mtarget.get();

            switch (msg.what)
            {
                case MSG_START_TEST:

                    onStartBtnClicked();



                    break;

                case MSG_RECORD_ADDED:

                    Log.d(TAG, "message record added"); //$NON-NLS-1$

                    if( !mTransferInProgress ){
                        transferRecords();
                    }
                    break;

//                case SpeedTestLauncher.MSG_ACTIVITY_STATUS_CHANGE:
//
//                    activityStopped = (boolean) msg.obj;
//
//                    Log.d(TAG, "MSG_ACTIVITY_STATUS_CHANGE. activityStopped: " + activityStopped);
//
//                    break;

                case MSG_SENT_SHARED_PREFS:

                    sharePrfs = (MySharedPrefsWrapper)msg.obj;
                    Log.d(TAG, "MSG_SENT_SHARED_PREFS. testCount: " + sharePrfs.getTestCount());

                    sharePrfs.prefs.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
                        @Override
                        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                            Log.d(TAG, "Shared pref changed. key: " + key );
                        }
                    });

                    break;


            }
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();


    }

    @Override
    public boolean onUnbind(Intent intent){
        Log.d(TAG, "onUnbind");  //$NON-NLS-1$


        return false;
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        super.onBind(intent);



        Log.d(TAG, "onBind");  //$NON-NLS-1$


        /* send an initial amount */
        return mServerMessenger.getBinder();


    }



    /** get the metadata
     *		returns whether or not to continue with the test
     */
    protected boolean putMetaData(){

		/* initialize new hashmap to store data */
        data = new HashMap<String, String>();

        data.put(FeedReaderDbHelper.TEST_COUNT, "" + sharePrfs.getTestCount());

        // get the MAC address of the router
        WifiManager wifiMgr = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        /* check for permission */
        String wirelessAccessPtMACAddr = "test : no permission";
        String MACAddr = "test: no permission";
        if ( Tests.checkPermission(Tests.PERMISSION_WIFI_STATE, this)) {

            WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
            //String wirelessNetworkName = wifiInfo.getSSID();
            wirelessAccessPtMACAddr = wifiInfo.getBSSID();

            /* get the mac addr */
            MACAddr = getMacAddr();


        }else {
            Log.e(TAG, "lacking permission: " + Tests.PERMISSION_WIFI_STATE);
        }

        data.put(FeedReaderDbHelper.MAC_STRING, MACAddr);
        data.put(FeedReaderDbHelper.ACCESSPT_STRING, wirelessAccessPtMACAddr);


		/* decide network type */
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

		/* get info on the active network */
        NetworkInfo info = cm.getActiveNetworkInfo();

		/* check if wi fi is on */
        NetworkInfo mWiFi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (!mWiFi.isConnected()) {
            sendMessageToActivity(WIFI_CONNECTED_RESULT, false, null);
            return false;
        }


		/* store data in hashmap */

		/* specify the file that was downloaded */
        data.put(FeedReaderDbHelper.DOWNLOAD_STRING, DOWNLOAD_FILE_URL);

		/* update gps location */
        updateLocation();
        data.put(FeedReaderDbHelper.LAT_STRING, mLastLocation.getLatitude() + "");
        data.put(FeedReaderDbHelper.LONG_STRING, mLastLocation.getLongitude() + "");
        data.put(FeedReaderDbHelper.ALT_STRING, mLastLocation.getAltitude() + "");

        /* attempt to get geo code */
        try {
            String address = mGeocoder.getFromLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude(), 1).get(0).getAddressLine(0);
            data.put(FeedReaderDbHelper.GEOCODE_STRING, address);
        } catch (IOException e) {
            //e.printStackTrace();
            Log.e( TAG, e.getMessage() );
        }

		/* get the time */
        data.put(FeedReaderDbHelper.TIMESTAMP_STRING, getTimeStamp());
        data.put(FeedReaderDbHelper.TIMESTAMPFMT_STRING, TIMESTAMP_FORMAT);



		/* name of network */
        String network = info.getExtraInfo().replace('"', ' ').trim();
        data.put(FeedReaderDbHelper.NETWORK_STRING, network);


        /* store the location provider (either network or gps) */
        data.put( FeedReaderDbHelper.LOCATIONPROV_STRING, mLastLocation.getProvider());

		/* give the data a unique id */
        data.put(FeedReaderDbHelper.ID_STRING, UUID.randomUUID() + "");


        if (!isAllowedNetwork(network)) {

            sendMessageToActivity(IS_ALLOWED_NETWORK, false, network);
            return false;
        }

        return true;

    }

    /* get the mac address of the device
    * should be called only once
       */
    private String getMacAddr(){
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo wInfo = wifiManager.getConnectionInfo();
        String mac = wInfo.getMacAddress();
        return mac;

    }

    /* get the most current location */
    public void updateLocation(){
        mLastLocation = mGPSTRacker.getLocation();

        Log.d(TAG, "updateLocation. " + "lat: " + mLastLocation.getLatitude() + ", " +
                "lon: " + mLastLocation.getLongitude() + " prov: " +
                mLastLocation.getProvider() + ", acc: " + mLastLocation.getAccuracy());


    }

    /** getTimeStamp
     *      mountain standard time down to seconds
     * @return
     */
    private static String getTimeStamp() {

        SimpleDateFormat dateFormat = new SimpleDateFormat(
                    TIMESTAMP_FORMAT, Locale.US);
        Date date = new Date();
        return dateFormat.format(date);

    }


    /** isAllowedNetwork
     * 		tests to see if the network name is one of the allowed networks
     * @param input the name of the network
     * @return whether or not is allowed
     */
    private boolean isAllowedNetwork( String input ){
		/* networks which are allowed to test */
        String[] allowedNetworks = new String[]{"WiOfTheTiger", "WiOfTheTiger-Employee", "NETGEAR64"};

        for (String network : allowedNetworks){
            if (input.equals(network)) return true;
        }

        return false;
    }


    /**onStartBtnClicked
     *      the result of the start button clicked
     * */
    private void onStartBtnClicked() {
        /* get initial metadata and put in a hashmap */
        boolean putMetaDataSuccessful = false;
        if (putMetaDataSuccessful = putMetaData()) {

            final Thread workerThread = new Thread(mSpeedTester);
            workerThread.start();

            /* make the thread timeout after a certain time */
            int tl = sharePrfs.prefs.getInt(sharePrfs.PREF_TIME_LIMIT, 10);
            mCountDownTimer = new CountDownTimer(1000 * tl, 1000) {

                @Override
                public void onTick(long millisUntilFinished) {
                }

                @Override
                public void onFinish() {
                    sendMessageToActivity(TIMED_OUT, sharePrfs.getIsContinuous(), null);
                    workerThread.interrupt();
                    Log.d(TAG, "countdown finished. "); //$NON-NLS-1$


                }
            };
            mCountDownTimer.start();
        }

        sendMessageToActivity(PUT_META_DATA, putMetaDataSuccessful, null);

    }

    /** forwareMessageToActivity
     *
     * @param msg   the messsage to be copied and forwarded
     * @param b the bundle to use, if null then it will create a new bundle
     */

    private void forwardSpeedTestMessage(Message msg, Bundle b){
        if (!(!sharePrfs.prefs.getBoolean(sharePrfs.PREF_ACTIVITY_RUNNING, false) && sharePrfs.getIsContinuous()) ) {
            if (b == null) {
                b = new Bundle();
            }
        /* cannot reuse messages */
            Message newMessage = new Message();
            newMessage.copyFrom(msg);

            b.putParcelable(String.valueOf(RECEIVED_FROM_TESTER), newMessage);
            resultReceiver.send(RECEIVED_FROM_TESTER, b);
        } else {
            Log.e(TAG, "suppressed message. activityRunning, isContinuous " +
                    sharePrfs.getIsActivityRunning() + ", " + sharePrfs.getIsContinuous());

        }
    }

    /** sendMessageToActivity
     *      send a notification back to the activity. If the activity is stopped
     * @param code the associated code
     * @param bool the associated boolean
     * @param s option string param
     */
    private void sendMessageToActivity(int code, boolean bool, String s) {
        if (!(!sharePrfs.getIsActivityRunning() && sharePrfs.getIsContinuous())) {
            Bundle b = new Bundle();
            b.putBoolean("", bool);
            b.putString("s", s);
            resultReceiver.send(code, b);
        } else {
            Log.e(TAG, "suppressed message. activityRunning, isContinuous " +
                    sharePrfs.getIsActivityRunning() + ", " + sharePrfs.getIsContinuous());        }
    }



    public final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(final Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_STATUS:
                    final SpeedTester.SpeedInfo info1 = (SpeedTester.SpeedInfo) msg.obj;

                    /* send the message back to the activity for UI purposes */
                    forwardSpeedTestMessage(msg, null);

                    break;
                case MSG_UPDATE_CONNECTION_TIME:
                    //mTxtConnectionSpeed.setText(String.format(getResources().getString(R.string.update_connectionspeed), msg.arg1));
                    mConnectionTime = msg.arg1;
                    forwardSpeedTestMessage(msg, null);
                    break;
                case MSG_COMPLETE_STATUS:

                    /* increment test count */
                    sharePrfs.setTestCount( sharePrfs.getTestCount() + 1 );


                    final SpeedTester.SpeedInfo info2 = (SpeedTester.SpeedInfo) msg.obj;

				/* store data in hashmap */

				/* download speed and total bytes downloaded */
                    data.put(FeedReaderDbHelper.SPEED_STRING, info2.megabits + "");
                    data.put(FeedReaderDbHelper.SPEED_STRING_UNIT, getString(R.string.down_speed_unit));

                    data.put(FeedReaderDbHelper.BYTES_STRING, msg.arg1 + "");

				/* connection time in ms */
                    data.put(FeedReaderDbHelper.CONNTIME_STRING, mConnectionTime + "");
                    data.put(FeedReaderDbHelper.CONNTIMEUNIT_STRING, getString(R.string.conn_time_unit));

                    Bundle b = new Bundle();
                    b.putSerializable("data", data);

                    /* inform ui test is complete */
                    forwardSpeedTestMessage(msg, b);


                /* store data in db */
                    if ( data != null) {
                        db.saveRecord(data);
                        String uuid = data.get(FeedReaderDbHelper.ID_STRING);
                        //removeRecord(uuid);
                    } else {
                        throw new IllegalStateException("Data hashmap is null. Did not save record");
                    }




                    /* keep testing even if activity in not bound and continuous mode is on
                    *   if activity is bound, and continuous mode on, then the handler activity side
                    *   will start the test again
                    * */
                    if ( !sharePrfs.prefs.getBoolean(sharePrfs.PREF_ACTIVITY_RUNNING, false) && sharePrfs.getIsContinuous() ) onStartBtnClicked();

                    /* tell super service to transfer records */
                    if( !mTransferInProgress ){
                        transferRecords();
                    }



                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    };











}