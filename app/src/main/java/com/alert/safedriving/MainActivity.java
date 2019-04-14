package com.alert.safedriving;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.util.List;

public class MainActivity extends AppCompatActivity implements LocationListener, SensorEventListener {

    private Button startButton;

    MediaRecorder mRecorder = null;
    static final double MAX_DB = 100.0;
    double calculatedDb = 0.0;

    static final double MAX_SPEED = 5.0;
    double calculatedSpeed = 0.0;


    private SensorManager sensorManager;
    private Sensor accelerometer;

    private float deltaX = 0;
    private float deltaY = 0;

    private float lastX, lastY;

    private double MAX_ACC = 5.0;

    DecimalFormat df = new DecimalFormat("###.##");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView accText = (TextView) findViewById(R.id.currentAcc);
        accText.setText("X:" + df.format(deltaX) + "\nY:" + df.format(deltaY));

        startButton = (Button) findViewById(R.id.detect);

        //Calculating current speed
        LocationManager lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if ( ContextCompat.checkSelfPermission( this, Manifest.permission.ACCESS_FINE_LOCATION ) == PackageManager.PERMISSION_GRANTED ) {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
            this.onLocationChanged(null);
        }

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            // success! we have an accelerometer

            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            //vibrateThreshold = accelerometer.getMaximumRange() / 2;
        } else {
            // fai! we dont have an accelerometer!
        }




        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Thread myThreadSpeed = new Thread(){
                    @Override
                    public void run() {
                        boolean isSmsSend = false;
                        try {
                            while(!isSmsSend) {
                                if(calculatedSpeed >= MAX_SPEED){
                                    sendMySMS("You are driving too fast. Be careful.");
                                    isSmsSend = true;
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };
                myThreadSpeed.start();

                Thread myThreadVolume = new Thread(){
                    @Override
                    public void run() {
                        boolean isSmsSend = false;
                        try {
                            mRecorder = new MediaRecorder();
                            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                            mRecorder.setOutputFile("/dev/null");
                            mRecorder.prepare();
                            mRecorder.start();
                            while(!isSmsSend) {
                                if(calculatedDb >= MAX_DB){
                                    sendMySMS("You are listening loud music while driving. Be careful.");
                                    isSmsSend = true;
                                    //mRecorder.stop();
                                    //mRecorder.release();
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };
                myThreadVolume.start();

                Thread myThreadAcc = new Thread(){
                    @Override
                    public void run() {
                        boolean isSmsSend = false;
                        try {
                            while(!isSmsSend) {
                                if(deltaX > MAX_ACC && deltaY > MAX_ACC){
                                    sendMySMS("Your movement is abnormal and sudden. Be careful.");
                                    isSmsSend = true;
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };
                myThreadAcc.start();
            }
        });
    }

    public void sendMySMS(String message) {

        String phone = "9495013332";//phoneEditText.getText().toString();
        //String message = "You are not driving safely. Be careful.";//messageEditText.getText().toString();

        //Check if the phoneNumber is empty
        if (phone.isEmpty()) {
            Toast.makeText(getApplicationContext(), "Please Enter a Valid Phone Number", Toast.LENGTH_SHORT).show();
        } else {

            SmsManager sms = SmsManager.getDefault();
            // if message length is too long messages are divided
            List<String> messages = sms.divideMessage(message);
            for (String msg : messages) {

                PendingIntent sentIntent = PendingIntent.getBroadcast(this, 0, new Intent("SMS_SENT"), 0);
                PendingIntent deliveredIntent = PendingIntent.getBroadcast(this, 0, new Intent("SMS_DELIVERED"), 0);
                sms.sendTextMessage(phone, null, msg, sentIntent, deliveredIntent);

            }
        }
        this.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(getApplicationContext(), "You are not driving carefully. We sent a SMS to registered contact number.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onLocationChanged(Location location) {

        TextView speedText = (TextView) findViewById(R.id.currentSpeed);

        //Calculating volume level
        updateAmplitude();

        if(null == location){
            speedText.setText(calculatedSpeed +" mph");
        } else {
            float currentSpeed = location.getSpeed();
            calculatedSpeed = currentSpeed * 2.237;
            speedText.setText( df.format(calculatedSpeed) + " mph");
        }
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    public double updateAmplitude() {
        double referenceAmp = Math.pow(10, -1);
        TextView volumeText = (TextView) findViewById(R.id.currentVolume);
        if (mRecorder != null) {
            int currentAmp = mRecorder.getMaxAmplitude();
            if(currentAmp > 0) {
                calculatedDb =  20 * Math.log10(currentAmp / referenceAmp);
                volumeText.setText(df.format(calculatedDb) + " amp");
            }
            return (mRecorder.getMaxAmplitude());
        }
        else {
            return 0;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
    @Override
    public void onSensorChanged(SensorEvent event) {

        // display the current x,y,z accelerometer values
        displayCurrentValues();

        // get the change of the x,y,z values of the accelerometer
        deltaX = Math.abs(lastX - event.values[0]);
        deltaY = Math.abs(lastY - event.values[1]);

        // if the change is below 2, it is just plain noise
        if (deltaX < 1)
            deltaX = 0;
        if (deltaY < 1)
            deltaY = 0;

        /*lastX = event.values[0];
        lastY = event.values[1];*/
    }

    // display the current x,y,z accelerometer values
    public void displayCurrentValues() {
            TextView accText = (TextView) findViewById(R.id.currentAcc);
            accText.setText("X:" + df.format(deltaX) + "\nY:" + df.format(deltaY));
    }
}
