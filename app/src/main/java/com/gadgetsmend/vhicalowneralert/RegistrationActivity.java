package com.gadgetsmend.vhicalowneralert;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.os.AsyncTaskCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

public class RegistrationActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);
        Button btn = (Button) findViewById(R. id. button) ;
        btn . setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                register();
            }
        } );
    }

    // Function to check Internet connectivity
    boolean checkConnection(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting() ;
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

    void register() {
        EditText evehicleNo = (EditText)findViewById(R.id.vehicleNo) ;
        final String vehicle_no = evehicleNo  . getText() . toString() ;
        if(vehicle_no . contains(" ")) {
            Toast . makeText(getApplicationContext() , "Vehicle number can not contain spaces ..." , Toast . LENGTH_SHORT) . show() ;
            return ;
        }

        if(vehicle_no . length() == 0) {
            Toast . makeText(getApplicationContext() , "Please enter Vehicle number ..." , Toast . LENGTH_SHORT) . show() ;
            return ;
        }

        EditText ename = (EditText) findViewById(R . id . name) ;
        final String name = ename . getText() . toString() ;

        if(name . length() == 0) {
            Toast . makeText(getApplicationContext() , "Please enter your name ..." , Toast . LENGTH_SHORT) . show() ;
            return ;
        }

        EditText econtactNo = (EditText) findViewById(R . id . contact_no) ;
        final String contact_no = econtactNo . getText() . toString() ;

        if(contact_no . length() == 0) {
            Toast . makeText(getApplicationContext() , "Please enter your contact number ..." , Toast . LENGTH_SHORT) . show() ;
            return ;
        }

        EditText referral = (EditText) findViewById(R . id . referral) ;
        final String referralStr = referral . getText() . toString() ;

        class wrapper {
            String status;
        }

        class SendPostReqAsyncTask extends AsyncTask<String, Void, wrapper> {
            private wrapper w = new wrapper();
            private ProgressDialog dialog = ProgressDialog.show(RegistrationActivity.this, "",
                    "Registering . Please wait...", true);

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                if(checkConnection(getApplicationContext())) {
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
            }

            @Override
            protected wrapper doInBackground(String... params) {

                String data = "";
                try {
                    data = URLEncoder.encode("no", "UTF-8")
                            + "=" + URLEncoder.encode(vehicle_no, "UTF-8") + "&" + URLEncoder.encode("name", "UTF-8")
                            + "=" + URLEncoder.encode(name, "UTF-8") + "&" + URLEncoder.encode("contactNo", "UTF-8")
                            + "=" + URLEncoder.encode(contact_no, "UTF-8") + "&" + URLEncoder.encode("referral", "UTF-8")
                            + "=" + URLEncoder.encode(referralStr, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    Log.v("HornWave" , "Registration data not encoded ...") ;
                }

                BufferedReader reader = null;

                // Send data
                try {
                    // Define URL where to send data
                    // https://hornwave.000webhostapp.com/register_vehicle.php
                    URL url = new URL("http://pntagencies.in/register_vehicle.php");

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
                        // Append server response in string
                        str = str + line ;
                    }

                    w.status = str;
                } catch (Exception ex) {
                    Log.v("HornWave" , "Error sending Registration data ...") ;
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
                    data = Integer.parseInt(w.status.trim()) ;
                } catch(NumberFormatException ex) {
                    Log.v("HornWave" , "Error in parsing ...") ;
                }

                if (data == 1) {
                    String text = "Vehicle successfully registered ..." ;
                    Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
                    SaveSharedPreference.setVehicleNo(getApplicationContext() , vehicle_no);
                    Intent intent1 = new Intent(getApplicationContext() , MainActivity.class);
                    intent1.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent1);
                    finish();
                } else {
                    if(data == 2) {
                        String text = "Vehicle already registered ..." ;
                        Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
                    } else {
                        String text = "Error registering Vehicle ..." ;
                        Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
                    }
                }
            }
        }
        SendPostReqAsyncTask sendPostReqAsyncTask = new SendPostReqAsyncTask();
        if (checkConnection(getApplicationContext()))
            AsyncTaskCompat.executeParallel(sendPostReqAsyncTask);
        else {
            Toast.makeText(getApplicationContext(), "No internet", Toast.LENGTH_SHORT).show();
            showInternetNotAvailableAlert(RegistrationActivity.this);
        }
    }
}