package com.xanderfehsenfeld.tigertest.Activity;

import android.os.Message;
import android.util.Log;

import com.xanderfehsenfeld.tigertest.MySharedPrefsWrapper;
import com.xanderfehsenfeld.tigertest.Service.DataGatherService;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * An internet speed test which downloads a file and measures how long it takes,
 * sending out periodic messages to SpeedTestLauncher
 *
 * Once the tester has started, it either stops when the file is downloaded,
 * or when its thread is interrupted
 *
 *
 */
public class SpeedTester implements Runnable {

    private static final double BYTE_TO_KILOBIT = 0.0078125;
    private static final double KILOBIT_TO_MEGABIT = 0.0009765625;

    public static final String TAG_WORKER = "WORKER";
    protected static int EXPECTED_SIZE_IN_BYTES = 5 * 1000000;//5MB 1024*1024

    /* connection timeout in ms */
    public int connectTimeout;

    //private SpeedTestLauncher speedTestLauncher;
    private DataGatherService dataGatherService;

    public SpeedTester(DataGatherService dataGatherService ) {
        //this.speedTestLauncher = speedTestLauncher;
        this.dataGatherService = dataGatherService;
    }

    @Override
    public void run() {

        /* get the connection timeout */
        connectTimeout = dataGatherService.sharePrfs.prefs.getInt(MySharedPrefsWrapper.PREF_CONNECT_TIME_LIMIT, 10000);

        InputStream stream = null;
        int bytesIn = 0;
        try {


            long startCon = System.currentTimeMillis();

            /* the file to be downloaded */
            URL url = new URL(dataGatherService.DOWNLOAD_FILE_URL);
            URLConnection con = url.openConnection();

            /* set the connection time out */
            con.setConnectTimeout(connectTimeout);



            con.setUseCaches(false);
            long connectionLatency = System.currentTimeMillis() - startCon;
            stream = con.getInputStream();

            EXPECTED_SIZE_IN_BYTES = con.getContentLength();

            /* don't need to send this to dataGatherService, bc this update only concerns UI */
            Message msgUpdateConnection = Message.obtain(dataGatherService.mHandler, SpeedTestLauncher.MSG_UPDATE_CONNECTION_TIME);
            msgUpdateConnection.arg1 = (int) connectionLatency;
            dataGatherService.mHandler.sendMessage(msgUpdateConnection);

            long start = System.currentTimeMillis();
            int currentByte = 0;
            long updateStart = System.currentTimeMillis();
            long updateDelta = 0;
            int bytesInThreshold = 0;


            /* get max bytes to read
            * There may not be this amount of bytes available to read there
            * are rarely more available
            */
            int bytesToRead = 1024;
            byte[] readBytes = new byte[bytesToRead];
            currentByte = stream.read(readBytes);

            while (currentByte != -1) {

                currentByte = stream.read(readBytes);


                if (Thread.interrupted()) {
                    Log.d(TAG_WORKER, "thread interupted. returning.");
                    /* send message complete even though its not */
                    Long downloadTime = (System.currentTimeMillis() - start);

                    Message msg = Message.obtain(dataGatherService.mHandler, SpeedTestLauncher.MSG_COMPLETE_STATUS, calculate(downloadTime, bytesIn, EXPECTED_SIZE_IN_BYTES));
                    //Message dataMsg = Message.obtain(dataGatherService.mHandler, speedTestLauncher.MSG_COMPLETE_STATUS, calculate(downloadTime, bytesIn));

                    msg.arg1 = bytesIn;

                    /* communicate this is not complete */
                    msg.arg2 = 0;

                    dataGatherService.mHandler.sendMessage(msg);

                    return;
                }

                //bytesIn++;
                bytesIn += currentByte;
                bytesInThreshold += currentByte;
                if (updateDelta >= SpeedTestLauncher.UPDATE_THRESHOLD) {
                    int progress = (int) ((bytesIn / (double) EXPECTED_SIZE_IN_BYTES) * 100);
                    Message msg = Message.obtain(dataGatherService.mHandler, SpeedTestLauncher.MSG_UPDATE_STATUS, calculate(updateDelta, bytesInThreshold, EXPECTED_SIZE_IN_BYTES));
                    msg.arg1 = progress;
                    msg.arg2 = bytesIn;
                    dataGatherService.mHandler.sendMessage(msg);
                    //Reset
                    updateStart = System.currentTimeMillis();
                    bytesInThreshold = 0;

                }
                /* reassign vars */
                bytesToRead = Math.max(stream.available(), 1024);
                readBytes = new byte[bytesToRead];
                updateDelta = System.currentTimeMillis() - updateStart;
            }

            long downloadTime = (System.currentTimeMillis() - start);

            /* cancel countdown before sending results, because the test is considered finished */
            dataGatherService.mCountDownTimer.cancel();


            //Prevent ArithmeticException
            if (downloadTime == 0) {
                downloadTime = 1;
            }

            Message msg = Message.obtain(dataGatherService.mHandler, SpeedTestLauncher.MSG_COMPLETE_STATUS, calculate(downloadTime, bytesIn, EXPECTED_SIZE_IN_BYTES));
            //Message dataMsg = Message.obtain(speedTestLauncher.mHandler, speedTestLauncher.MSG_COMPLETE_STATUS, calculate(downloadTime, bytesIn));
            msg.arg2 = 1;
            msg.arg1 = bytesIn;
            //speedTestLauncher.mHandler.sendMessage(msg);
            dataGatherService.mHandler.sendMessage(msg);
        } catch (MalformedURLException e) {

            Log.e(TAG_WORKER, "malformed url exception: " + e.getMessage());

        } catch  ( java.net.SocketTimeoutException e ){
            Log.e(TAG_WORKER, "java.net.SocketTimeoutException: " + e.getMessage());

            /* send message complete even though its not  complete
            * */
            //Long downloadTime = (System.currentTimeMillis() - start);
            Message msg = Message.obtain(dataGatherService.mHandler, SpeedTestLauncher.MSG_COMPLETE_STATUS, calculate(0, bytesIn, EXPECTED_SIZE_IN_BYTES));

            msg.arg1 = bytesIn;

            /* communicate this test is not complete */
            msg.arg2 = -1;

            dataGatherService.mHandler.sendMessage(msg);

        }  catch (IOException e) {
            Log.e(TAG_WORKER, "io exception " + e.getMessage() + ". restarting test");
            Message msg = Message.obtain(dataGatherService.mHandler, SpeedTestLauncher.MSG_COMPLETE_STATUS, null);
            dataGatherService.mHandler.sendMessage(msg);

        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (IOException e) {
                //Suppressed
            }
        }

        /* cencel the countdown timer */
        dataGatherService.mCountDownTimer.cancel();

    }

    /**
     *
     * 1 byte = 0.0078125 kilobits
     * 1 kilobits = 0.0009765625 megabit
     *
     * @param downloadTime in miliseconds
     * @param bytesIn number of bytes downloaded
     * @return SpeedInfo containing current speed
     */
    private static SpeedInfo calculate(final long downloadTime, final long bytesIn, final long expectedBytes){
        SpeedInfo info=new SpeedInfo();
        //from mil to sec
        long bytespersecond;
        if (!(downloadTime == 0)) {
            bytespersecond = (bytesIn / downloadTime) * 1000;
        } else {
            bytespersecond = 0;
        }
        double kilobits=bytespersecond * BYTE_TO_KILOBIT;
        double megabits=kilobits  * KILOBIT_TO_MEGABIT;
        info.downspeed=bytespersecond;
        info.kilobits=kilobits;
        info.megabits=megabits;
        info.percentComplete = (float)bytesIn / (float)expectedBytes;

        return info;
    }

    /**
     * Transfer Object
     * @author devil
     *
     */
    public static class SpeedInfo{
        public double kilobits=0;
        public double megabits=0;
        public double downspeed=0;
        public float percentComplete=0;
    }

}
