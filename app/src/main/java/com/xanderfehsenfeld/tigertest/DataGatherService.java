package com.xanderfehsenfeld.tigertest;


/**
 * created by Xander
 */

import android.content.Context;
import android.content.Intent;
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
import com.xanderfehsenfeld.tigertest.Launcher.SpeedTestLauncher;
import com.xanderfehsenfeld.tigertest.Launcher.SpeedTester;
import com.xanderfehsenfeld.tigertest.Launcher.Tests;
import com.xanderfehsenfeld.tigertest.LocalDB.DatabaseManagerService;
import com.xanderfehsenfeld.tigertest.LocalDB.FeedReaderDbHelper;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
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
    public String DOWNLOAD_FILE_URL = "http://www.smdc.army.mil/smdcphoto_gallery/Missiles/IFT_13B_Launch/IFT13b-3-02.jpg";


    private static final String TAG = "DataGatherService";



    public static final int MSG_STOP_TEST = 2;
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
    public int TIME_LIMIT = 5;
    public int CONNECT_TIMEOUT = 0;
    private String SERVER_URL;
    private boolean activityStopped = false;


    @Override
    public void onCreate()
    {


        Log.d(TAG, "onCreate"); //$NON-NLS-1$

        mServerMessenger = new Messenger(new IncomingHandler(this));

        initDataSources();




    }

    private void initDataSources(){
        SERVER_URL = getString(R.string.server_address);
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

                    TIME_LIMIT = msg.arg1;
                    CONNECT_TIMEOUT = msg.arg2;
                    isContinuous = (boolean) msg.obj;

                    Log.d(TAG, "message start test. time limit: " + TIME_LIMIT);
                    onStartBtnClicked();



                    break;

                case MSG_RECORD_ADDED:

                    Log.d(TAG, "message record added"); //$NON-NLS-1$

                    if( !mTransferInProgress ){
                        transferRecords();
                    }
                    break;


            }
        }
    }


//    /* IncomingHandler */
//    protected class IncomingHandler extends Handler
//    {
//        private WeakReference<DataGatherService> mtarget;
//
//        IncomingHandler(DataGatherService target)
//        {
//            mtarget = new WeakReference<DataGatherService>(target);
//            Log.d(TAG, "IncomingHandler");
//        }
//
//
//        @Override
//        public void handleMessage(Message msg)
//        {
//            final DataGatherService target = mtarget.get();
//
//            switch (msg.what)
//            {
//                case MSG_STOP_TEST:
//
//                    /* code to deal with stopping the test early */
//
//                case MSG_START_TEST:
//
//                    /* start the test */
//
//
//            }
//        }
//    }




    @Override
    public void onDestroy()
    {
        super.onDestroy();


//        /* send end code to parent activity */
//        Bundle bundle = new Bundle();
//        //bundle.putString("end", "Timer Stopped....");
//        resultReceiver.send(200, bundle);
    }

    @Override
    public boolean onUnbind(Intent intent){
        Log.d(TAG, "onUnbind");  //$NON-NLS-1$

        activityStopped = true;

        return false;
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        super.onBind(intent);

        activityStopped = false;


        Log.d(TAG, "onBind");  //$NON-NLS-1$
//
//        resultReceiver = intent.getParcelableExtra("receiver");
//
//        /* TODO remove after testing */
//        mTestResultReceiver = intent.getParcelableExtra(Tests.TEST_RECEIVER_STRING);
//
//        db = intent.getParcelableExtra("db");
//        if ( db != null ) {
//            Log.d(TAG, "recieved db not null");
//        } else{
//            Log.e(TAG, "recieved db is null");
//        }
//
//
//        FeedReaderDbHelper mDbHelper = new FeedReaderDbHelper(getApplicationContext());
//        db = new MyDbWrapper(mDbHelper.getWritableDatabase());
//        Log.d(TAG, "opened writeable db:");
//        db.printDb();


        /* send an initial amount */
        return mServerMessenger.getBinder();
    }



    /** get the metadata
     *		returns whether or not to continue with the test
     */
    protected boolean putMetaData(){

		/* initialize new hashmap to store data */
        data = new HashMap<String, String>();

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
            //showPopup( "WIFI NOT CONNECTED", 0 );
            //playSound(SOUND_ERROR);
            Bundle b = new Bundle();
            b.putBoolean("", false);
            resultReceiver.send(WIFI_CONNECTED_RESULT, b);
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
        data.put(FeedReaderDbHelper.TIMESTAMPFMT_STRING, SpeedTestLauncher.TIMESTAMP_FORMAT);



		/* name of network */
        String network = info.getExtraInfo().replace('"', ' ').trim();
        data.put(FeedReaderDbHelper.NETWORK_STRING, network);


        /* store the location provider (either network or gps) */
        data.put( FeedReaderDbHelper.LOCATIONPROV_STRING, mLastLocation.getProvider());

		/* give the data a unique id */
        data.put(FeedReaderDbHelper.ID_STRING, UUID.randomUUID() + "");


        if (!isAllowedNetwork(network)) {

            Bundle b = new Bundle();
            b.putBoolean("", false);
            b.putString("network", network);
            resultReceiver.send(IS_ALLOWED_NETWORK, b);
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

    /* get the date and time */
    private String getTimeStamp(){
        //getting current date and time using Date class
        //DateFormat df = new DateFormat();
        Date dateobj = new Date();
        //return df.format(dateobj);
        return null;
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


    /**should be called when startbutton is clicked on mainActivity */
    private void onStartBtnClicked() {
        /* get initial metadata and put in a hashmap */
        boolean putMetaDataSuccessful = false;
        if (putMetaDataSuccessful = putMetaData()) {

            final Thread workerThread = new Thread(mSpeedTester);
            workerThread.start();

            /* make the thread timeout after a certain time */
            mCountDownTimer = new CountDownTimer(1000 * TIME_LIMIT, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                }

                @Override
                public void onFinish() {
                    workerThread.interrupt();
                    Log.d(TAG, "countdown finished"); //$NON-NLS-1$

                    /* send back to UI the outcome of timer  */
                    Bundle b = new Bundle();
                    b.putBoolean("", isContinuous);
                    resultReceiver.send(TIMED_OUT, b);
//
//                    if (!isContinuous) {
//
//                        Bundle b = new Bundle();
//                        b.putStringArray("methods", new String[]{SpeedTestLauncher.FUNCTN_SHOW_POPUP});
//                        b.putInt("mode", 1);
//                        resultReceiver.send(SpeedTestLauncher.MSG_CALL_FUNCTION, b);
//
//
//
//                    } else {
//                        Toast t = Toast.makeText(DataGatherService.this, "Test timed out.", Toast.LENGTH_SHORT);
//                        t.show();
//                    }

                }
            };
            mCountDownTimer.start();
        }

        /* send back to UI the outcome of putMetadata   */
        Bundle b = new Bundle();
        b.putBoolean("", putMetaDataSuccessful);
        resultReceiver.send(PUT_META_DATA, b);


    }

    /* forward the message to the activity */

    private void forwardMessageToActivity( Message msg, Bundle b ){
        if ( b == null ) {
            b = new Bundle();
        }

        b.putParcelable(String.valueOf(RECEIVED_FROM_TESTER), msg);
        resultReceiver.send(RECEIVED_FROM_TESTER, b);
    }



    public final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(final Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_STATUS:
                    final SpeedTester.SpeedInfo info1 = (SpeedTester.SpeedInfo) msg.obj;

                    /* send the message back to the activity for UI purposes */
                    forwardMessageToActivity(msg, null);


                    //mCustomProgressBar.setProgress(100 * msg.arg1);
                    //callSpeedTestLauncherMethod("mCustomProgressBar.setProgress", "", 100 * msg.arg1);

//                    float progress = 100 * msg.arg1;
//                    Bundle b = new Bundle();
//                    b.putStringArray("methods", new String[]{SpeedTestLauncher.SET_PROGRESS_BAR});
//                    b.putFloat("param", progress);
//                    resultReceiver.send(SpeedTestLauncher.MSG_CALL_FUNCTION, b);


                    break;
                case MSG_UPDATE_CONNECTION_TIME:
                    //mTxtConnectionSpeed.setText(String.format(getResources().getString(R.string.update_connectionspeed), msg.arg1));
                    mConnectionTime = msg.arg1;
                    forwardMessageToActivity(msg, null);
                    break;
                case MSG_COMPLETE_STATUS:

                    /* change the ui and play a sound */
                    //changeUI(UI_MODE_NOT_TESTING);

                    /* play a sound depending on whether complete or not */
                    if (msg.arg2 == 1) {
                        //playSound(SOUND_TEST_COMPLETE);
                    } else if (msg.arg2 == 0) {
                        //playSound(SOUND_ERROR);

                    /* connection timeout error  do not continue */
                    } else if (msg.arg2 == -1) {
                        //playSound(SOUND_ERROR);
                        //showPopup("", 3);

                        //db.saveRecord(data);
                        //data = null;
                        break;
                    }


                    final SpeedTester.SpeedInfo info2 = (SpeedTester.SpeedInfo) msg.obj;

				/* store data in hashmap */

				/* download speed and total bytes downloaded */
                    data.put(FeedReaderDbHelper.SPEED_STRING, info2.megabits + "");
                    data.put(FeedReaderDbHelper.BYTES_STRING, msg.arg1 + "");

				/* connection time in ms */
                    data.put(FeedReaderDbHelper.CONNTIME_STRING, mConnectionTime + "");
                    data.put(FeedReaderDbHelper.CONNTIMEUNIT_STRING, "ms");


                    //Bundle toSend = new Bundle();
                    ArrayList<String> methodsToCall = new ArrayList<>();

				/* prepare data for user */
                    //prepareData();


                /* store data in db */
                    db.saveRecord(data);
                    String uuid = data.get(FeedReaderDbHelper.ID_STRING);
                    //removeRecord(uuid);


                    /* tell super service to transfer records */
                    if( !mTransferInProgress ){
                        transferRecords();
                    }

                    Bundle b = new Bundle();
                    b.putSerializable("data", data);

                    /* inform ui test is complete */
                    forwardMessageToActivity(msg, b);

                    /* keep testing even if activity in not bound */
                    if ( activityStopped && isContinuous ) onStartBtnClicked();



                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    };

    /** startTestFromService
     *      called when the service is starting its own test
     *      (as opposed to the activity starting it like with a buttonclick)
     */
    private void startTestFromService(){

    }









}