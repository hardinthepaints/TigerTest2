package com.xanderfehsenfeld.tigertest.Launcher;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
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

import com.xanderfehsenfeld.tigertest.Permissions;
import com.xanderfehsenfeld.tigertest.R;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Created by Xander on 2/22/16.
 */
public class MainActivity extends SpeedTestLauncher {


    /* vibrator */
    private Vibrator v;

    /* container of bottom scroller */
    protected LinearLayout mResultContainer;



    //private HorizontalScrollView mScroller;
    protected ScrollView mScroller;
    protected ScrollView mTopScroller;

    protected RelativeLayout mStartBtnContainer;


    public MainActivity(){


    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        mResultContainer = (LinearLayout) findViewById(R.id.resultContainer);
        mResultContainer.removeView(mResultViewer);


        mScroller = (ScrollView) findViewById(R.id.horizontalScrollView);
        mStartBtnContainer = (RelativeLayout) findViewById(R.id.topContainerA);
        //RelativeLayout mBottomContainer = (RelativeLayout) findViewById(R.id.start_btn_parent);

        /* top scroll view */
        mTopScroller = (ScrollView) findViewById(R.id.topScrollView);
        ViewGroup temp = (ViewGroup)(mTopScroller.getChildAt(0));
        temp.setMinimumHeight(2 * screenDimens.y);
        (temp.getChildAt(0)).setMinimumHeight(screenDimens.y);


        /* initialize the popup window */
        initPopup();
        initSettingsPopup();


        bindListeners();



        /* initialize all animations and associated listeners */
        initAnimations();

//        RelativeLayout spacer = new RelativeLayout(SpeedTestLauncher.this);
//        spacer.setMinimumHeight( screenDimens.y);
//        mResultContainer.addView(spacer);

        /* make start button parent a certain size */
        //mBottomContainer.setMinimumHeight((int) (1.1 * ratioHeightBtnParent * screenDimens.y));
        mResultContainer.setMinimumHeight((int) (1.5 * screenDimens.y));

        /* add spacers to scroll view */
        populateScrollView();
        // do extra stuff on your resources, using findViewById on your layout_for_activity1

        /* TODO remove after testing */
        tests = new Tests(this);
        //t.runTests();

//        String toLog = "";
//        try {
//            t.setMobileDataEnabled(this, false);
//        } catch (Exception e ){
//            toLog += e.getMessage();
//        }
//        Log.e(TAG, toLog);


    }



    /**
     * Setup event handlers and bind variables to values from xml
     */
    protected void bindListeners() {
        mBtnStart = (Button) mStartBtnContainer.findViewById(R.id.btnStart);

        mBtnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {

                playSound(SOUND_CLICK_DOWN);

                changeUI(UI_MODE_TESTING);

				/* get initial metadata and put in a hashmap */
                if (putMetaData()) {

                    final Thread workerThread = new Thread(mSpeedTester);
                    workerThread.start();

                    /* make the thread timeout after a certain time */
                    mCountDownTimer = new CountDownTimer(1000 * timeLimit, 1000) {
                        @Override
                        public void onTick(long millisUntilFinished) {
                        }

                        @Override
                        public void onFinish() {
                            workerThread.interrupt();
                            Log.d(TAG, "countdown finished"); //$NON-NLS-1$

                            if (!isContinuous) {
                                showPopup("", 1);
                            } else {
                                Toast t = Toast.makeText(MainActivity.this, "Test timed out.", Toast.LENGTH_SHORT);
                                t.show();
                            }
                            //playSound(2);
                        }
                    };
                    mCountDownTimer.start();


                    playSound(SOUND_TEST_STARTED);

                } else changeUI(UI_MODE_NOT_TESTING);


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
        TextView records = (TextView) mSettingsBody.findViewById(R.id.records_in_db);

        /* set settings popup to 2/3 height of screen */
        ((ViewGroup) layout.findViewById(R.id.setting_layout_container)).setMinimumHeight((int) (screenDimens.y * .75));

        /* make settings body extra long so it can scroll */
        //mSettingsBody.setMinimumHeight((int) (screenDimens.y));


        /* set up seekbar */
        final TextView mTimeLimitTv = (TextView) mSettingsBody.findViewById(R.id.time_limit_tv);
        final SeekBar mTimeLimitSb = (SeekBar) mSettingsBody.findViewById(R.id.time_limit_seekbar);
        mTimeLimitSb.setProgress(timeLimit);
        mTimeLimitTv.setText("Test Time Limit: " + mTimeLimitSb.getProgress() + " seconds");

        final TextView mConnTimeOutTv = (TextView) mSettingsBody.findViewById(R.id.connect_timeout_tv);
        final SeekBar mConnTimeoutSb = (SeekBar) mSettingsBody.findViewById(R.id.connect_timeout_seekbar);
        mConnTimeoutSb.setProgress((int) Math.sqrt(mSpeedTester.connectTimeout));
        mConnTimeOutTv.setText("Connection Timeout: " + mConnTimeoutSb.getProgress() * mConnTimeoutSb.getProgress() + "ms");


        /* seekbar listeners */
        SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress < 1) seekBar.setProgress(1);
                if (seekBar == mTimeLimitSb) {
                    mTimeLimitTv.setText("Test Time Limit: " + seekBar.getProgress() + "seconds");
                    timeLimit = seekBar.getProgress();

                } else if ( seekBar == mConnTimeoutSb ){
                    progress = progress * progress;
                    if (progress < 1000) {
                        mConnTimeOutTv.setText("Connection Timeout: " + progress + "ms");
                    } else {
                        mConnTimeOutTv.setText("Connection Timeout: " + progress / 1000 + "sec");
                    }
                    mSpeedTester.connectTimeout = progress;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                playSound(SOUND_CLICK_DOWN);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        };
        mTimeLimitSb.setOnSeekBarChangeListener(seekBarChangeListener);
        mConnTimeoutSb.setOnSeekBarChangeListener(seekBarChangeListener);

        /* make errorpopupwindow the appropriate size */
        int width = screenDimens.x;
        int height = screenDimens.y;
        settingsPwindo = new PopupWindow(layout, width - 100, (int) (height/ 1.5), true);
        //settingsPwindo = new PopupWindow(layout, width, height, true);


        /* set scroller to halfway */
        ScrollView mSettingsScroller = (ScrollView) settingsPwindo.getContentView().findViewById(R.id.settings_scroller);
        mSettingsScroller.setMinimumHeight((int) (screenDimens.y / 2));
        mSettingsScroller.scrollTo(mSettingsScroller.getScrollX(), mSettingsScroller.getMaxScrollAmount() / 2);


        final ToggleButton mSwitchBlockInternet = (ToggleButton) mSettingsBody.findViewById(R.id.switch_block_internet);
        mSwitchBlockInternet.setChecked(Permissions.INTERNET_ACCESS);
        mConnTimeoutSb.setEnabled(Permissions.INTERNET_ACCESS);


        /* create operate on checked changed listeners */
        CompoundButton.OnCheckedChangeListener myOnCheckChangedListener = new CompoundButton.OnCheckedChangeListener() {

            private final String NOTIFY_CONTINOUS_ON = "Mode: CONTINUOUS test";
            private final String NOTIFY_CONTINOUS_OFF = "Mode: SINGLE test";
            private final String NOTIFY_INTERNET_OFF = "Internet BLOCKED";
            private final String NOTIFY_INTERNET_ON = "Internet ALLOWED";

            Toast t = Toast.makeText(MainActivity.this, "", Toast.LENGTH_SHORT);

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if ( buttonView == mSwitchContinuous ) {
                    if (isChecked) {
                        isContinuous = true;
                        playSound(SOUND_POPUP_SHOW);
                        t.setText(NOTIFY_CONTINOUS_ON);
                    } else {
                        isContinuous = false;
                        playSound(SOUND_POPUP_HIDE);
                        t.setText(NOTIFY_CONTINOUS_OFF);

                    }

                } else if (buttonView == mSwitchBlockInternet){
                    if (isChecked) {
                        Permissions.INTERNET_ACCESS = true;
                        playSound(SOUND_POPUP_SHOW);
                        mConnTimeoutSb.setEnabled(true);
                        mConnTimeoutSb.setProgress((int) Math.sqrt(timeLimit * 1000));
                        t.setText(NOTIFY_INTERNET_ON);
                    } else {
                        Permissions.INTERNET_ACCESS = false;
                        playSound(SOUND_POPUP_HIDE);
                        mConnTimeoutSb.setProgress((int) Math.sqrt(1));
                        mConnTimeoutSb.setEnabled(false);
                        t.setText(NOTIFY_INTERNET_OFF);
                    }
                }
                t.show();
            }
        };
        mSwitchBlockInternet.setOnCheckedChangeListener(myOnCheckChangedListener);
        mSwitchContinuous.setOnCheckedChangeListener(myOnCheckChangedListener);


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
        settingsPwindo.setFocusable(true);
    }


    /** initPopup
     * 		set up the popup view for later use
     */
    private void initPopup(){
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
                // TODO Auto-generated method stub
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
        pwindo.setFocusable(true);
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
     *      initialize the animations:
     *
     */
    private void initAnimations(){

        /* data structures to keep track of text and color */
        //textAnim = new LinkedList();
        scrollersToAnimate = new LinkedList<>();
        //textAndColor = new HashMap<>();

        v = (Vibrator) MainActivity.this.getSystemService(Context.VIBRATOR_SERVICE);

        scrollerFlyIn = new ScaleAnimation(0, 1, 0, 1, Animation.RELATIVE_TO_PARENT, .5f, Animation.ABSOLUTE, -1000);
        scrollerFlyIn.setDuration(300);
        scrollerFlyIn.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {

                /* vibrate for 250 ms */
                v.vibrate(250);

                if (!scrollersToAnimate.isEmpty()) {

                    HorizontalScrollView last = scrollersToAnimate.pop();


                    if (!scrollersToAnimate.isEmpty()) {
                        HorizontalScrollView next = scrollersToAnimate.peekFirst();
                        //next.startAnimation(scrollerFlyIn);
                        runOnUiThread(new UpdateUI(next));
                    }

                    last.setVisibility(View.VISIBLE);
                    mScroller.fling(300);
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
        animFadeOut = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_out);
    }

    /* add empty textviews to use as spacers */
    protected void populateScrollView(){


        /* initialize scrollers */
        network_scroller = getNewItem("<network scroller>", getResources().getColor(R.color.networkTextColor));
        ping_scroller = getNewItem("<ping_scroller>", getResources().getColor(R.color.PingTextColor));
        downspeed_scroller = getNewItem("<downspeed_scroller>", getResources().getColor(R.color.downSpeedTextColor));

        /* labels */
        final TextView label_network = getLabel("<network>");
        final TextView label_speed = getLabel("<downspeed (megabits per sec)");
        final TextView label_ping = getLabel("<connection latency (ms)>");

        /* by default should be invisible */
        network_scroller.setVisibility(View.INVISIBLE);
        ping_scroller.setVisibility(View.INVISIBLE);
        downspeed_scroller.setVisibility(View.INVISIBLE);

        /* order they will appear */
        mResultContainer.addView(label_network);
        mResultContainer.addView(network_scroller);
        mResultContainer.addView(label_ping);
        mResultContainer.addView(ping_scroller);
        mResultContainer.addView(label_speed);
        mResultContainer.addView(downspeed_scroller);

        //mTopScroller.setSmoothScrollingEnabled(true);
        mSettingsBtnScroller = (HorizontalScrollView)findViewById(R.id.btn_settings_scroller);

        mTopScroller.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {

            int lastScrollY = mTopScroller.getScrollY();

            @Override
            public void onScrollChanged() {

                int deltaY = mTopScroller.getScrollY() - lastScrollY;
                lastScrollY = mTopScroller.getScrollY();
                mTopScroller.scrollTo(mTopScroller.getScrollX(), mTopScroller.getScrollY() + deltaY);
                mScroller.scrollTo(mScroller.getScrollX(), mScroller.getScrollY() + deltaY);

                /* percent of the whole scoller scrolled */
                /* expand views apart as you scroll up */
                //float percent_scrolled = (float)deltaY / (float)mScroller.getMaxScrollAmount();
                float percent_scrolled = (float) mTopScroller.getScrollY() / (float) mTopScroller.getMaxScrollAmount();
                int goalHeight = (int) ((float) screenDimens.y / 5f);
                float multiplier = goalHeight - (screenDimens.y * ratioHeightSpacer);
                int newminHeight = (int) ((screenDimens.y * ratioHeightSpacer) + (percent_scrolled * multiplier));

                /* scroll settings button in depending on location of start button */
                mSettingsBtnScroller.scrollTo((int) (percent_scrolled * mSettingsBtnScroller.getMaxScrollAmount()), mSettingsBtnScroller.getScrollY());


                downspeed_scroller.setMinimumHeight(newminHeight);
                network_scroller.setMinimumHeight(newminHeight);
                ping_scroller.setMinimumHeight(newminHeight);

                ArrayList<View> labels = new ArrayList<View>(){{
                    add(label_network);
                    add(label_ping);
                    add(label_speed);
                }};
                percent_scrolled = percent_scrolled / 2;
                float visibility_threshold = (float) .9;
                for ( View v : labels) {
                    if (percent_scrolled > visibility_threshold && (v.getVisibility() == View.INVISIBLE)){
                        v.setVisibility(View.VISIBLE);
                        v.startAnimation(animFadeIn);
                    } else if (percent_scrolled < visibility_threshold && (v.getVisibility() == View.VISIBLE)){
                        v.setVisibility(View.INVISIBLE);
                        v.startAnimation(animFadeOut);

                    }
                }




                //int scrollY = mTopScroller.getScrollY(); //for verticalScrollView

                //Log.d(TAG, "onScrollChanged: " + scrollY);
                //DO SOMETHING WITH THE SCROLL COORDINATES

            }
        });


    }

    /** getNewItem
     *      returns a view to put in the main scroll view
     * @param text the text in the view
     * @param c the color of the text
     * @return a new horizontal scroll view object
     */
    protected HorizontalScrollView getNewItem(String text, int c){
        /* make a text view to display in horizontal mScroller */
        TextView toAdd = new TextView(this);
        HorizontalScrollView hs = new HorizontalScrollView(this);
        RelativeLayout rl = new RelativeLayout(this);
        //rl.setGravity(Gravity.CENTER);
        rl.addView(toAdd);
        hs.addView(rl);


        /* set a minimum height */
        hs.setMinimumHeight((int) (ratioHeightSpacer * screenDimens.y));

        toAdd.setGravity(Gravity.CENTER);
        toAdd.setText(text);
        toAdd.setTextSize(mResultViewer.getTextSize());
        toAdd.setMovementMethod(new ScrollingMovementMethod());
        toAdd.setMaxLines(1);
        toAdd.setScrollbarFadingEnabled(true);
        toAdd.setTextColor(c);

		/* if it is a spacer, it will have no content */
        if( text.equals("")){
            hs.setVisibility(View.INVISIBLE);
        } else {
            toAdd.setBackgroundColor(getResources().getColor(R.color.textColorPrimary));

            /* adjust text size so it fits all in one line */
            adjustTextView(toAdd);

        }
        return hs;
    }

    /**getLabel
     *      create a text view with defualt invisibilty with the string content to use as a label
     * @param content
     * @return
     */
    protected TextView getLabel(String content){
        final TextView label = new TextView(this);
        label.setText(content);
        label.setTextColor(getResources().getColor(R.color.bloodRed));
        label.setVisibility(View.INVISIBLE);
        //label.setAlpha(0);
        return label;
    }



}
