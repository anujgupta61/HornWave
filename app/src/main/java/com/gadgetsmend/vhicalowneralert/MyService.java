package com.gadgetsmend.vhicalowneralert;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.os.AsyncTaskCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class MyService extends Service implements GoogleApiClient.ConnectionCallbacks , GoogleApiClient.OnConnectionFailedListener , LocationListener {
    int notificationId = 1 ;
    String message1 = "" ;
    ArrayList<Integer> alertIds = new ArrayList<> () ;
    private double latitude, longitude;
    private GoogleApiClient mGoogleApiClient; // Creating Google API client

    @Override
    public IBinder onBind(Intent intent) {
        // Service is not binded to any activity , hence no binding required
        return null;
    }

    private void startInForeground() {
        int notification_icon = R.mipmap.new_logo ;
        String notificationTrickertext = "Hornwave app about to start";
        long notificationTimeStamp = System.currentTimeMillis();
        String notificationTitleText = "Hornwave App";
        String notificationBodyText = "Hornwave App is Currently running";
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent notificationPendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        Notification _foregroundNotification ;
        _foregroundNotification = new Notification.Builder(getApplicationContext())
                .setSmallIcon(notification_icon)
                .setTicker(notificationTrickertext)
                .setWhen(notificationTimeStamp)
                .setContentText(notificationBodyText)
                .setContentTitle(notificationTitleText)
                .setContentIntent(notificationPendingIntent)
                .build();

        startForeground(notificationId, _foregroundNotification);

    }

    // Function to check Internet connectivity
    boolean checkConnection(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting() ;
    }

    private class FetchNotification extends TimerTask {

        public void run() {
            class wrapper {
                String status;
            }

            class SendPostReqAsyncTask extends AsyncTask<String, Void, wrapper> {
                private wrapper w = new wrapper();

                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                }

                @Override
                protected wrapper doInBackground(String... params) {
                    String data = "";
                    try {
                        data = URLEncoder.encode("no", "UTF-8")
                                + "=" + URLEncoder.encode(SaveSharedPreference . getVehicleNo(getApplicationContext()) , "UTF-8") ;
                    } catch (UnsupportedEncodingException e) {
                        //
                    }

                    BufferedReader reader = null;

                    // Send data
                    try {

                        // Defined URL  where to send data
                        // https://hornwave.000webhostapp.com/fetch_notification.php
                        URL url = new URL("http://pntagencies.in/fetch_notification.php");

                        // Send POST data request

                        URLConnection conn = url.openConnection();
                        conn.setDoOutput(true);
                        OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
                        wr.write(data);
                        wr.flush();

                        // Get the server response

                        reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        String line , str = "";

                        // Read Server Response
                        while ((line = reader.readLine()) != null) {
                            str = str + line ;
                        }

                        w.status = str;
                    } catch (Exception ex) {
                        //
                    } finally {
                        try {
                            if(reader != null)
                                reader.close();
                        } catch (Exception ex) {
                            //
                        }
                    }
                    return w;
                }

                @Override
                protected void onPostExecute(wrapper w) {
                    super.onPostExecute(w);
                    String data = null ;
                    try {
                        data = w.status.trim();
                    } catch(Exception ex) {
                        //
                    }
                    message1 = "" ;
                    if (data != null && data.charAt(0) == '1') {
                        int i = 0;
                        while (data.charAt(i) != '1')
                            i++;
                        i++;
                        while (i < data.length()) {
                            message1 += data.charAt(i);
                            i++;
                        }
                        show_notification(message1, 0);
                    }
                }
            }
            SendPostReqAsyncTask sendPostReqAsyncTask = new SendPostReqAsyncTask();
            try {
                if (checkConnection(getApplicationContext()))
                    AsyncTaskCompat.executeParallel(sendPostReqAsyncTask);
            } catch(Exception ex) {
                //
            }
        }
    }

    private class FetchEmergencyAlert extends TimerTask {

        public void run() {
            class wrapper {
                int id ;
                String msg ;
            }

            class SendPostReqAsyncTask extends AsyncTask<String, Void, wrapper[]> {
                private wrapper[] w ;

                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                }

                @Override
                protected wrapper[] doInBackground(String... params) {

                    String data = "";
                    try {
                        data = URLEncoder.encode("lat", "UTF-8")
                                + "=" + URLEncoder.encode(latitude + "" , "UTF-8") + "&" + URLEncoder.encode("lng", "UTF-8")
                                + "=" + URLEncoder.encode(longitude + "" , "UTF-8") ;
                    } catch (UnsupportedEncodingException e) {
                        //
                    }

                    BufferedReader reader = null;
                    HttpURLConnection conn = null;

                    // Send data
                    try {

                        // Defined URL  where to send data
                        // https://hornwave.000webhostapp.com/fetch_emergency.php
                        URL url = new URL("http://pntagencies.in/fetch_emergency.php");

                        // Send POST data request

                        conn =(HttpURLConnection) url.openConnection();
                        conn.setDoOutput(true);
                        OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
                        wr.write(data);
                        wr.flush();

                        // Get the server response

                        reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        int fetchedData = reader.read();
                        // Reading json string from server
                        String json_str = "{ \"alert\": ";
                        while (fetchedData != -1) {
                            char current = (char) fetchedData;
                            fetchedData = reader.read();
                            json_str = json_str + current;
                        }

                        json_str = json_str + "}";
                        final JSONObject obj = new JSONObject(json_str);
                        final JSONArray geodata = obj.getJSONArray("alert");
                        final int n = geodata.length();
                        if(n == 0)
                            return null ;
                        w = new wrapper[n];
                        for (int i = 0; i < n; i++) {
                            final JSONObject alert = geodata.getJSONObject(i);
                            w[i] = new wrapper();
                            w[i] . id = alert . getInt("id") ;
                            w[i] . msg = alert . getString("message") ;
                        }
                    } catch (Exception j) {
                        //
                    }
                    finally {
                        if (conn != null) {
                            conn.disconnect();
                            try {
                                if(reader != null)
                                    reader.close();
                            }catch (IOException e){
                                Log.e("ReaderMap","Reader was not opened");
                            }
                        }
                    }
                    return w;
                }

                @Override
                protected void onPostExecute(wrapper w[]) {
                    super.onPostExecute(w);
                    if (w != null) {
                        int i = 0 ;
                        while(i < w.length) {
                            if(! alertIds . contains(w[i] . id)) {
                                message1 = w[i].msg;
                                notificationId = w[i].id ;
                                alertIds . add(w[i].id);
                                show_notification(message1, 1);
                            }
                            i ++ ;
                        }
                        notificationId = 1 ;
                    }
                }
            }
            try {
                if (checkConnection(getApplicationContext())) {
                    SendPostReqAsyncTask sendPostReqAsyncTask = new SendPostReqAsyncTask();
                    AsyncTaskCompat.executeParallel(sendPostReqAsyncTask);
                } else {
                    Toast.makeText(getApplicationContext(), "No internet", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception ex) {
                //
            }
        }
    }

    void show_notification(String message , int type) {
        // type 1 means emergency alert
        int notification_icon = R.mipmap.new_logo ;
        String notificationTitleText ;
        String notificationTrickertext = "Hornwave app about to start" ;
        if(type == 0)
            notificationTitleText = "Hornwave" ;
        else
            notificationTitleText = "Hornwave(Emergency)" ;
        long notificationTimeStamp = System.currentTimeMillis();
        if(message . length() == 0)
            message = " " ;
        String notificationBodyText = message ;
        Intent intent = new Intent(this , MainActivity.class) ;
        intent . setFlags(FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK);
        intent . putExtra("fromnotification", true);
        intent . putExtra("msg" , message) ;

        PendingIntent notificationPendingIntent = PendingIntent.getActivity(this, 0, intent , PendingIntent.FLAG_UPDATE_CURRENT);

        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION) ;

        Notification Notification1 = new Notification.Builder(getApplicationContext())
                .setSmallIcon(notification_icon)
                .setTicker(notificationTrickertext)
                .setWhen(notificationTimeStamp)
                .setContentText(notificationBodyText)
                .setContentTitle(notificationTitleText)
                .setContentIntent(notificationPendingIntent)
                .setSound(alarmSound)
                .build();
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        notificationId ++ ;
        mNotificationManager.notify(notificationId , Notification1);
    }

        @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        Timer timer1 = new Timer();
        timer1.scheduleAtFixedRate(new FetchNotification(), 0, 500);
        Timer timer2 = new Timer();
        timer2.scheduleAtFixedRate(new FetchEmergencyAlert(), 0, 500);
        startInForeground(); // start in foreground
    }

    @Override
    public void onConnected(Bundle bundle) {
        LocationRequest mLocationRequest;
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(500);
        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        } catch (SecurityException e) {
            //
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {
        longitude = location.getLongitude();
        latitude = location.getLatitude();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mGoogleApiClient.connect();
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        mGoogleApiClient . disconnect();
        super.onDestroy();
    }
}