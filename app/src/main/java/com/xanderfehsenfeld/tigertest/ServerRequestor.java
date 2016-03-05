package com.xanderfehsenfeld.tigertest;

import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Xander on 11/16/15.
 *
 * Handle connection to a server and simple requests
 */
public class ServerRequestor {

    private static HttpClient httpclient;

    private static String TAG = "ServerRequestor";

    static final String ERROR_RESPONSE_READ = "error reading response";


//    /**getDownloadUrl
//     *
//     * @param serverUrl the url of the server
//     * @return the url of the file for speedtest to download
//     * @throws IOException
//     */
//
//    public static String getDownloadUrl( String serverUrl ) throws IOException {
//
//        // init client
//        httpclient = new DefaultHttpClient();
//
//        /* store your params as name value pairs */
//        List<NameValuePair> params = new ArrayList<NameValuePair>();
//
//        /* Add in requests */
//        params.add(new BasicNameValuePair("request", "DOWNLOAD_FILE_URL" ) );
//
//        String paramString = URLEncodedUtils.format(params, "utf-8");
//
//        serverUrl += "?" + paramString;
//
//        // Prepare a request object
//        HttpGet httpget = new HttpGet(serverUrl);
//
//        System.out.println( serverUrl );
//
//        //System.out.println( httpget.toString() );
//
//        HttpResponse response = null;
//
//        // Execute the request
//        try {
//
//            // Execute HTTP Post Request
//            response = httpclient.execute(httpget);
//
//
//        } catch (Exception e) {
//            System.out.println(e.toString());
//        }
//
//        if ( response != null ){
//
//            /* put the response to a string */
//            HttpEntity responseEntity = response.getEntity();
//            String responseString = EntityUtils.toString(responseEntity, String.valueOf(responseEntity.getContentEncoding( )));
//            System.out.println(responseString);
//
//        } else {
//
//            throw new IllegalStateException( "server did not respond to urlDownloadFile request." );
//        }
//
//        return "";
//
//
//
//    }

    public static String post(String url, HashMap<String, String> data) throws IOException {


        // init client
        httpclient = new DefaultHttpClient();

        // Prepare a request object
        HttpPost httppost = new HttpPost(url);
        HttpResponse response = null;

        // Execute the request
        try {


            // Add your data
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();

            /* turn hashmap into name value pairs */
            for (String key : new ArrayList<String>( data.keySet() ) ){
                nameValuePairs.add(new BasicNameValuePair(key, data.get(key) ) );
            }

            /* store message body in post request */
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            // Execute HTTP Post Request
            response = httpclient.execute(httppost);

        } catch (Exception e) {
            Log.d(TAG, e.toString());

        }



        /* attempt to read response */
        if ( response != null) {
            StatusLine s = response.getStatusLine();
            Log.d(TAG, "response status: " + s.toString());
            Log.d(TAG, "response locale: " + response.getLocale());

            try {
                HttpEntity responseEntity = response.getEntity();
                Log.d(TAG, "content encoding: " + responseEntity.getContentEncoding());
                //String contentEncoding = String.valueOf(responseEntity.getContentEncoding();
                String contentEncoding = "UTF-8";

                String responseString = EntityUtils.toString(responseEntity, contentEncoding);
                Log.d(TAG, "response string: " + responseString);

                /* cull out uuid */
                int start, end;
                if ( (start =  responseString.indexOf('{')) != -1 && (end =  responseString.indexOf('}'))!= -1) {
                    responseString = responseString.substring(start, end);
                }
                /* format of response is {added:<uuid>}..<otherstuff>, and we just want the uuid */
                if (responseString.contains("{added:")) {
                    responseString = responseString.replace("{added:", "");
                    responseString = responseString.replace("}", "");
                    responseString = responseString.trim();
                } else if (responseString.contains("{already have:")) {
                    responseString = responseString.replace("{already have:", "");
                    responseString = responseString.replace("}", "");
                    responseString = responseString.trim();
                }

                //interpretResponse( responseString, db );
                return responseString;

            } catch (Exception e){
                Log.e(TAG, "failed to decode response body: " + e.toString() + " response string: " );

            }



        } else {
            Log.d(TAG, "response was null");

        }
        return ERROR_RESPONSE_READ;
    }






}
