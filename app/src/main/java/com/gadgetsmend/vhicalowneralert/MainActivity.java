package com.gadgetsmend.vhicalowneralert ;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.os.AsyncTaskCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks , GoogleApiClient.OnConnectionFailedListener , LocationListener {
    private double latitude, longitude;
    private GoogleApiClient mGoogleApiClient; // Creating Google API client

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (SaveSharedPreference.getVehicleNo(this).length() == 0) {
            Intent intent = new Intent(getApplicationContext(), RegistrationActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        } else {
            mGoogleApiClient = new GoogleApiClient
                    .Builder(this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
            startService(new Intent(getApplicationContext(), MyService.class));
            setContentView(R.layout.activity_main);
            Button btn = (Button) findViewById(R.id.notifyButton);
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    send_notification();
                }
            });
            ImageView img_btn = (ImageView) findViewById(R.id.alertButton);
            img_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    sendEmergencyAlert();
                }
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.qr:
                final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                try {
                    LayoutInflater inflater = MainActivity.this.getLayoutInflater();
                    final View view = inflater.inflate(R.layout.dialog_qr, null) ;
                    // Inflate and set the layout for the dialog
                    // Pass null as the parent view because its going in the dialog layout
                    builder.setView(view)
                            .setTitle("QR Code")
                            // Add action buttons
                            .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });
                    AlertDialog alert = builder.create();
                    alert.show();
                } catch(Exception ex) {
                    //
                }
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        ShowGPSSettings(MainActivity.this);
        mGoogleApiClient.connect();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(this.getIntent().getBooleanExtra("fromnotification", false)) {
            String message = MainActivity.this.getIntent().getStringExtra("msg") ;
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Alert Message")
                    .setMessage(message)
                    .setCancelable(true)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog . cancel() ;
                        }
                    }) ;
            AlertDialog alert = builder.create();
            alert.show();
            this.getIntent().removeExtra("fromnotification");
            this.getIntent().removeExtra("msg");
        }
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
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

    // Function to check GPS connectivity and show alert dialog if there is no GPS
    public void ShowGPSSettings(Activity activity) {
        LocationManager lm = (LocationManager)getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        if(! lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) { // If GPS is disabled
            try {
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle("NO GPS")
                        .setMessage("Please select High Accuracy Location Mode")
                        .setCancelable(true)
                        .setPositiveButton("Cancel",new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog . cancel() ;
                            }
                        })
                        .setNegativeButton("GPS Settings",new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) ;
                            }
                        });
                AlertDialog alert = builder.create();
                alert.show();
            } catch(Exception e) {
                //
            }
        }
    }

    // Function to take run-time permissions in Android Marshmallow
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 1) {
            if(grantResults.length == 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                ShowGPSSettings(MainActivity.this) ;
                this.recreate() ;
            } else {
                // Permission was denied or request was cancelled
                Toast.makeText(getApplicationContext(), "You must grant permission to access the gps and use map ...", Toast.LENGTH_LONG).show();
            }
        }
    }

    // Function to check Internet connectivity
    boolean checkConnection(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }

    // Function to show NO INTERNET alert dialog
    public void showInternetNotAvailableAlert(Activity activity) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle("NO INTERNET")
                    .setMessage("Please enable internet")
                    .setCancelable(true)
                    .setPositiveButton("Cancel",new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog . cancel() ;
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        } catch(Exception e) {
            //
        }
    }

    void send_notification() {
        if(! checkConnection(getApplicationContext())) {
            Toast . makeText(getApplicationContext() , "No internet" , Toast .  LENGTH_SHORT) . show() ;
            showInternetNotAvailableAlert(MainActivity.this);
            return ;
        }
        EditText evehicleNo = (EditText) findViewById(R . id . number) ;
        final String vehicleNo = evehicleNo . getText() . toString() ;
        if(vehicleNo . length() == 0) {
            Toast . makeText(getApplicationContext() , "Enter vehicle number ..." , Toast . LENGTH_SHORT) . show() ;
            return ;
        }
        if(vehicleNo . contains(" ")) {
            Toast . makeText(getApplicationContext() , "Vehicle number can not contain spaces ..." , Toast . LENGTH_SHORT) . show() ;
            return ;
        }
        EditText emessage = (EditText) findViewById(R . id . message) ;
        final String message = emessage . getText() . toString() ;
        class wrapper {
            String status;
        }

        class SendPostReqAsyncTask extends AsyncTask<String, Void, wrapper> {
            private wrapper w = new wrapper();
            private ProgressDialog dialog = ProgressDialog.show(MainActivity.this, "",
                    "Sending Notification...", true);

            @Override
            protected void onPreExecute() {
                Log.v("Data" , "OnPre") ;
                super.onPreExecute();
                dialog.show();
                dialog.setOnKeyListener(new Dialog.OnKeyListener() {

                    @Override
                    public boolean onKey(DialogInterface arg0, int keyCode,
                                         KeyEvent event) {
                        if (keyCode == KeyEvent.KEYCODE_BACK) {
                            dialog.dismiss();
                        }
                        return true;
                    }
                });

            }

            @Override
            protected wrapper doInBackground(String... params) {
                Log.v("Data" , "doIn") ;
                String data = "";
                try {
                    data = URLEncoder.encode("no", "UTF-8")
                            + "=" + URLEncoder.encode(vehicleNo , "UTF-8") + "&" + URLEncoder.encode("message", "UTF-8")
                            + "=" + URLEncoder.encode(message , "UTF-8") ;
                } catch (UnsupportedEncodingException e) {
                    Log.v("Exception" , "data not encoded") ;
                }

                BufferedReader reader = null;

                // Send data
                try {

                    // Defined URL  where to send data
                    // https://hornwave.000webhostapp.com/insert_notification.php
                    URL url = new URL("http://pntagencies.in/insert_notification.php");

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
                    Log.v("Exception" , "data not sent") ;
                } finally {
                    try {
                        if(reader != null)
                            reader.close();
                    } catch (IOException ex) {
                        Log.v("Exception" , "reader not closed") ;
                    }
                }
                return w;
            }

            @Override
            protected void onPostExecute(wrapper w) {
                Log.v("Data" , "OnPost") ;
                super.onPostExecute(w);
                dialog . dismiss() ;
                int data = -1 ;
                try {
                    data = Integer.parseInt(w.status.trim());
                } catch(NumberFormatException ex) {
                    Log.v("Exception" , "Number Format Exception") ;
                }
                if(data == 1)
                    Toast. makeText(getApplicationContext() , "Now sit back and relax. The vehicle will give you a way soon ." , Toast . LENGTH_LONG) . show() ;
                else {
                    if(data == 0)
                        Toast. makeText(getApplicationContext() , "You have already sent Notification to driver ..." , Toast . LENGTH_LONG) . show() ;
                    if(data == 2)
                        Toast. makeText(getApplicationContext() , "Notification can not be sent since vehicle is not registered ..." , Toast . LENGTH_LONG) . show() ;
                }
            }
        }
        SendPostReqAsyncTask sendPostReqAsyncTask = new SendPostReqAsyncTask();
        try {
            if (checkConnection(getApplicationContext()))
                AsyncTaskCompat.executeParallel(sendPostReqAsyncTask);
            else {
                Toast.makeText(getApplicationContext(), "No internet", Toast.LENGTH_SHORT).show();
                showInternetNotAvailableAlert(MainActivity.this);
            }
        } catch(Exception ex) {
            Log.v("HornWave" , "Error in sending notification ...") ;
        }
    }

    void sendEmergencyAlert() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        try {
            LayoutInflater inflater = MainActivity.this.getLayoutInflater();
            final View view = inflater.inflate(R.layout.dialog_emergency_alert, null) ;
            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            builder.setView(view)
                    // Add action buttons
                    .setPositiveButton("SEND", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            EditText emessage = (EditText) view . findViewById(R.id.alertMessage);
                            String alert_message = emessage.getText().toString();
                            if (alert_message.length() == 0) {
                                Toast.makeText(getApplicationContext(), "Enter alert message ...", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            EditText earea = (EditText) view . findViewById(R.id.alertArea);
                            String alert_area = earea.getText().toString();
                            if (alert_area . length() == 0) {
                                Toast.makeText(getApplicationContext(), "Enter alert area ...", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            if(Integer.parseInt(alert_area . trim()) > 999) {
                                Toast.makeText(getApplicationContext(), "Alert area can be maximum 999 meters ...", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            sendEmergency(alert_message , alert_area);
                        }
                    })
                    .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        } catch(Exception ex) {
            Log.v("HornWave" , "Error in sending emergency alert ...") ;
        }
    }

    void sendEmergency(final String alert_message , final String alert_area) {
        if(! checkConnection(getApplicationContext())) {
            Toast . makeText(getApplicationContext() , "No internet" , Toast .  LENGTH_SHORT) . show() ;
            showInternetNotAvailableAlert(MainActivity.this);
            return ;
        }

        class wrapper {
            String status;
        }

        class SendPostReqAsyncTask extends AsyncTask<String, Void, wrapper> {
            private wrapper w = new wrapper();
            private ProgressDialog dialog = ProgressDialog.show(MainActivity.this, "", "Sending emergency alert ...", true);

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                dialog.show();
                dialog.setOnKeyListener(new Dialog.OnKeyListener() {

                    @Override
                    public boolean onKey(DialogInterface arg0, int keyCode,
                                         KeyEvent event) {
                        if (keyCode == KeyEvent.KEYCODE_BACK) {
                            dialog.dismiss();
                        }
                        return true;
                    }
                });
            }

            @Override
            protected wrapper doInBackground(String... params) {
                String data = "";
                try {
                    data = URLEncoder.encode("message", "UTF-8")
                            + "=" + URLEncoder.encode(alert_message , "UTF-8") + "&" + URLEncoder.encode("lat", "UTF-8")
                            + "=" + URLEncoder.encode(latitude + "" , "UTF-8") + "&" + URLEncoder.encode("lng", "UTF-8")
                            + "=" + URLEncoder.encode(longitude + "" , "UTF-8") + "&" + URLEncoder.encode("area", "UTF-8")
                            + "=" + URLEncoder.encode(alert_area , "UTF-8") ;
                } catch (Exception e) {
                    Log.v("HornWave" , "Error in encoding emergency data ...") ;
                }

                BufferedReader reader = null;

                // Send data
                try {
                    // Define URL  where to send data
                    // https://hornwave.000webhostapp.com/insert_emergency.php
                    URL url = new URL("http://pntagencies.in/insert_emergency.php");

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
                    Log.v("HornWave" , "Error in sending emergency data ...") ;
                } finally {
                    try {
                        if(reader != null)
                            reader.close();
                    } catch(IOException ex) {
                        Log.v("HornWave" , "BufferedReader not closed ...") ;
                    }
                }
                return w;
            }

            @Override
            protected void onPostExecute(wrapper w) {
                super.onPostExecute(w);
                dialog . dismiss() ;
                int data = -1 ;
                try {
                    data = Integer.parseInt(w.status.trim());
                } catch(NumberFormatException ex) {
                    Log.v("HornWave" , "Error in parsing ...") ;
                }
                if(data == 1)
                    Toast. makeText(getApplicationContext() , "Emergency alert sent ...." , Toast . LENGTH_LONG) . show() ;
                else if(data == 0)
                    Toast. makeText(getApplicationContext() , "Emergency alert not sent ...." , Toast . LENGTH_LONG) . show() ;
                else
                    Toast. makeText(getApplicationContext() , "Some error occured , Try again ..." , Toast . LENGTH_LONG) . show() ;
            }
        }
        SendPostReqAsyncTask sendPostReqAsyncTask = new SendPostReqAsyncTask();
        try {
            if (checkConnection(getApplicationContext()))
                AsyncTaskCompat.executeParallel(sendPostReqAsyncTask);
            else {
                Toast.makeText(getApplicationContext(), "No internet", Toast.LENGTH_SHORT).show();
                showInternetNotAvailableAlert(MainActivity.this);
            }
        } catch(Exception ex) {
            Log.v("HornWave" , "Error in sending emergency alert ...") ;
        }
    }

    public void shareLink(View view) {
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        String shareBody = "Are you frustated with wrong parking and do not get free space in Emergency ? Now be tension-free ; this smart parking assistance app is for you . Download the app from the link - https://play.google.com/store/apps/details?id=com.gadgetsmend.vhicalowneralert&hl=en and share it with your friends ." ;
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "HornWave app");
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(sharingIntent, "Share via"));
    }
}