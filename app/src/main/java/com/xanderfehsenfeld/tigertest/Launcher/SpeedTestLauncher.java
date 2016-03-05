/*
	This file is part of SpeedTest.

    SpeedTest is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    SpeedTest is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with SpeedTest.  If not, see <http://www.gnu.org/licenses/>.

 */
package com.xanderfehsenfeld.tigertest.Launcher;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.xanderfehsenfeld.tigertest.DataGatherService;
import com.xanderfehsenfeld.tigertest.LocalDB.FeedReaderDbHelper;
import com.xanderfehsenfeld.tigertest.LocalDB.MyDbWrapper;
import com.xanderfehsenfeld.tigertest.MySoundPlayer;
import com.xanderfehsenfeld.tigertest.R;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * Test speed of our network connection
 * @author Xander Fehsenfeld
 * @version 1.0
 *
 */
public class SpeedTestLauncher extends Activity {

    public static final int MSG_ACTIVITY_STATUS_CHANGE = 7;


    /* contants for the method 'playsound' */
    public static int SOUND_TEST_STARTED = 0;
    public static int SOUND_TEST_COMPLETE = 1;
    public static int SOUND_ERROR = 2;
    public static int SOUND_POPUP_SHOW = 3;
    public static int SOUND_POPUP_HIDE = 4;
    public static int SOUND_CLICK_DOWN = 5;

    public static final Class SERVICE_CLASS = DataGatherService.class;

    /* connection timeout of speed tester */
    protected static int CONNECT_TIMEOUT = 5000;


    /* constants for method 'changeUI' */
    public static final int UI_MODE_TESTING = 0;
    public static final int UI_MODE_NOT_TESTING = 1;
    public static int MSG_CALL_FUNCTION = -1;

    protected Button mBtnStart;
    protected TextView mResultViewer;

    /* animations */
    protected Animation animationFlyIn;
    public Animation animSpin;
    protected Animation scrollerFlyIn;
    //protected LinkedList<String> textAnim;
    protected LinkedList<HorizontalScrollView> scrollersToAnimate;

    //Private fields
    public static final String TAG = SpeedTestLauncher.class.getSimpleName();


    private float ratioHeightBtnParent;
    public float ratioHeightSpacer;

    /* popup window */
    PopupWindow pwindo;

    /* HashMap to store resultant data */
    HashMap<String, String> data;

    protected static final int MSG_UPDATE_STATUS = 0;
    protected static final int MSG_UPDATE_CONNECTION_TIME = 1;
    protected static final int MSG_COMPLETE_STATUS = 2;
    protected final static int UPDATE_THRESHOLD = 200;


    protected HorizontalScrollView network_scroller;
    protected HorizontalScrollView downspeed_scroller;
    protected HorizontalScrollView ping_scroller;
    protected HorizontalScrollView mSettingsBtnScroller;

    /* progress bar */
    ProgressBar mCustomProgressBar;

    protected ScrollView mTopScroller;

    protected boolean mRaining = false;

    /* store the screen dimensions */
    public Point screenDimens;
    protected Animation animFadeIn;
    protected Animation animFadeOut;


    /* for binding with service */
    private int mBindFlag;
    protected Messenger mServiceMessenger;

    /* time limit of test in seconds */
    public static int timeLimit = 5;

    /* preferences */
    MySharedPrefsWrapper prefs;

    /* A service connection to connect this Activity with the speech recognition service */
    public final ServiceConnection mServiceConnection = new ServiceConnection()
    {
        public static final boolean DEBUG = true;

        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            if (DEBUG) {Log.d(TAG, "onServiceConnected");} //$NON-NLS-1$
            mServiceMessenger = new Messenger(service);
            Message msg = Message.obtain(null, DataGatherService.MSG_SENT_SHARED_PREFS, 0, 0, prefs);

            try
            {
                //mServerMessenger.send(msg);
                mServiceMessenger.send(msg);
            }
            catch (RemoteException e)
            {
                e.printStackTrace();
            }

            /* inform service that activity is running */
            //onActivityStatusChanged(false);
            prefs.prefEditer.putBoolean(prefs.PREF_ACTIVITY_RUNNING, true);
            prefs.prefEditer.commit();

        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            if (DEBUG) {
                Log.d(TAG, "onServiceDisconnected");} //$NON-NLS-1$
            mServiceMessenger = null;
        }

    }; // mServiceConnection

    MyResultReceiver resultReceiver;

    /* local data base */
    MyDbWrapper db;
    private FeedReaderDbHelper mDbHelper;

    protected CountDownTimer mCountDownTimer;
    protected Button mBtnSettings;
    protected PopupWindow settingsPwindo;

    /* whether or not to test again after complete testing */
	protected boolean isContinuous = false;

    MySoundPlayer mySoundPlayer;

    /* TODO remove after Testing */
    Tests tests;
    private TextView loader;


    /** Called when the activity is first created. */
	//@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);


		/* get screenDimens width and height */
		screenDimens = new Point();
		Display display = getWindowManager().getDefaultDisplay();
		display.getSize(screenDimens);

        /* control look of layout using ratio of screen height */
        ratioHeightBtnParent = .8f;
        ratioHeightSpacer = .75f * (1 - ratioHeightBtnParent);

        //Request the progress bar to be shown in the title
		requestWindowFeature(Window.FEATURE_PROGRESS);
		setContentView(R.layout.main);

		mResultViewer = (TextView)findViewById(R.id.resultviewer);
		mResultViewer.setText("");


        /* initialize custom progeress bar */
        mCustomProgressBar = (ProgressBar) findViewById(R.id.progressBar1);
        findViewById(R.id.progress_bar_container).setMinimumHeight((int) (ratioHeightSpacer * screenDimens.y));

        /* resize start button */
        int radius = (int) (screenDimens.x/3.5f);
        Button b = ((Button)findViewById(R.id.btnStart));
        loader = ((TextView)findViewById(R.id.loader));
        RelativeLayout.LayoutParams relativeLayoutParams = new RelativeLayout.LayoutParams(radius * 2, radius * 2);
        relativeLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
//        b.setWidth(radius * 2);
//        b.setHeight(radius * 2);
        b.setLayoutParams(relativeLayoutParams);
        relativeLayoutParams = new RelativeLayout.LayoutParams(radius * 2 + 50, radius * 2 + 50);
        relativeLayoutParams.addRule( RelativeLayout.CENTER_IN_PARENT );
        loader.setLayoutParams( relativeLayoutParams );



//        loader.setMinimumWidth( radius * 2 + 10 );
//        loader.setMinimumHeight( radius * 2 + 10 );

        /* adjust text size */
        Paint paint = b.getPaint();
        b.setMaxLines(1);
        b.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        String text = "" + b.getText();
        Rect r = new Rect();
        paint.getTextBounds(text, 0, text.length(), r);
        float ratio = (float)( radius * 1.5 ) / r.width();
        b.setTextSize(TypedValue.COMPLEX_UNIT_PX, b.getTextSize() * ratio);


        (findViewById(R.id.btn_settings_scroller)).setMinimumWidth(screenDimens.x * 2);
        (findViewById(R.id.btn_settings_container)).setMinimumWidth(screenDimens.x * 2);
        mBtnSettings = (Button)findViewById(R.id.btn_settings);
        mBtnSettings.setLayoutParams(new LinearLayout.LayoutParams(screenDimens.x / 5, screenDimens.x / 5));


        /* get a database helper */
        mDbHelper = new FeedReaderDbHelper(getApplicationContext());

        // Gets the data repository in write mode
        db = new MyDbWrapper(mDbHelper.getWritableDatabase());


        mySoundPlayer = new MySoundPlayer(this);



        /* start the service, which will be stopped in onDestroy */
        startMyService();
        bindMyService();

        initPrefs();

    }

    private void initPrefs(){
        prefs = new MySharedPrefsWrapper(this);

    }



    /**dissMissAllPopups
     *      close any popups which are open
     * @param v
     */
    public void dissMissAllPopups( View v ){
        System.out.println("dissMissAllPopups");
        if (pwindo != null )  pwindo.dismiss();
        if (settingsPwindo != null ) settingsPwindo.dismiss();
    }

    /* VISUAL UI CODE */


    /** showPopup
     *
     * @param badNetwork the name of the network currently connected to
     */
    protected void showPopup(String badNetwork, int mode){

	    /* get the viewGroup to be displayed on the popup */
        RelativeLayout layout = (RelativeLayout) pwindo.getContentView();

        /* set the error message on the popup */
        TextView errorMessage = (TextView) layout.findViewById(R.id.errorMessage);

        /* grey out background */
        findViewById(R.id.grey_out).setVisibility(View.VISIBLE);

        if ( mode == 0 ) {

            errorMessage.setText("Invalid network: " + badNetwork + ". Valid networks: WiOfTheTiger, WiOfTheTiger-Employee.");

            /* display popup + play sound*/
            pwindo.showAtLocation(layout, Gravity.CENTER, 0, 0);
        } else if ( mode == 1 ){
            errorMessage.setText("TEST TIMED OUT. The test did not complete before the time limit. Go to settings to change time limit. Note: data will still be sent to server");
            Button wifiSettings = (Button)layout.findViewById(R.id.settings_btn);
            wifiSettings.setVisibility(View.GONE);

            /* display popup + play sound*/
            pwindo.showAtLocation(layout, Gravity.CENTER, 0, 0);
        } else if (mode == 2){
            layout = (RelativeLayout) settingsPwindo.getContentView();
            TextView records = (TextView) layout.findViewById(R.id.records_in_db);
            records.setText( "LOCAL DB SIZE: " + db.getRecordCount() + ", " + "TESTS: " + prefs.getTestCount() );
            settingsPwindo.showAtLocation(layout, Gravity.CENTER, 0, 0);

        }else if ( mode == 3 ) {
            errorMessage.setText("TEST TIMED OUT. The test did not complete because the initial connection timed out. Go to settings to change connection timeout. Note: data will NOT be sent to server");
            Button wifiSettings = (Button) layout.findViewById(R.id.settings_btn);
            wifiSettings.setVisibility(View.GONE);

            /* display popup + play sound*/
            pwindo.showAtLocation(layout, Gravity.CENTER, 0, 0);

        }

        playSound(SOUND_POPUP_SHOW);

    }

    /** adjustTextView
     *      adjust tje size of the text in a textview so it is the screen's width
     * @param toAdd
     */
    protected void adjustTextView(TextView toAdd){
        /* ratio of current width to screen width */
        Paint paint = toAdd.getPaint();

        String text = "" + toAdd.getText();

        Rect r = new Rect();
        paint.getTextBounds(text, 0, text.length(), r);
        float ratio = (float)(screenDimens.x * .8) / r.width();
        toAdd.setTextSize(TypedValue.COMPLEX_UNIT_PX, Math.min(toAdd.getTextSize() * ratio, (screenDimens.y * ratioHeightSpacer))/2);
    }


    @Override
    protected void onStart() {
        Log.d(TAG, "onStart");


        changeUI(UI_MODE_NOT_TESTING);
        super.onStart();





    }

    @Override
    protected void onRestart() {
        Log.d(TAG, "onRestart");


        changeUI(UI_MODE_NOT_TESTING);
        super.onRestart();

        /* activity not stopped */
        //onActivityStatusChanged(false);
        prefs.prefEditer.putBoolean(prefs.PREF_ACTIVITY_RUNNING, true);
        prefs.prefEditer.commit();


    }

    /** onActivityStatusChanged
     *      called when it is important to inform the service the activity's status has changed
     * @param isStopped whether or not the activity is started
     */
    private void onActivityStatusChanged(boolean isStopped){
//        Message activityIsStopped = Message.obtain(null, MSG_ACTIVITY_STATUS_CHANGE, 0, 0, new Boolean(isStopped));
//        try {
//            mServiceMessenger.send(activityIsStopped);
//        } catch (RemoteException e) {
//            e.printStackTrace();
//        }
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");

        super.onStop();

        /* whether or not is stopped */
        //onActivityStatusChanged(true);
        prefs.prefEditer.putBoolean(prefs.PREF_ACTIVITY_RUNNING, false);
        prefs.prefEditer.commit();



    }



    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        unbindMyService();
        stopMyService();

    }

    /** onSettingsChanged
     *      sends a message to the service to syncronize settings in the activity with those in the service
     *      should be called whenever a setting is changed
     */
    public void onSettingsChanged( ){
        /* params: handler, int what, int arg1, int arg2, object obj */
        Message changeSettings = Message.obtain(null, DataGatherService.MSG_SETTINGS_CHANGE, timeLimit, CONNECT_TIMEOUT, isContinuous);
        try {
            mServiceMessenger.send( changeSettings );
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (java.lang.NullPointerException e){
            //e.printStackTrace();
            Log.e(TAG, "" + e.getMessage());
        }
    }

    /* for binding to the service */
    /* Bind the the speech recog service */
    private void bindMyService(){
        Log.d(TAG, "bindMyService");

        /* setup result reciever */
        resultReceiver = new MyResultReceiver(null);
        Intent i = new Intent(this, SERVICE_CLASS);
        i.putExtra("receiver", resultReceiver);
        /* TODO remove after testing */
        i.putExtra(Tests.TEST_RECEIVER_STRING, tests.myTestResultResultReceiver);

        bindService(i, mServiceConnection, mBindFlag);

        isContinuous = false;
        onSettingsChanged();

    }
    /* unBind the the speech recog service */
    private void unbindMyService(){
        Log.d(TAG, "unbindMyService");

        //resultReceiver = null;
        if (mServiceMessenger != null)
        {
            unbindService(mServiceConnection);
            mServiceMessenger = null;
        }

    }

    /* stop and unbind with the service */
    private void stopMyService(){

        Intent service = new Intent(SpeedTestLauncher.this, SERVICE_CLASS);
        SpeedTestLauncher.this.stopService(service);
    }

    /* start the continuous speech service and bind to it */
    private void startMyService(){

        /* start the service */
        Intent service = new Intent(SpeedTestLauncher.this, SERVICE_CLASS);
        /* send the reciever to the service */
        service.putExtra("receiver", resultReceiver);
        //service.putExtra("score", currentScore);
        mBindFlag = Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH ? 0 : Context.BIND_ABOVE_CLIENT;
        SpeedTestLauncher.this.startService(service);


    }


    /** MyResultReceiver
     *      recieve result messages from the DataGatherService
     */
    public class MyResultReceiver extends ResultReceiver
    {

        public MyResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            if (resultCode == DataGatherService.RECEIVED_FROM_TESTER){
                /* process data if present */
                if (resultData.containsKey("data")){
                    data = (HashMap<String, String>) resultData.getSerializable("data");
                }
                mHandler.sendMessage((Message)resultData.getParcelable(String.valueOf(DataGatherService.RECEIVED_FROM_TESTER)));

            } else if (resultCode == DataGatherService.PUT_META_DATA){
                boolean putMetaDataSuccessful;
                if (putMetaDataSuccessful = resultData.getBoolean("")) {
                    playSound(SOUND_TEST_STARTED);
                } else changeUI(UI_MODE_NOT_TESTING);
                if (!putMetaDataSuccessful) Log.e(TAG, "putMetaData = " + putMetaDataSuccessful);
                else Log.d(TAG, "putMetaData = " + putMetaDataSuccessful);


            } else if (resultCode == DataGatherService.TIMED_OUT){
                boolean isContinuous;
                    if (!(isContinuous = resultData.getBoolean(""))) {

                        showPopup("", 1);

                    } else {
                        Toast t = Toast.makeText(SpeedTestLauncher.this, "Test timed out.", Toast.LENGTH_SHORT);
                        t.show();
                    }
                Log.e(TAG, "timed out. is continous = " + isContinuous);

            } else if (resultCode == DataGatherService.WIFI_CONNECTED_RESULT) {
                boolean wifiConnected;
                if (!(wifiConnected = resultData.getBoolean(""))) {

                    showPopup( "WIFI NOT CONNECTED", 0 );
                    playSound(SOUND_ERROR);
                    changeUI(UI_MODE_NOT_TESTING);

                }
                Log.e(TAG, "wifi connected: " + wifiConnected);

            } else if (resultCode == DataGatherService.IS_ALLOWED_NETWORK) {

                boolean allowedNetwork;
                String network = resultData.getString("s");
                if (!(allowedNetwork = resultData.getBoolean(""))) {
                    showPopup( network, 0 );
                    playSound(SOUND_ERROR);

                }
                Log.e(TAG, "network " + network + " is allowed: " + allowedNetwork);
            } else {
                boolean wasDeleted = resultData.getBoolean("wasDeleted");
                String uuid = resultData.getString(FeedReaderDbHelper.ID_STRING);
                Log.d(TAG, "received result from " + SERVICE_CLASS.getName() + ": " + uuid + ", " + wasDeleted);

            }
        }
    }



    protected final Handler mHandler=new Handler(){
        @Override
        public void handleMessage(final Message msg) {
            switch(msg.what){
                case MSG_UPDATE_STATUS:
                    final SpeedTester.SpeedInfo info1=(SpeedTester.SpeedInfo) msg.obj;

                    // Title progress is in range 0..10000
                    setProgress(100 * msg.arg1);
                    mCustomProgressBar.setProgress(100 * msg.arg1);

                    break;
                case MSG_UPDATE_CONNECTION_TIME:
                    break;
                case MSG_COMPLETE_STATUS:

                    /* change the ui and play a sound */
                    changeUI(UI_MODE_NOT_TESTING);

                    /* play a sound depending on whether complete or not */
                    if ( msg.arg2 == 1){
                        playSound(SOUND_TEST_COMPLETE);
                    } else if (msg.arg2 == 0){
                        playSound(SOUND_ERROR);

                    /* connection timeout error  do not continue */
                    } else if (msg.arg2 == -1 ) {
                        playSound(SOUND_ERROR);
                        showPopup("", 3);


                        break;
                    }

                    final SpeedTester.SpeedInfo info2 = (SpeedTester.SpeedInfo) msg.obj;

				/* prepare data for user */
                    prepareData();

				/* if raining animation is already started, then the above added items will rain */
                    if (!mRaining) startRainAnimation();
                    //mTopScroller.fullScroll(View.FOCUS_DOWN);
                    //mTopScroller.fling(3000);

                    /* on continuous mode, start the test again */
                    if (isContinuous) mBtnStart.performClick();

                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    };

    /**
     * Our Slave worker that does actually all the work
     */
    //protected final SpeedTester mSpeedTester = new SpeedTester(this);


    /** changeUI
     *      change the
     * @param mode
     *      UI_MODE_TESTING - the app appears to be carrying out a test
     *      UI_MODE_NOT_TESTING - app not carrying out a test
     */
    protected void changeUI(int mode){
        if (mode == UI_MODE_TESTING){
            setProgressBarVisibility(true);
            mCustomProgressBar.startAnimation(animFadeIn);
            mCustomProgressBar.setVisibility(View.VISIBLE);
            mResultViewer.setText("");

            /* remove the items from scroller that are not spacers */
            //int maxIndex = mResultContainer.getChildCount()-1;
            //int removeCount = Math.max(mResultContainer.getChildCount() - 6, 0);
            //mResultContainer.removeViews(maxIndex - removeCount + 1, removeCount);
            downspeed_scroller.setVisibility(View.INVISIBLE);
            ping_scroller.setVisibility(View.INVISIBLE);
            network_scroller.setVisibility(View.INVISIBLE);

            /* disable slider during test */
            ((SeekBar)settingsPwindo.getContentView().findViewById(R.id.time_limit_seekbar)).setEnabled(false);

            mBtnStart.setEnabled(false);
            mBtnStart.setText("TESTING...");
            mBtnStart.setBackground(getResources().getDrawable(R.drawable.circular_button_pressed));

            loader.startAnimation( animSpin );

        } else if ( mode == UI_MODE_NOT_TESTING){
            mCustomProgressBar.startAnimation(animFadeOut);
            mCustomProgressBar.setVisibility(View.INVISIBLE);
            mBtnStart.setEnabled(true);
            mBtnStart.setBackground(getResources().getDrawable(R.drawable.circular_button_background));
            mBtnStart.setText("START TEST");
            setProgressBarVisibility(false);

            /* re-enable slider */
            ((SeekBar)settingsPwindo.getContentView().findViewById(R.id.time_limit_seekbar)).setEnabled(true);

            loader.clearAnimation();

        }
    }

    /** prepareData
     * 		display select data to the user with hardcoded style
     */
	void prepareData(){
        //NOTE order matters! the text will appear in the scroller in the reverse order of the code

        // download speed
        String z = data.get(FeedReaderDbHelper.SPEED_STRING);
        if ( z.contains(".")) z = z.substring(0, z.indexOf(".")+2);
        TextView tv;
        tv = ((TextView)((RelativeLayout) downspeed_scroller.getChildAt(0)).getChildAt(0));
        tv.setText(z);
        adjustTextView(tv);
        scrollersToAnimate.add(downspeed_scroller);


        z = data.get(FeedReaderDbHelper.CONNTIME_STRING);
        tv = ((TextView)((RelativeLayout) ping_scroller.getChildAt(0)).getChildAt(0));
        tv.setText(z);
        adjustTextView(tv);
        scrollersToAnimate.add(ping_scroller);

        //network name
        z = data.get(FeedReaderDbHelper.NETWORK_STRING);
        tv = ((TextView)((RelativeLayout) network_scroller.getChildAt(0)).getChildAt(0));
        tv.setText(z);
        adjustTextView(tv);
        scrollersToAnimate.add(network_scroller);


    }


    /** startRainAnimation
     *      The "rain" animation is the animation of the views down into the scroller after a test is complete.
     *      in prepareData(), the scrollers are prepared and put in the queue 'scrollers to animate.'
     *      When this function is called, the first scroller is popped off the queue and animated. On animationEnd,
     *      if there is another scroller in the queue, then that one is animatated, and so on.
     *
     *      The final effect is the scrollers rain down in succession
     */
	void startRainAnimation(){

        if (!scrollersToAnimate.isEmpty()){
            HorizontalScrollView first = scrollersToAnimate.peekFirst();
            runOnUiThread(new UpdateUI(first));
        }
    }




    /** playSound
     *
     * @param sound the number to represent the sound
     */
    protected void playSound(int sound){
        mySoundPlayer.playSound(sound);
    }

    /** UpdateUI
     *      a runnable to perform a scrollview animation (usually called to operate on the UI thread )
     */
    class UpdateUI implements Runnable
    {
        HorizontalScrollView horizontalScrollView;

        public UpdateUI(HorizontalScrollView horizontalScrollView) {

            this.horizontalScrollView = horizontalScrollView;
        }
        public void run() {

            horizontalScrollView.startAnimation(scrollerFlyIn);
        }
    }



}