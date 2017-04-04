package com.xanderfehsenfeld.tigertest.Activity;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
import android.util.Log;

import com.xanderfehsenfeld.tigertest.R;

import java.util.HashMap;

/**MySoundPlayer
 *      a class to play sound effects located in the res/raw directory
 */


public class MySoundPlayer {

    private static final String TAG = "MySoundPLayer";

    private SoundPool soundPool;
    float actVolume, maxVolume, volume;
    AudioManager audioManager;
    int counter;

    Activity a;

    HashMap<Integer, Boolean> isPlaying;
    HashMap<Integer, Boolean> isLoaded;

    /** Called when first created. */
    public MySoundPlayer(Activity a) {

        isPlaying = new HashMap<>();
        isLoaded = new HashMap<>();

        volume = .5f;

        //set context
        this.a = a;

        updateVolume();

        //Hardware buttons setting to adjust the media sound
        a.setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // the counter will help us recognize the stream id of the sound played  now
        counter = 0;

        // Load the sounds
        soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
        soundPool.setOnLoadCompleteListener(new OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                Log.d(TAG, "onLoadComplete");
                isLoaded.put(sampleId, true);
                //loaded = true;

            }
        });

        /* constants for the method 'playsound' */
        SpeedTestLauncher.SOUND_TEST_STARTED = soundPool.load(a, R.raw.test_started, 1);
        SpeedTestLauncher.SOUND_TEST_COMPLETE = soundPool.load(a, R.raw.test_complete, 1);
        SpeedTestLauncher.SOUND_ERROR = soundPool.load(a, R.raw.error, 1);;
        SpeedTestLauncher.SOUND_POPUP_SHOW = soundPool.load(a, R.raw.popup_show, 1);
        SpeedTestLauncher.SOUND_POPUP_HIDE = soundPool.load(a, R.raw.popup_hide, 1);
        SpeedTestLauncher.SOUND_CLICK_DOWN = soundPool.load(a, R.raw.click_down, 1);

    }

    public void updateVolume(){
        // AudioManager audio settings for adjusting the volume
        audioManager = (AudioManager) a.getSystemService(Context.AUDIO_SERVICE);
        actVolume = (float) audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        maxVolume = (float) audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        volume = actVolume / maxVolume;
    }

    public void playSound( int id ) {
        updateVolume();
        // Is the sound loaded does it already play?
        if ( isLoaded.containsKey(id) ) {
            soundPool.play(id, volume, volume, 1, 0, 1f);
            counter = counter++;
            Log.d(TAG, "playSound at volume " + volume);

        }

        Log.d(TAG, "playSound failed. not loaded. at volume " + volume);

    }
}
