package com.example.pasb;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Locale;

public class SensorService extends Service {
    public SensorService() {
    }

    private SensorManager mSensorManager;
    private Sensor lnaccl;
    private Sensor step;
    private Sensor orientation;
    private SensorEventListener mLnAcclListner;
    private SensorEventListener mStepListener;
    private SensorEventListener mOrntListener;

    private TextToSpeech tts;

    public final static String TO_MAIN =
            "com.example.pasb.TO_MAIN";

    private long lastStepMillis;
    public final float K = 0.48f;
    public float SL;

    public static float[] axRaws = new float[5];
    public static float[] ayRaws = new float[5];
    public static float[] azRaws = new float[5];

    private float[] dist = new float[2];
    private float[] angle = new float[2];
    private float[] theta = new float[2];

    public static int stepcount=0;

    private static long prevtime;

    private static boolean functionswitch;


    private class OrientationListener implements SensorEventListener {

        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            Log.d("SensorService", "orientation changed");
            Calc.q0 = sensorEvent.values[3];
            Calc.q1 = sensorEvent.values[0];
            Calc.q2 = sensorEvent.values[1];
            Calc.q3 = sensorEvent.values[2];

            Calc.quaternionToMatrix();

            if (!functionswitch && (System.currentTimeMillis()-prevtime)>250) {

                angle[0] = angle[1];
                angle[1] = (float) (Math.atan2(Calc.c13, Calc.c23) * 180.0f / Math.PI);

                String temptts;
                if (Math.floor(angle[0] / 45.0f) != Math.floor(angle[1] / 45.0f)) {
                    prevtime = System.currentTimeMillis();

                    if((angle[0]>90.0f&&angle[1]<-90.0f)||(angle[0]<-90.0f&&angle[1]>90.0f)){
                        temptts = "south";
                    }
                    else{
                        int tempangle = Math.round((angle[0]+angle[1])/90.0f) * 45;
                        if(tempangle>0)
                            temptts = Integer.toString(tempangle) + "degrees, east";
                        else if(tempangle<0)
                            temptts = Integer.toString(-tempangle) + "degrees, west";
                        else
                            temptts = "north";
                    }

                    tts.speak(temptts, TextToSpeech.QUEUE_FLUSH, null);
                }
            }

            Intent intent = new Intent(TO_MAIN);
            sendBroadcast(intent);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
        }
    }

    private class StepListener implements SensorEventListener {

        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            if(sensorEvent.values[0]==1&&(System.currentTimeMillis()-lastStepMillis)>200){

                stepcount++;

                lastStepMillis = System.currentTimeMillis();

                SL = (float) Math.pow((Calc.accmax-Calc.accmin),0.25f);
                float magnitude = (float) Math.sqrt(Calc.accEast*Calc.accEast+Calc.accNorth*Calc.accNorth);
                Calc.delX += SL*Calc.accEast/magnitude;
                Calc.delY += SL*Calc.accNorth/magnitude;

//                Calc.accEast = Calc.accNorth = 0.0f;
//                Calc.accmax = -100.0f;
//                Calc.accmin = 100.0f;
            }
        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
        }
    }

    private BroadcastReceiver stepReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            float mag = (float) Math.sqrt(Calc.c13*Calc.c13+Calc.c23*Calc.c23);
            Calc.delX += Calc.SL * Calc.c13 / mag;
            Calc.delY += Calc.SL * Calc.c23 / mag;

            if(functionswitch){
                dist[0] = dist[1];
                dist[1] = (float)Math.sqrt(Calc.delX*Calc.delX+Calc.delY*Calc.delY);
                String temptts;
                if(Math.floor(dist[0])!=Math.floor(dist[1])){
                    temptts = Integer.toString((int) Math.floor(dist[1])) + "meter";
                    tts.speak(temptts, TextToSpeech.QUEUE_FLUSH,null);
                }

                theta[0] = theta[1];
                theta[1] = (float) (Math.atan2(Calc.delX, Calc.delY)*180.0f/Math.PI);
                if (Math.floor(theta[0] / 30.0f) != Math.floor(theta[1] / 30.0f)) {
                    if(theta[1]*theta[0]<0){
                        temptts = "south";
                    }
                    else{
                        int tempangle = Math.round((theta[0]+theta[1])/60.0f) * 30;
                        if(tempangle>0)
                            temptts = Integer.toString(tempangle) + "degrees, east";
                        else if(tempangle<0)
                            temptts = Integer.toString(-tempangle) + "degrees, west";
                        else
                            temptts = "north";
                    }
                    if(dist[1]>2.0f)
                        tts.speak(temptts, TextToSpeech.QUEUE_FLUSH, null);
                }
            }

        }
    };


    @Override
    public void onCreate() {

        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);

        orientation = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        mOrntListener = new OrientationListener();
        mSensorManager.registerListener(mOrntListener, orientation, SensorManager.SENSOR_DELAY_GAME);

//        step = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
//        mStepListener = new StepListener();
//        mSensorManager.registerListener(mStepListener, step, SensorManager.SENSOR_DELAY_FASTEST);

        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if(i!=TextToSpeech.ERROR){
                    tts.setLanguage(Locale.ENGLISH);
                }
            }
        });

        registerReceiver(stepReceiver, new IntentFilter(BLEService.STEP_DATA));

        Log.d("SensorService", "Service create");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        functionswitch = intent.getBooleanExtra("switch", true);

        if(functionswitch){
            Calc.delX = Calc.delY = Calc.delZ = 0;

            int initornt = (int) (Math.round(Math.atan2(Calc.c13, Calc.c23) * 18.0f / Math.PI) * 10d);
            String temptts = "Location Service on, you are heading to";
            if(initornt>0){
                temptts = temptts + Integer.toString(initornt) +"degrees east";
            }
            else if(initornt<0){
                temptts = temptts + Integer.toString(-initornt) +"degrees west";
            }
            else{
                temptts = temptts + "north";
            }
            tts.speak(temptts, TextToSpeech.QUEUE_FLUSH, null);

        }else{
            prevtime = System.currentTimeMillis();

            int initornt = (int) (Math.round(Math.atan2(Calc.delX, Calc.delY) * 18.0f / Math.PI) * 10d);
            String temptts = "Orientation Service on, you are heading to";
            if(initornt>0){
                temptts = temptts + Integer.toString(initornt) +"degrees east";
            }
            else if(initornt<0){
                temptts = temptts + Integer.toString(-initornt) +"degrees west";
            }
            else{
                temptts = temptts + "north";
            }
            tts.speak(temptts, TextToSpeech.QUEUE_FLUSH, null);
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
//        mSensorManager.unregisterListener(mLnAcclListner);
        mSensorManager.unregisterListener(mOrntListener);
        unregisterReceiver(stepReceiver);
//        mSensorManager.unregisterListener(mStepListener);
        Log.d("SensorService", "Service destroy");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
