package com.xanderfehsenfeld.tigertest.LocalDB;


/**
 * created by Xander
 */

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.Messenger;
import android.os.ResultReceiver;
import android.util.Log;

import com.xanderfehsenfeld.tigertest.Launcher.MySharedPrefsWrapper;
import com.xanderfehsenfeld.tigertest.Launcher.Tests;
import com.xanderfehsenfeld.tigertest.R;
import com.xanderfehsenfeld.tigertest.ServerRequestor;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class DatabaseManagerService extends Service
{
    private static final String TAG = "DatabaseManagerService";
    private static final String LOCATION_SERVICES_TAG = "DatabaseManagerService";


    //protected final Messenger mServerMessenger = new Messenger(new IncomingHandler(this));
    protected Messenger mServerMessenger;

    public static final int MSG_RECORD_ADDED = 2;


    /* send data back to MainActivity */
    public ResultReceiver resultReceiver;

    /* TODO remove after testing */
    ResultReceiver mTestResultReceiver;

    /* keep speech recognizer going */
    CountDownTimer mTimer;

    /* db with wrapper class */
    protected MyDbWrapper db;
    private boolean mTransferInProgress = false;
    private String SERVER_URL;

    /* shared prefs */
    public MySharedPrefsWrapper sharePrfs;


    @Override
    public void onCreate()
    {
        SERVER_URL = getResources().getString(R.string.server_url);

        Log.d(TAG, "onCreate"); //$NON-NLS-1$

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(TAG, "onStartCommand"); //$NON-NLS-1$



        return START_STICKY;



    }


    /** transferRecords
     *      transfer all the records to the server
     *      the server requester deletes the records upon response from server so the db should eventually dwindle to empty
     */
    public void transferRecords(){
        mTransferInProgress = true;

        new Thread(new Runnable() {
            @Override
            public void run() {
                if ( db.getRecordCount() > 0 ) {
                    for ( String id : db.getIds()) {
                        HashMap<String,String> record = db.retrieveRecord(id);

                        AsyncTask task = new PostToServerTask().execute(record);
                        try {
                            task.get(30, TimeUnit.SECONDS);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        } catch (TimeoutException e) {
                            e.printStackTrace();
                        }

                    }

                }
            }
        }).run();

        mTransferInProgress = false;

    }

    /** interpretResponse
     *      if the server returns a uuid, this function will remove that record from the db
     * @param response the response string
     */
    private boolean interpretResponse(String response){
        if (db.retrieveRecord(response) != null) {
            db.removeRecord(response);
            return true;
        }
        return false;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();


        /* send end code to parent activity */
        Bundle bundle = new Bundle();
        //bundle.putString("end", "Timer Stopped....");
        resultReceiver.send(200, bundle);
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        Log.d(TAG, "onBind");  //$NON-NLS-1$

        resultReceiver = intent.getParcelableExtra("receiver");

        /* TODO remove after testing */
        mTestResultReceiver = intent.getParcelableExtra(Tests.TEST_RECEIVER_STRING);


        FeedReaderDbHelper mDbHelper = new FeedReaderDbHelper(getApplicationContext());
        db = new MyDbWrapper(mDbHelper.getWritableDatabase());
        Log.d(TAG, "opened writeable db:");
        db.printDb();


        /* send an initial amount */
        return mServerMessenger.getBinder();
    }

    /* send results out */
    private void sendResults( HashMap<String, String> record, Boolean wasDeleted ){
        /* send info back to parent activity */
        Bundle bundle = new Bundle();
        for ( String key : record.keySet() ){
            bundle.putString(key, record.get(key));
        }
        bundle.putBoolean("wasDeleted", wasDeleted);
        resultReceiver.send(2, bundle);

        mTestResultReceiver.send(2, bundle);
    }

    /** PostToServerTask
     *      params <
     *          HashMap<String,String> - The record to post (only one)
     *          Void, - the update type (no update)
     *          String - the result string
     *          >
     *      When it executes, it deletes the records on the local db if possible and sends the results
     *
     */
    private class PostToServerTask extends AsyncTask<HashMap<String, String>, Void, String> {

        public static final String TAG_POSTER = "PostToServerTask";

        @Override
        protected String doInBackground(HashMap<String,String> ... records) {
            String result = "no result";
            try {
                result = ServerRequestor.post(SERVER_URL, records[0]);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return result;
        }

        @Override
        protected void onPostExecute(String result) {

            HashMap<String, String> record = db.retrieveRecord(result);
            if (record != null) sendResults(db.retrieveRecord(result), interpretResponse(result));
            else {
                Log.e(TAG, "onPostExecute record is null");
            }

        }
    }





}