package com.xanderfehsenfeld.tigertest.Activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.RemoteException;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.xanderfehsenfeld.tigertest.R;
import com.xanderfehsenfeld.tigertest.Service.DataGatherService;

import java.util.LinkedList;

/**
 * Created by Xander on 2/22/16.
 */
public class MainActivity extends SpeedTestLauncher {

    /* vibrator */
    private Vibrator v;



    //private HorizontalScrollView mScroller;
    protected ScrollView mScroller;


    protected RelativeLayout mStartBtnContainer;



    public MainActivity(){


    }

    @Override
    public void onCreate(Bundle bundle) {

        /* TODO remove after testing */
        tests = new Tests(this);

        super.onCreate(bundle);

        mResultContainer = (LinearLayout) findViewById(R.id.resultContainer);
        mResultContainer.removeView(mResultViewer);


        mScroller = (ScrollView) findViewById(R.id.horizontalScrollView);
        mStartBtnContainer = (RelativeLayout) findViewById(R.id.topContainerA);
        //RelativeLayout mBottomContainer = (RelativeLayout) findViewById(R.id.start_btn_parent);

        /* top scroll view */
        mTopScroller = (ScrollView) findViewById(R.id.topScrollView);
        final ViewGroup temp = (ViewGroup)(mTopScroller.getChildAt(0));
        temp.setMinimumHeight((int) (1.5 * screenDimens.y));
        temp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTopScroller.fling(-5000);
            }
        });
        (temp.getChildAt(0)).setMinimumHeight(screenDimens.y);


        /* initialize the popup window */
        initErrorPopup();
        initSettingsPopup();


        bindListeners();



        /* initialize all animations and associated listeners */
        initAnimations();

        /* make start button parent a certain size */
        mResultContainer.setMinimumHeight((int) (screenDimens.y));

        /* add spacers to scroll view */
        populateScrollView();

    }


    private void startTest( int time_limit, int timeout, Object iscontinuous ){
        /* params: handler, int what, int arg1, int arg2, object obj */
        Message startTest = Message.obtain(null, DataGatherService.MSG_START_TEST, time_limit, timeout, iscontinuous);
        try {
            mServiceMessenger.send(startTest );
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (java.lang.NullPointerException e){
            //e.printStackTrace();
            Log.e(TAG, "" + e.getMessage());
        }
    }

    private void stopTest( ){
        /* params: handler, int what, int arg1, int arg2, object obj */
        Message stopTest = Message.obtain(null, DataGatherService.MSG_STOP_TEST);
        try {
            mServiceMessenger.send(stopTest );
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (java.lang.NullPointerException e){
            //e.printStackTrace();
            Log.e(TAG, "" + e.getMessage());
        }
    }



    /**
     * Setup event handlers and bind variables to values from xml
     */
    protected void bindListeners() {
        mBtnStart = (Button) mStartBtnContainer.findViewById(R.id.btnStart);

        mBtnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {

                if ( currentUIMode == UI_MODE_NOT_TESTING) {

                    playSound(SOUND_CLICK_DOWN);

                    changeUI(UI_MODE_TESTING);


                    startTest(timeLimit, CONNECT_TIMEOUT, new Boolean(isContinuous));

                /* in the middle of a test */
                } else if (currentUIMode == UI_MODE_TESTING) {
                    stopTest();
                }



            }
        });

        /* set up listener for settings button */
        mBtnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playSound(SOUND_CLICK_DOWN);

                showPopup("", 2);

                /* click the continuous toggle if it is on */
                ToggleButton mBtnContinous = (ToggleButton) settingsPwindo.getContentView().findViewById(R.id.switch_continuous);
                if (mBtnContinous != null && isContinuous) {
                    mBtnContinous.performClick();
                }
            }
        });

        /* do stuff when popups dismissed */
        PopupWindow.OnDismissListener myDismissListener = new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                findViewById(R.id.grey_out).setVisibility(View.GONE);
                playSound(SOUND_POPUP_HIDE);
            }
        };
        pwindo.setOnDismissListener(myDismissListener);
        settingsPwindo.setOnDismissListener(myDismissListener);

        /* when clicking on he back background around the start button, fling the scroller down */
        mStartBtnContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTopScroller.fling(5000);}});


    }

    /**initSettingsPopup
     *      initializes the popup for settings
     */
    private void initSettingsPopup(){

        /* get the viewGroup to be displayed on the popup */
        LayoutInflater inflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        RelativeLayout layout = (RelativeLayout) inflater.inflate(R.layout.settingspopupwindow, null);

        ViewGroup mSettingsBody = (ViewGroup) layout.findViewById(R.id.settings_body_container);
        final ToggleButton mSwitchContinuous = (ToggleButton) mSettingsBody.findViewById(R.id.switch_continuous);

        /* set settings popup to 2/3 height of screen */
        ( layout.findViewById(R.id.setting_layout_container)).setMinimumHeight((int) (screenDimens.y * .75));

        /* set up seekbar */
        final TextView mTimeLimitTv = (TextView) mSettingsBody.findViewById(R.id.time_limit_tv);
        final SeekBar mTimeLimitSb = (SeekBar) mSettingsBody.findViewById(R.id.time_limit_seekbar);
        mTimeLimitSb.setProgress(prefs.prefs.getInt(prefs.PREF_TIME_LIMIT, 30));
        mTimeLimitTv.setText("Test Time Limit: " + mTimeLimitSb.getProgress() + " seconds");

        final TextView mConnTimeOutTv = (TextView) mSettingsBody.findViewById(R.id.connect_timeout_tv);
        final SeekBar mConnTimeoutSb = (SeekBar) mSettingsBody.findViewById(R.id.connect_timeout_seekbar);
        mConnTimeoutSb.setProgress((int) Math.sqrt( prefs.prefs.getInt(prefs.PREF_CONNECT_TIME_LIMIT, 5)));
        mConnTimeOutTv.setText("Connection Timeout: " + mConnTimeoutSb.getProgress() * mConnTimeoutSb.getProgress() + "ms");


        /* seekbar listeners */
        SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (seekBar == mTimeLimitSb) {
                    if (progress < 30) seekBar.setProgress(30);
                    mTimeLimitTv.setText("Test Time Limit: " + seekBar.getProgress() + "seconds");
                    timeLimit = seekBar.getProgress();

                } else if ( seekBar == mConnTimeoutSb ){
                    if (progress < Math.sqrt(5000)) seekBar.setProgress((int) Math.sqrt(5000));
                    progress = mConnTimeoutSb.getProgress();
                    progress = progress * progress;
                    if (progress < 1000) {
                        mConnTimeOutTv.setText("Connection Timeout: " + progress + "ms");
                    } else {
                        mConnTimeOutTv.setText("Connection Timeout: " + progress / 1000 + "sec");
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                playSound(SOUND_CLICK_DOWN);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                onSettingsChanged();
                prefs.prefEditer.putInt(prefs.PREF_TIME_LIMIT, mTimeLimitSb.getProgress());
                prefs.prefEditer.putInt(prefs.PREF_CONNECT_TIME_LIMIT, (int) Math.pow(mConnTimeoutSb.getProgress(), 2));
                prefs.prefEditer.commit();
            }
        };
        mTimeLimitSb.setOnSeekBarChangeListener(seekBarChangeListener);
        mConnTimeoutSb.setOnSeekBarChangeListener(seekBarChangeListener);

        /* make errorpopupwindow the appropriate size */
        int width = screenDimens.x;
        int height = screenDimens.y;
        settingsPwindo = new PopupWindow(layout, width - 100, (int) (height/ 1.5), true);


        /* set scroller to halfway */
        ScrollView mSettingsScroller = (ScrollView) settingsPwindo.getContentView().findViewById(R.id.settings_scroller);
        mSettingsScroller.setMinimumHeight((int) (screenDimens.y / 2));
        mSettingsScroller.scrollTo(mSettingsScroller.getScrollX(), mSettingsScroller.getMaxScrollAmount() / 2);


        final ToggleButton mSwitchBlockInternet = (ToggleButton) mSettingsBody.findViewById(R.id.switch_block_internet);
        final ToggleButton mInfinitySwitch = (ToggleButton) findViewById(R.id.toggle_infinity);
        mSwitchBlockInternet.setChecked(prefs.prefs.getBoolean(prefs.PREF_INTERNET_ON, true));
        mConnTimeoutSb.setEnabled(prefs.prefs.getBoolean(prefs.PREF_INTERNET_ON, true));


        /* create operate on checked changed listeners */
        CompoundButton.OnCheckedChangeListener myOnCheckChangedListener = new CompoundButton.OnCheckedChangeListener() {

            private final String NOTIFY_CONTINOUS_ON = "Mode: CONTINUOUS test";
            private final String NOTIFY_CONTINOUS_OFF = "Mode: SINGLE test";
            private final String NOTIFY_INTERNET_OFF = "Internet BLOCKED";
            private final String NOTIFY_INTERNET_ON = "Internet ALLOWED";

            Toast t = Toast.makeText(MainActivity.this, "", Toast.LENGTH_SHORT);

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if ( buttonView == mSwitchContinuous || buttonView == mInfinitySwitch  ) {
                    if (isChecked) {
                        //isContinuous = true;
                        playSound(SOUND_POPUP_SHOW);
                        t.setText(NOTIFY_CONTINOUS_ON);
                    } else {
                        //isContinuous = false;
                        playSound(SOUND_POPUP_HIDE);
                        t.setText(NOTIFY_CONTINOUS_OFF);

                    }

                    /* synchronize the 2 buttons since they do the same thing */
                    mSwitchContinuous.setChecked(isChecked);
                    mInfinitySwitch.setChecked(isChecked);

                } else if (buttonView == mSwitchBlockInternet){
                    if (isChecked) {
                        playSound(SOUND_POPUP_SHOW);
                        mConnTimeoutSb.setEnabled(true);
                        mConnTimeoutSb.setProgress((int) Math.sqrt(timeLimit * 1000));
                        t.setText(NOTIFY_INTERNET_ON);
                    } else {
                        playSound(SOUND_POPUP_HIDE);
                        mConnTimeoutSb.setProgress((int) Math.sqrt(1));
                        mConnTimeoutSb.setEnabled(false);
                        t.setText(NOTIFY_INTERNET_OFF);
                    }
                }
                t.show();
                prefs.prefEditer.putBoolean(prefs.PREF_INTERNET_ON, mSwitchBlockInternet.isChecked());
                prefs.prefEditer.putBoolean(prefs.PREF_IS_CONTINUOUS, mSwitchContinuous.isChecked());
                prefs.prefEditer.commit();
                onSettingsChanged();
            }
        };

        /* assign the listeners */
        mSwitchBlockInternet.setOnCheckedChangeListener(myOnCheckChangedListener);
        mSwitchContinuous.setOnCheckedChangeListener(myOnCheckChangedListener);
        mInfinitySwitch.setOnCheckedChangeListener(myOnCheckChangedListener);


        /* aniamation */
        settingsPwindo.setAnimationStyle(R.style.Animation);

        /* create onclick listener to close the popup */
        View.OnClickListener cl = new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                settingsPwindo.dismiss();
            }
        };

		/* make so clicking on the popup or circular_button_background closes the window */
        layout.setOnClickListener(cl);

        settingsPwindo.setOutsideTouchable(true);
    }


    /** initErrorPopup
     * 		set up the popup view for later use
     */
    private void initErrorPopup(){
		/* get the viewGroup to be displayed on the popup */
        LayoutInflater inflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        RelativeLayout layout = (RelativeLayout) inflater.inflate(R.layout.errorpopupwindow, null);

        RelativeLayout settingsContainer = (RelativeLayout) layout.findViewById(R.id.setting_layout_container);

		/* make errorpopupwindow the appropriate size */
        int width = screenDimens.x;
        int height = screenDimens.y;
        pwindo = new PopupWindow(layout, width - 100, (int) (height/ 1.5), true);


		/* aniamation */
        pwindo.setAnimationStyle(R.style.Animation);

		/* create onclick listener to close the popup */
        View.OnClickListener cl = new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                pwindo.dismiss();
            }
        };

		/* make so clicking on the popup or circular_button_background closes the window */
        Button btn_closepopup = (Button)layout.findViewById(R.id.close_popup_btn);
        btn_closepopup.setOnClickListener(cl);
        layout.setOnClickListener(cl);

		/* map the settings button */
        Button btn_settings = (Button) layout.findViewById(R.id.settings_btn);
        btn_settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

				/* go to WIFI settings */
                startActivityForResult(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS), 0);
            }
        });

        pwindo.setOutsideTouchable(true);
        //pwindo.setFocusable(true);
    }

    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        playSound(SOUND_CLICK_DOWN);

        /* show settings popup */
        if ( settingsPwindo.isShowing() ) settingsPwindo.dismiss();
        else showPopup("", 2);


        return super.onMenuOpened(featureId, menu);
    }


    @Override
    public void onBackPressed(){
        playSound(SOUND_CLICK_DOWN);
        if ( settingsPwindo.isShowing() ) settingsPwindo.dismiss();
        if ( pwindo.isShowing() ) settingsPwindo.dismiss();

    }
    /**initAnimations
     *      initialize the animations and associated listeners
     *
     */
    private void initAnimations(){

        /* data structures to keep track of text and color */
        //textAnim = new LinkedList();
        viewsToAnimate = new LinkedList<>();
        //textAndColor = new HashMap<>();

        v = (Vibrator) MainActivity.this.getSystemService(Context.VIBRATOR_SERVICE);

        flyInFromLeft = new ScaleAnimation(0, 1, 0, 1, Animation.RELATIVE_TO_PARENT, .5f, Animation.ABSOLUTE, -1000);
        flyInFromLeft.setDuration(300);
        Animation.AnimationListener repeater;
        flyInFromLeft.setAnimationListener( repeater = new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {

                /* vibrate for 250 ms */
                v.vibrate(250);

                if (!viewsToAnimate.isEmpty()) {

                    View last = viewsToAnimate.pop();
                    last.clearAnimation();


                    if (!viewsToAnimate.isEmpty()) {
                        View next = viewsToAnimate.peekFirst();
                        //next.startAnimation(flyInFromLeft);
                        runOnUiThread(new UpdateUI(next));
                    }

                    last.setVisibility(View.VISIBLE);
                }


            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });



		/* load a flyin animation to animate text views */
        animationFlyIn = new ScaleAnimation(0, 1, 0, 1, Animation.RELATIVE_TO_PARENT, .5f, Animation.ABSOLUTE, -1000);
        animationFlyIn.setDuration(300);

        /* fade in and fade out animations */
        animFadeIn = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_in);
        animFadeIn.setAnimationListener(repeater);
        animFadeOut = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_out);

        animSpin = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.spinning);

    }

    /* add empty textviews to use as spacers */
    protected void populateScrollView(){

        tv_network = (TextView)findViewById(R.id.tv_network);
        tv_ping = (TextView)findViewById(R.id.tv_ping);
        tv_downspeed = (TextView)findViewById(R.id.tv_downspeed);
        tv_test_number = (TextView)findViewById(R.id.tv_test_number);

        /* make everything in the main scroll view invisible initially */
        for ( int i = 0 ; i < mResultContainer.getChildCount(); i ++){
            mResultContainer.getChildAt(i).setVisibility(View.INVISIBLE);
        }


        //mTopScroller.setSmoothScrollingEnabled(true);
        mSettingsBtnScroller = (HorizontalScrollView)findViewById(R.id.btn_settings_scroller);

        /* make ui changes in response to changes in the scroll amount */
        ViewTreeObserver.OnScrollChangedListener listener;
        mTopScroller.getViewTreeObserver().addOnScrollChangedListener(listener = new ViewTreeObserver.OnScrollChangedListener() {

            int lastScrollY = mTopScroller.getScrollY();

            @Override
            public void onScrollChanged() {

                int deltaY = mTopScroller.getScrollY() - lastScrollY;
                lastScrollY = mTopScroller.getScrollY();
                mTopScroller.scrollTo(mTopScroller.getScrollX(), mTopScroller.getScrollY() + deltaY);
                mScroller.scrollTo(mScroller.getScrollX(), mScroller.getScrollY() + deltaY);

                /* percent of the whole scoller scrolled */
                /* expand views apart as you scroll up */
                float percent_scrolled = (float) mTopScroller.getScrollY() / (float) mTopScroller.getMaxScrollAmount();
                int goalHeight = (int) ((float) screenDimens.y / 5f);
                float multiplier = goalHeight - (screenDimens.y * ratioHeightSpacer);
                int newminHeight = (int) (((screenDimens.y * ratioHeightSpacer) / 2) + (percent_scrolled * multiplier));

                /* scroll settings button in depending on location of start button */
                mSettingsBtnScroller.scrollTo((int) (percent_scrolled * mSettingsBtnScroller.getMaxScrollAmount()), mSettingsBtnScroller.getScrollY());

                for (int i = 0; i < mResultContainer.getChildCount(); i ++){
                    mResultContainer.getChildAt(i).setMinimumHeight(newminHeight);
                }

            }
        });
        listener.onScrollChanged();

    }

}
